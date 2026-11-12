"""
TrailMind - itinerary generation (report 4.1.3, Algorithm 2).

Deterministic core (route order, day split, distances, budget) is computed in code.
Gemini only writes day titles/activities inside a fixed day->location skeleton; if it
fails, is too slow, or returns bad JSON, a deterministic template is used.

Timeout chain: Gemini deadline (35 s) < ml-service total (~40 s) < Node proxy (90 s)
< Android read timeout (130 s). Failures return one friendly warning plus a
machine-readable fallback_reason; details go only to the server log.
Successful Gemini plans are cached in memory for 30 minutes.
"""
import hashlib
import json
import logging
import os
import re
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from concurrent.futures import TimeoutError as FuturesTimeout
from typing import Dict, List, Optional

from dotenv import load_dotenv
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field, ValidationError

from routing.destinations import DESTINATIONS
from routing.router import haversine, journey_distance, macro_distance, optimize_multi_stop

load_dotenv()
log = logging.getLogger("trailmind.itinerary")

MAX_STOPS = 10
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-3.6-flash")
LLM_TIMEOUT_S = 30                                        # passed to the client
LLM_DEADLINE_S = float(os.getenv("LLM_DEADLINE_S", "35"))  # hard limit enforced by us
CACHE_TTL_S = 30 * 60
CACHE_MAX = 100

# relative share of the trip days each archetype gets (min 1 day per stop)
DAY_WEIGHT = {"Trekking": 3.0, "Wildlife": 2.0, "Hiking": 1.5,
              "Homestay": 1.5, "Cultural": 1.0}

# casual names -> canonical destination. NOTE: bare "annapurna" maps to the Circuit.
EXTRA_ALIASES = {
    "ebc": "Everest Base Camp", "everest": "Everest Base Camp",
    "annapurna": "Annapurna Circuit", "chitwan": "Chitwan National Park",
    "bardia": "Bardia National Park", "mustang": "Upper Mustang",
    "pashupati": "Pashupatinath", "boudha": "Boudhanath",
    "swayambhu": "Swayambhunath", "monkey temple": "Swayambhunath",
    "bhaktapur": "Bhaktapur Durbar Square", "shivapuri": "Shivapuri National Park",
    "bandipur": "Bandipur Trails", "gokyo": "Gokyo Lakes",
    "langtang": "Langtang Valley", "manaslu": "Manaslu Circuit",
    "mardi": "Mardi Himal", "sagarmatha": "Sagarmatha National Park",
}

ACTIVITIES = {
    "Cultural": (["Visit the main heritage site", "Walk the surrounding old streets and markets", "Try local food in the evening"],
                 ["Visit a second temple or museum nearby", "Free time for local crafts and cafes"]),
    "Trekking": (["Start the day's trail stage", "Lunch at a village teahouse", "Rest and acclimatise at the night's lodge"],
                 ["Continue along the trail at a steady pace", "Rest early and drink plenty of water"]),
    "Hiking": (["Start the hike early for clear views", "Picnic or tea stop on the ridge", "Return and relax in the evening"],
               ["Take a second short walk around the area", "Enjoy the sunset viewpoint"]),
    "Homestay": (["Arrive and meet your host family", "Walk around the village", "Share a home-cooked dinner"],
                 ["Join a local farming or cultural activity", "Evening cultural programme if available"]),
    "Wildlife": (["Check in and brief with the park guide", "Afternoon jungle walk or jeep safari", "Evening at leisure by the park"],
                 ["Early-morning safari or canoe ride", "Visit a local Tharu village or breeding centre"]),
}

FRIENDLY = {
    "no_key": "AI text is not set up on the server, so a template plan was used.",
    "timeout": "The AI took too long, so a template plan was used.",
    "bad_format": "The AI reply could not be used, so a template plan was used.",
    "llm_error": "The AI is unavailable right now, so a template plan was used.",
}
FRIENDLY_SUFFIX = " Distances and costs are unaffected."


def _build_alias_map():
    amap = {}
    for name in DESTINATIONS:
        low = name.lower()
        amap[low] = name
        for part in re.split(r"[()/]", low):
            part = part.strip()
            if len(part) >= 4:
                amap.setdefault(part, name)
    for alias, name in EXTRA_ALIASES.items():
        amap.setdefault(alias, name)
    return amap


_ALIASES = _build_alias_map()


def resolve_destinations(text: str) -> List[str]:
    """Map free text to canonical destination names, in order of mention."""
    low = text.lower()
    hits = {}
    for alias, name in _ALIASES.items():
        m = re.search(r"\b" + re.escape(alias) + r"\b", low)
        if m and (name not in hits or m.start() < hits[name]):
            hits[name] = m.start()
    return [n for n, _ in sorted(hits.items(), key=lambda kv: kv[1])]


def allocate_days(stops: List[str], total: int) -> List[int]:
    n = len(stops)
    extra = total - n
    w = [DAY_WEIGHT.get(DESTINATIONS[s][2], 1.0) for s in stops]
    ws = sum(w)
    raw = [extra * x / ws for x in w]
    base = [int(r) for r in raw]
    remainder = extra - sum(base)
    for i in sorted(range(n), key=lambda i: raw[i] - base[i], reverse=True)[:remainder]:
        base[i] += 1
    return [1 + b for b in base]


# ---------------- request / response models ----------------
class ItineraryRequest(BaseModel):
    query: str = ""
    stops: List[str] = Field(default_factory=list)
    starting_city: str = "Kathmandu"
    duration_days: int = Field(7, ge=1, le=30)
    group_size: int = Field(2, ge=1, le=20)
    travel_month: str = "October"
    nationality_category: str = "Foreign"
    traveler_type: str = "Group"
    accommodation_tier: str = "Standard"
    transport_mode: str = "Public Bus"
    use_guide: bool = True
    use_porter: bool = True
    use_llm: bool = True


class DayOut(BaseModel):
    day: int
    location: str
    title: str
    activities: List[str]
    tips: Optional[str] = None


class RouteInfo(BaseModel):
    stops: List[str]
    leg_km: List[float]
    macro_km: float
    micro_km: float
    total_km: float


class BudgetInfo(BaseModel):
    permit_npr: float
    guide_npr: float
    porter_npr: float
    lodging_npr: float
    meals_npr: float
    transport_npr: float
    total_npr: float
    per_person_npr: float
    permit_by_stop: Dict[str, float] = Field(default_factory=dict)


class ItineraryResponse(BaseModel):
    title: str
    summary: str
    days: List[DayOut]
    days_per_stop: List[int]
    safety_notes: List[str]
    route: RouteInfo
    budget: BudgetInfo
    source: str                                  # "gemini" | "fallback"
    model: Optional[str] = None
    warnings: List[str] = Field(default_factory=list)
    fallback_reason: Optional[str] = None        # no_key | timeout | bad_format | llm_error | disabled
    cached: bool = False


class _LLMDay(BaseModel):
    day: int = 0
    location: str = ""
    title: str = ""
    activities: List[str] = Field(default_factory=list)
    tips: Optional[str] = None


class _LLMPlan(BaseModel):
    title: str = ""
    summary: str = ""
    days: List[_LLMDay]
    safety_notes: List[str] = Field(default_factory=list)


# ---------------- cache (successful Gemini plans only) ----------------
_cache = {}
_cache_lock = threading.Lock()


def _cache_key(req: ItineraryRequest) -> str:
    return hashlib.sha256(json.dumps(req.model_dump(), sort_keys=True).encode()).hexdigest()


def _cache_get(key: str):
    with _cache_lock:
        hit = _cache.get(key)
        if not hit:
            return None
        if time.time() - hit[0] > CACHE_TTL_S:
            _cache.pop(key, None)
            return None
        return hit[1]


def _cache_put(key: str, value: ItineraryResponse) -> None:
    with _cache_lock:
        if len(_cache) >= CACHE_MAX:
            _cache.pop(min(_cache, key=lambda k: _cache[k][0]))
        _cache[key] = (time.time(), value)


# ---------------- LLM helpers ----------------
SYSTEM = (
    "You are a data formatter for a Nepal travel app, not a travel writer. "
    "Use ONLY the facts provided. Do not invent distances, prices, permits or "
    "places that are not implied by the stop names. Respond with a single JSON "
    "object and nothing else: no markdown, no code fences, no commentary."
)
HUMAN = (
    "Traveller request: {query}\n\n"
    "Facts (JSON, authoritative): {facts}\n\n"
    "Day skeleton (day -> location), keep this order exactly:\n{skeleton}\n\n"
    "Return JSON with exactly this shape:\n"
    '{{"title": "...", "summary": "two sentences", "days": [{{"day": 1, "location": "...", '
    '"title": "...", "activities": ["...", "...", "..."], "tips": "..."}}], '
    '"safety_notes": ["..."]}}\n\n'
    "Rules: exactly {total_days} entries in days, one per skeleton line; 3 or 4 "
    "activities per day; mention only places that belong to the day's location; "
    "do not mention prices or permit fees. Day 1 must begin with the journey from "
    "facts.start.city (facts.start.km km). The first day at each later stop must begin "
    "with the transfer from the previous stop (use leg_km). Tips must be practical "
    "(timing, what to bring, etiquette) and must not repeat altitude, distances or the "
    "location name; use an empty string if there is nothing useful. Do not repeat the "
    "same activity on consecutive days."
)

_llm = None
_executor = ThreadPoolExecutor(max_workers=8, thread_name_prefix="gemini")


def _get_llm():
    global _llm
    if _llm is not None:
        return _llm
    key = os.getenv("GEMINI_API_KEY")
    if not key:
        raise RuntimeError("GEMINI_API_KEY not set")
    from langchain_google_genai import ChatGoogleGenerativeAI
    try:
        _llm = ChatGoogleGenerativeAI(model=GEMINI_MODEL, google_api_key=key,
                                      max_retries=0, timeout=LLM_TIMEOUT_S)
    except Exception as e:
        log.warning("Gemini client options not accepted (%s); using defaults", type(e).__name__)
        _llm = ChatGoogleGenerativeAI(model=GEMINI_MODEL, google_api_key=key)
    return _llm


def _flatten(content) -> str:
    if isinstance(content, list):
        return "".join(b.get("text", "") if isinstance(b, dict) else str(b) for b in content)
    return str(content)


def _extract_json(text: str) -> dict:
    text = re.sub(r"```(?:json)?", "", text).strip()
    start, end = text.find("{"), text.rfind("}")
    if start == -1 or end == -1:
        raise ValueError("no JSON object in model output")
    return json.loads(text[start:end + 1])


def _llm_plan(query, facts, skeleton, total_days) -> _LLMPlan:
    from langchain_core.prompts import ChatPromptTemplate
    prompt = ChatPromptTemplate.from_messages([("system", SYSTEM), ("human", HUMAN)])
    result = (prompt | _get_llm()).invoke({
        "query": query or "(none given; use the stops provided)",
        "facts": json.dumps(facts),
        "skeleton": "\n".join(f"Day {d}: {loc}" for d, loc in skeleton),
        "total_days": total_days,
    })
    plan = _LLMPlan.model_validate(_extract_json(_flatten(result.content)))
    if len(plan.days) != total_days:
        raise ValueError(f"model returned {len(plan.days)} days, expected {total_days}")
    return plan


def _llm_plan_with_deadline(query, facts, skeleton, total_days) -> _LLMPlan:
    """Run the Gemini call in a worker thread and give up after LLM_DEADLINE_S."""
    future = _executor.submit(_llm_plan, query, facts, skeleton, total_days)
    try:
        return future.result(timeout=LLM_DEADLINE_S)
    except FuturesTimeout:
        future.cancel()
        raise


def _fallback_reason(e: Exception) -> str:
    name = type(e).__name__.lower()
    if isinstance(e, RuntimeError) and "GEMINI_API_KEY" in str(e):
        return "no_key"
    if isinstance(e, FuturesTimeout) or "timeout" in name or "deadline" in name:
        return "timeout"
    if isinstance(e, (ValueError, ValidationError)):
        return "bad_format"
    return "llm_error"


def _safe_msg(e: Exception) -> str:
    """Exception text for the server log only, with the API key masked."""
    msg = f"{type(e).__name__}: {e}"
    key = os.getenv("GEMINI_API_KEY")
    if key:
        msg = msg.replace(key, "***")
    return msg[:300]


def _fmt_km(km: float) -> str:
    return f"{km:.0f}" if km >= 10 else f"{km:.1f}"


def _fallback_days(order, alloc, leg_kms, start_city, start_km):
    out, n = [], 1
    last_stop = len(order) - 1
    for idx, (stop, d) in enumerate(zip(order, alloc)):
        first, later = ACTIVITIES[DESTINATIONS[stop][2]]
        for k in range(d):
            acts = []
            if k == 0 and idx == 0 and start_km >= 1:
                acts.append(f"Travel from {start_city} to {stop} (about {_fmt_km(start_km)} km by road)")
            elif k == 0 and idx > 0:
                acts.append(f"Travel from {order[idx - 1]} (about {_fmt_km(leg_kms[idx - 1])} km by road)")
            acts += first if k == 0 else [later[(k - 1) % len(later)]]
            if k == d - 1:
                acts.append("Check out and head home" if idx == last_stop
                            else f"Pack up and prepare to move on to {order[idx + 1]}")
            out.append(DayOut(day=n, location=stop, title=f"Day {n}: {stop}", activities=acts))
            n += 1
    return out


def _nearest_stop(stops: List[str], city_xy) -> str:
    """The stop closest to the starting city (the first given stop if the city is unknown)."""
    if not city_xy:
        return stops[0]
    return min(stops, key=lambda s: haversine(city_xy, (DESTINATIONS[s][0], DESTINATIONS[s][1])))


# ---------------- router ----------------
def build_itinerary_router(predict_fn, budget_cls, transport_fare_fn, dest_meta, cities) -> APIRouter:
    router = APIRouter()

    @router.post("/itinerary", response_model=ItineraryResponse)
    def itinerary(req: ItineraryRequest):
        key = _cache_key(req)
        if req.use_llm:
            hit = _cache_get(key)
            if hit is not None:
                return hit.model_copy(update={"cached": True})

        # 1. resolve stops
        stops = list(dict.fromkeys(req.stops)) or resolve_destinations(req.query)
        if not stops:
            raise HTTPException(400, "No supported destination found. Name one (e.g. "
                                     "'Everest Base Camp', 'Lumbini') or see GET /destinations.")
        unknown = [s for s in stops if s not in DESTINATIONS]
        if unknown:
            raise HTTPException(400, f"Unknown destination(s): {', '.join(unknown)}")
        if len(stops) > MAX_STOPS:
            raise HTTPException(400, f"Maximum {MAX_STOPS} stops")
        if len(stops) > req.duration_days:
            raise HTTPException(400, f"{len(stops)} stops need at least {len(stops)} days")

        # 2. route order + distances
        # the trip starts at the stop nearest the starting city; the rest is optimised from there
        start = _nearest_stop(stops, cities.get(req.starting_city))
        if len(stops) > 2:
            order = optimize_multi_stop(stops, start)[0]
        else:
            order = [start] + [s for s in stops if s != start]
        leg_kms = [round(macro_distance(a, b), 1) for a, b in zip(order, order[1:])]
        comp = journey_distance(order)

        # 3. days per stop
        alloc = allocate_days(order, req.duration_days)

        # 4. budget: per-stop components + start->first stop + inter-stop legs
        parts = {"permit": 0.0, "guide": 0.0, "porter": 0.0, "lodging": 0.0, "meals": 0.0}
        first_transport, start_km = 0.0, 0.0
        permit_by_stop = {}
        for i, (stop, d) in enumerate(zip(order, alloc)):
            r = predict_fn(budget_cls(
                starting_city=req.starting_city, destination=stop, duration_days=d,
                group_size=req.group_size, travel_month=req.travel_month,
                nationality_category=req.nationality_category,
                traveler_type=req.traveler_type, accommodation_tier=req.accommodation_tier,
                transport_mode=req.transport_mode, use_guide=req.use_guide,
                use_porter=req.use_porter, skip_ml=True))
            b = r.breakdown
            parts["permit"] += b.permit_npr
            permit_by_stop[stop] = b.permit_npr
            parts["guide"] += b.guide_npr
            parts["porter"] += b.porter_npr
            parts["lodging"] += b.lodging_npr
            parts["meals"] += b.meals_npr
            if i == 0:
                first_transport = b.transport_npr
                start_km = r.start_to_dest_km
        transport = first_transport + sum(transport_fare_fn(km) * req.group_size for km in leg_kms)
        total = sum(parts.values()) + transport
        budget = BudgetInfo(
            permit_npr=round(parts["permit"], 2), guide_npr=round(parts["guide"], 2),
            porter_npr=round(parts["porter"], 2), lodging_npr=round(parts["lodging"], 2),
            meals_npr=round(parts["meals"], 2), transport_npr=round(transport, 2),
            total_npr=round(total, 2), per_person_npr=round(total / req.group_size, 2),
            permit_by_stop={k: round(v, 2) for k, v in permit_by_stop.items()})

        # 5. deterministic safety notes
        notes = []
        max_alt = max(DESTINATIONS[s][3] for s in order)
        if max_alt >= 3000:
            notes.append(f"Highest point on this route is {max_alt:,} m. Above 3,000 m, raise your "
                         "sleeping altitude gradually (about 300-500 m per day), take rest days, "
                         "and descend if symptoms of altitude sickness appear.")
        restricted = [s for s in order if dest_meta.get(s, (0, 0))[1]]
        if restricted:
            notes.append(f"Restricted area on this route ({', '.join(restricted)}): special permits "
                         "arranged through a registered agency are required. Check current rules before booking.")

        # 6. itinerary text: Gemini inside a fixed skeleton, else fallback
        skeleton = []
        for loc, d in zip(order, alloc):
            for _ in range(d):
                skeleton.append((len(skeleton) + 1, loc))
        facts = {
            "ordered_stops": [{"name": s, "category": DESTINATIONS[s][2],
                               "altitude_m": DESTINATIONS[s][3], "days": d}
                              for s, d in zip(order, alloc)],
            "leg_km": leg_kms, "total_km": comp["total_km"], "month": req.travel_month,
            "group_size": req.group_size, "traveller_type": req.traveler_type,
            "accommodation": req.accommodation_tier,
            "start": {"city": req.starting_city, "km": start_km},
        }
        warnings, source, reason = [], "fallback", None
        title = f"{req.duration_days}-day trip: {' → '.join(order)}"
        summary = (f"{len(order)} stop(s) over {req.duration_days} days, about "
                   f"{comp['total_km']:.0f} km of travel and walking in total.")
        days = _fallback_days(order, alloc, leg_kms, req.starting_city, start_km)

        if req.use_llm:
            try:
                plan = _llm_plan_with_deadline(req.query, facts, skeleton, req.duration_days)
                fb = {d.day: d for d in days}
                days = []
                for i, d in enumerate(plan.days):
                    num, loc = skeleton[i]
                    acts = [a for a in d.activities if a.strip()][:5] or fb[num].activities
                    days.append(DayOut(day=num, location=loc,
                                       title=d.title.strip() or fb[num].title,
                                       activities=acts, tips=d.tips or None))
                title = plan.title.strip() or title
                summary = plan.summary.strip() or summary
                notes += [n for n in plan.safety_notes if n.strip()][:3]
                source = "gemini"
            except Exception as e:
                reason = _fallback_reason(e)
                log.warning("Gemini plan failed (%s): %s", reason, _safe_msg(e))
                warnings.append(FRIENDLY[reason] + FRIENDLY_SUFFIX)
        else:
            reason = "disabled"

        response = ItineraryResponse(
            title=title, summary=summary, days=days, days_per_stop=alloc,
            safety_notes=notes,
            route=RouteInfo(stops=order, leg_km=leg_kms, macro_km=comp["macro_km"],
                            micro_km=comp["micro_km"], total_km=comp["total_km"]),
            budget=budget, source=source,
            model=GEMINI_MODEL if source == "gemini" else None,
            warnings=warnings, fallback_reason=reason, cached=False)

        if source == "gemini":
            _cache_put(key, response)
        return response

    return router