"""
TrailMind - review sentiment + safety escalation
(report 2.5; class diagram Review.analyzeSentiment).
DistilBERT (SST-2) gives POSITIVE/NEGATIVE; keyword rules escalate hazard
mentions. The model loads lazily on first use so the server starts fast.
"""
import re
import threading
from typing import List

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

MODEL_NAME = "distilbert-base-uncased-finetuned-sst-2-english"

HAZARD_PATTERNS = {
    "avalanche": r"\bavalanche",
    "landslide": r"\blandslide",
    "rockfall": r"\brock ?fall",
    "flood": r"\bflood",
    "altitude sickness": r"\baltitude sickness|\bacute mountain sickness",
    "hypothermia": r"\bhypothermi",
    "frostbite": r"\bfrostbite",
    "injury": r"\binjur(?:y|ies|ed)",
    "accident": r"\baccident",
    "rescue": r"\brescue",
    "evacuation": r"\bevacuat",
    "stranded": r"\bstranded",
    "collapse": r"\bcollaps",
    "dangerous": r"\bdangerous|\bunsafe",
    "emergency": r"\bemergenc",
}
NEGATORS = re.compile(r"\b(?:no|not|never|without|zero|nothing|free of|isn't|wasn't|didn't)\b")

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


def find_hazards(text: str) -> List[str]:
    low = text.lower()
    hits = []
    for label, pattern in HAZARD_PATTERNS.items():
        for m in re.finditer(pattern, low):
            window = low[max(0, m.start() - 30):m.start()]
            if NEGATORS.search(window):
                continue            # "no landslides", "not dangerous"
            hits.append(label)
            break
    return hits


def classify_safety(label: str, confidence: float, hazards: List[str]) -> str:
    if hazards:
        return "WARNING"
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
        raise HTTPException(503, f"Sentiment model unavailable: {type(e).__name__}: {str(e)[:200]}")


def _to_out(text: str, raw: dict) -> ReviewOut:
    label = str(raw["label"]).upper()
    conf = round(float(raw["score"]), 4)
    p_pos = conf if label == "POSITIVE" else round(1 - conf, 4)
    hazards = find_hazards(text)
    return ReviewOut(label=label, confidence=conf, positive_probability=p_pos,
                     safety_flag=classify_safety(label, conf, hazards),
                     matched_keywords=hazards)


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