from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional
from langchain_google_genai import ChatGoogleGenerativeAI
from langchain_core.prompts import ChatPromptTemplate
import os
from dotenv import load_dotenv

load_dotenv()

router = APIRouter()

llm = ChatGoogleGenerativeAI(
    model="gemini-3.6-flash",
    google_api_key=os.getenv("GEMINI_API_KEY"),
    temperature=0.7
)

class ItineraryRequest(BaseModel):
    region: str
    days: int
    purpose: str            # Holiday | Trekking | Pilgrimage | Other
    group_size: int
    interests: Optional[List[str]] = []

class ItineraryResponse(BaseModel):
    itinerary: str

PROMPT = ChatPromptTemplate.from_template(
    """You are a Nepal travel expert. Create a day-by-day itinerary.

Region: {region}
Duration: {days} days
Purpose: {purpose}
Group size: {group_size}
Interests: {interests}

Format as:
Day 1: [title]
- morning activity
- afternoon activity
- evening activity

Keep each day to 3-4 bullet points. Be specific to real places in the region.
Do not include pricing (handled separately). Do not add disclaimers."""
)

@router.post("/generate-itinerary", response_model=ItineraryResponse)
def generate_itinerary(req: ItineraryRequest):
    if not os.getenv("GEMINI_API_KEY"):
        raise HTTPException(status_code=500, detail="GEMINI_API_KEY not configured")

    chain = PROMPT | llm
    try:
        result = chain.invoke({
            "region": req.region,
            "days": req.days,
            "purpose": req.purpose,
            "group_size": req.group_size,
            "interests": ", ".join(req.interests) if req.interests else "general sightseeing"
        })

        # Newer Gemini models return content as a list of blocks, not a plain string
        content = result.content
        if isinstance(content, list):
            text = "".join(
                block.get("text", "") if isinstance(block, dict) else str(block)
                for block in content
            )
        else:
            text = content

        return ItineraryResponse(itinerary=text)
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"LLM generation failed: {str(e)}")