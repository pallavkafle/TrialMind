"""
TrailMind - destination + route endpoints.
Plugged into app.py via include_router; destination metadata
(restricted / porter) stays defined once, in app.py.
"""
from typing import List, Optional

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field

from routing.destinations import DESTINATIONS
from routing.router import (
    astar, build_graph, journey_distance, macro_distance, optimize_multi_stop,
)

MAX_STOPS = 10          # report 1.4: up to ten destination nodes
_graph = None


def get_graph():
    """Build the destination graph once, on first use."""
    global _graph
    if _graph is None:
        _graph = build_graph(k=6)
    return _graph


class DestinationOut(BaseModel):
    name: str
    category: str
    latitude: float
    longitude: float
    altitude_m: int
    is_restricted: bool
    has_porter: bool


class RouteRequest(BaseModel):
    stops: List[str] = Field(..., min_length=2, max_length=MAX_STOPS)
    start: Optional[str] = None


class StopOut(BaseModel):
    order: int
    name: str
    category: str
    latitude: float
    longitude: float
    altitude_m: int


class LegOut(BaseModel):
    from_stop: str
    to_stop: str
    km: float


class RouteResponse(BaseModel):
    stops: List[StopOut]
    legs: List[LegOut]
    macro_km: float
    micro_km: float
    total_km: float
    nearest_neighbour_km: float
    optimized_km: float
    method: str


def build_router(dest_meta: dict, components: dict) -> APIRouter:
    router = APIRouter()

    @router.get("/destinations", response_model=List[DestinationOut])
    def list_destinations(category: Optional[str] = None, q: Optional[str] = None):
        out = []
        for name, (lat, lon, arch, alt) in DESTINATIONS.items():
            if category and arch.lower() != category.lower():
                continue
            if q and q.lower() not in name.lower():
                continue
            out.append(DestinationOut(
                name=name, category=arch, latitude=lat, longitude=lon,
                altitude_m=alt,
                is_restricted=bool(dest_meta.get(name, (alt, 0))[1]),
                has_porter=bool(components.get(name, {}).get("has_porter", False)),
            ))
        return out

    @router.post("/route", response_model=RouteResponse)
    def plan_route(req: RouteRequest):
        stops = list(dict.fromkeys(req.stops))          # dedupe, keep order
        unknown = [s for s in stops if s not in DESTINATIONS]
        if unknown:
            raise HTTPException(400, f"Unknown destination(s): {', '.join(unknown)}")
        if req.start:
            if req.start not in DESTINATIONS:
                raise HTTPException(400, f"Unknown start: {req.start}")
            if req.start not in stops:
                stops.insert(0, req.start)
        if len(stops) < 2:
            raise HTTPException(400, "Need at least two distinct stops")
        if len(stops) > MAX_STOPS:
            raise HTTPException(400, f"Maximum {MAX_STOPS} stops")

        if len(stops) == 2:
            order, dist = astar(get_graph(), stops[0], stops[1])
            if order is None:
                raise HTTPException(404, "No route found")
            nn_km = opt_km = dist
            method = "astar"
        else:
            order, opt_km, nn_km = optimize_multi_stop(stops, req.start or stops[0])
            method = "nearest_neighbour+2opt"

        legs = [
            LegOut(from_stop=a, to_stop=b, km=round(macro_distance(a, b), 1))
            for a, b in zip(order, order[1:])
        ]
        comp = journey_distance(order)
        stop_out = [
            StopOut(order=i, name=n, category=DESTINATIONS[n][2],
                    latitude=DESTINATIONS[n][0], longitude=DESTINATIONS[n][1],
                    altitude_m=DESTINATIONS[n][3])
            for i, n in enumerate(order, 1)
        ]
        return RouteResponse(
            stops=stop_out, legs=legs,
            macro_km=comp["macro_km"], micro_km=comp["micro_km"],
            total_km=comp["total_km"],
            nearest_neighbour_km=round(nn_km, 1), optimized_km=round(opt_km, 1),
            method=method,
        )

    return router