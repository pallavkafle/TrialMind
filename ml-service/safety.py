"""
TrailMind - review sentiment + safety escalation
(report 2.5; class diagram Review.analyzeSentiment).
DistilBERT (SST-2) gives POSITIVE/NEGATIVE; keyword rules escalate hazard mentions.

Hazards come in two kinds:
  hard - avalanche, landslide, injury ...: any mention that is not negated is a WARNING.
  soft - rescue, emergency, evacuation: ambiguous ("Rescue Association post", "emergency exit"),
         so WARNING only when the review is NEGATIVE, otherwise CAUTION.
Negation is scoped to the same clause: a negator ("no", "not", "never", "without", "n't" ...)
within the 4 words before the keyword, with no punctuation or conjunction in between.
For safety a missed hazard is worse than a false alarm, so the rules err on the side of flagging.
The model loads lazily, or in the background via warm_up(), so the server starts fast.
"""
import logging
import re
import threading
from typing import List, Tuple

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

log = logging.getLogger("trailmind.safety")

MODEL_NAME = "distilbert-base-uncased-finetuned-sst-2-english"

HARD_PATTERNS = {
    "avalanche": r"\bavalanche",
    "landslide": r"\blandslide",
    "rockfall": r"\brock ?fall",
    "flood": r"\bflood",
    "altitude sickness": r"\b(?:altitude|mountain) sickness",
    "hypothermia": r"\bhypothermi",
    "frostbite": r"\bfrostbite",
    "injury": r"\binjur(?:y|ies|ed)",
    "accident": r"\baccidents?\b",
    "stranded": r"\bstranded",
    "collapse": r"\bcollaps",
    "dangerous": r"\bdangerous|\bunsafe",
    "blizzard": r"\bblizzard|\bsnowstorm",
    "earthquake": r"\bearthquake",
}
SOFT_PATTERNS = {
    "rescue": r"\brescue",
    "emergency": r"\bemergenc",
    "evacuation": r"\bevacuat",
}

CLAUSE_SPLIT = re.compile(r"[.!?;,:\n]|\b(?:but|and|however|although|though|while|yet|except|whereas)\b")
NEGATORS = {"no", "not", "never", "without", "zero", "nothing", "none", "neither", "nor", "cannot"}
NEGATION_WINDOW = 4

_pipe = None
_lock = threading.Lock()


def _get_pipe():
    global _pipe
    if _pipe is None:
        with _lock:
            if _pipe is None:
                from transformers import pipeline
                _pipe = pipeline("text-classification", model=MODEL_NAME)
    return _pipe


def warm_up():
    """Load the sentiment model in a background thread so the first review is fast."""
    def _load():
        try:
            _get_pipe()
            log.info("sentiment model ready")
        except Exception as e:                      # never break server startup
            log.warning("sentiment warm-up skipped: %s", type(e).__name__)

    threading.Thread(target=_load, daemon=True, name="sentiment-warmup").start()


def _negated(low: str, start: int) -> bool:
    clause = CLAUSE_SPLIT.split(low[:start])[-1]
    tokens = re.findall(r"[a-z']+", clause)[-NEGATION_WINDOW:]
    return any(t in NEGATORS or t.endswith("n't") for t in tokens)


def _scan(low: str, patterns: dict) -> List[str]:
    hits = []
    for label, pattern in patterns.items():
        for m in re.finditer(pattern, low):
            if not _negated(low, m.start()):
                hits.append(label)
                break
    return hits


def find_hazards(text: str) -> Tuple[List[str], List[str]]:
    """Returns (hard hazards, soft hazards) mentioned and not negated."""
    low = text.lower().replace("\u2019", "'")
    return _scan(low, HARD_PATTERNS), _scan(low, SOFT_PATTERNS)


def classify_safety(label: str, confidence: float, hard: List[str], soft: List[str]) -> str:
    if hard:
        return "WARNING"
    if soft:
        return "WARNING" if label == "NEGATIVE" else "CAUTION"
    if label == "NEGATIVE" and confidence >= 0.85:
        return "CAUTION"
    return "OK"


class ReviewIn(BaseModel):
    text: str = Field(..., min_length=3, max_length=2000)


class ReviewOut(BaseModel):
    label: str
    confidence: float
    positive_probability: float
    safety_flag: str
    matched_keywords: List[str]


class BatchIn(BaseModel):
    reviews: List[str] = Field(..., min_length=1, max_length=100)


class BatchItem(ReviewOut):
    text: str


class BatchSummary(BaseModel):
    count: int
    positive: int
    negative: int
    positive_pct: float
    score_100: float
    warning_count: int
    caution_count: int


class BatchOut(BaseModel):
    results: List[BatchItem]
    summary: BatchSummary


def _run_model(texts: List[str]):
    try:
        return _get_pipe()(texts, truncation=True, max_length=512, batch_size=16)
    except Exception as e:
        log.warning("sentiment model unavailable: %s: %s", type(e).__name__, str(e)[:200])
        raise HTTPException(503, "Sentiment model unavailable")


def _to_out(text: str, raw: dict) -> ReviewOut:
    label = str(raw["label"]).upper()
    conf = round(float(raw["score"]), 4)
    p_pos = conf if label == "POSITIVE" else round(1 - conf, 4)
    hard, soft = find_hazards(text)
    return ReviewOut(label=label, confidence=conf, positive_probability=p_pos,
                     safety_flag=classify_safety(label, conf, hard, soft),
                     matched_keywords=hard + soft)


def build_safety_router() -> APIRouter:
    router = APIRouter()

    @router.post("/analyze-review", response_model=ReviewOut)
    def analyze_review(req: ReviewIn):
        return _to_out(req.text, _run_model([req.text])[0])

    @router.post("/analyze-reviews-batch", response_model=BatchOut)
    def analyze_batch(req: BatchIn):
        texts = [t.strip() for t in req.reviews]
        if any(len(t) < 3 for t in texts):
            raise HTTPException(400, "Every review needs at least 3 characters")
        raws = _run_model(texts)
        items = []
        for t, raw in zip(texts, raws):
            out = _to_out(t, raw)
            items.append(BatchItem(text=t, **out.model_dump()))
        n = len(items)
        pos = sum(1 for i in items if i.label == "POSITIVE")
        avg_pos = sum(i.positive_probability for i in items) / n
        summary = BatchSummary(
            count=n, positive=pos, negative=n - pos,
            positive_pct=round(100 * pos / n, 1), score_100=round(100 * avg_pos, 1),
            warning_count=sum(1 for i in items if i.safety_flag == "WARNING"),
            caution_count=sum(1 for i in items if i.safety_flag == "CAUTION"))
        return BatchOut(results=items, summary=summary)

    return router