from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
import math

router = APIRouter()

class Place(BaseModel):
    name: str
    lat: float
    lon: float

class RouteRequest(BaseModel):
    places: List[Place]
    start_index: int = 0  # which place to start from

class RouteResponse(BaseModel):
    ordered_route: List[str]
    total_distance_km: float

def haversine(p1: Place, p2: Place) -> float:
    """Great-circle distance between two lat/lon points, in km."""
    R = 6371
    lat1, lon1 = math.radians(p1.lat), math.radians(p1.lon)
    lat2, lon2 = math.radians(p2.lat), math.radians(p2.lon)
    dlat, dlon = lat2 - lat1, lon2 - lon1
    a = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 2 * R * math.asin(math.sqrt(a))

def route_distance(route: List[Place]) -> float:
    return sum(haversine(route[i], route[i + 1]) for i in range(len(route) - 1))

def nearest_neighbor(places: List[Place], start_index: int) -> List[Place]:
    unvisited = places.copy()
    current = unvisited.pop(start_index)
    route = [current]
    while unvisited:
        nearest = min(unvisited, key=lambda p: haversine(current, p))
        route.append(nearest)
        unvisited.remove(nearest)
        current = nearest
    return route

def two_opt(route: List[Place]) -> List[Place]:
    best = route
    best_dist = route_distance(best)
    improved = True
    while improved:
        improved = False
        for i in range(1, len(best) - 2):
            for j in range(i + 1, len(best) - 1):
                new_route = best[:i] + best[i:j + 1][::-1] + best[j + 1:]
                new_dist = route_distance(new_route)
                if new_dist < best_dist:
                    best, best_dist = new_route, new_dist
                    improved = True
    return best

@router.get("/route-opt-status")
def status():
    return {"status": "route optimizer not implemented yet"}

@router.post("/optimize-route", response_model=RouteResponse)
def optimize_route(req: RouteRequest):
    initial = nearest_neighbor(req.places, req.start_index)
    optimized = two_opt(initial)
    return RouteResponse(
        ordered_route=[p.name for p in optimized],
        total_distance_km=round(route_distance(optimized), 2)
    )