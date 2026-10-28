from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
from transformers import pipeline

router = APIRouter()

# Loads once at startup, not per-request — model stays in memory
sentiment_pipeline = pipeline(
    "sentiment-analysis",
    model="distilbert-base-uncased-finetuned-sst-2-english"
)

class ReviewRequest(BaseModel):
    text: str

class ReviewResponse(BaseModel):
    label: str          # POSITIVE | NEGATIVE
    confidence: float
    safety_flag: str    # derived: OK | CAUTION | WARNING

class BatchReviewRequest(BaseModel):
    reviews: List[str]

# Keywords that escalate a negative review to a safety WARNING vs just CAUTION
SAFETY_KEYWORDS = [
    "avalanche", "landslide", "injury", "accident", "rescue", "altitude sickness",
    "dangerous", "collapsed", "flood", "stranded", "emergency", "unsafe"
]

def classify_safety(text: str, label: str, confidence: float) -> str:
    lowered = text.lower()
    if any(kw in lowered for kw in SAFETY_KEYWORDS):
        return "WARNING"
    if label == "NEGATIVE" and confidence > 0.85:
        return "CAUTION"
    return "OK"

@router.post("/analyze-review", response_model=ReviewResponse)
def analyze_review(req: ReviewRequest):
    result = sentiment_pipeline(req.text)[0]
    label = result["label"]
    confidence = round(result["score"], 4)
    safety_flag = classify_safety(req.text, label, confidence)
    return ReviewResponse(label=label, confidence=confidence, safety_flag=safety_flag)

@router.post("/analyze-reviews-batch")
def analyze_reviews_batch(req: BatchReviewRequest):
    results = sentiment_pipeline(req.reviews)
    output = []
    for text, result in zip(req.reviews, results):
        label = result["label"]
        confidence = round(result["score"], 4)
        output.append({
            "text": text,
            "label": label,
            "confidence": confidence,
            "safety_flag": classify_safety(text, label, confidence)
        })
    return {"results": output}