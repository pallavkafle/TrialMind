"""
TrailMind - healthcare facility dataset builder (nearest hospital / health post / rescue).
Downloads OpenStreetMap healthcare + rescue features within RADIUS_M of each
destination and writes backend/src/data/healthcare.json. Run ONCE with internet.
Resumable: destinations already cached in ml-service/cache/healthcare are skipped.

Usage (from ml-service, venv active):  python -m routing.build_healthcare
Optional: set OVERPASS_URL to use a different Overpass server.
"""
import json
import os
import time
from pathlib import Path

import osmnx as ox

from .destinations import DESTINATIONS
from .router import _slugify, haversine

if os.getenv("OVERPASS_URL"):
    ox.settings.overpass_url = os.getenv("OVERPASS_URL")

RADIUS_M = 25000
PAUSE_S = 5
UTM_NEPAL = 32645            # only used to compute polygon centroids
TAGS = {
    "amenity": ["hospital", "clinic", "doctors"],
    "healthcare": ["hospital", "clinic", "centre", "doctor"],
    "emergency": ["ambulance_station", "mountain_rescue"],
}

ROOT = Path(__file__).resolve().parents[2]               # .../trailmind
CACHE = Path(__file__).resolve().parents[1] / "cache" / "healthcare"
OUT = ROOT / "backend" / "src" / "data" / "healthcare.json"


def _s(v):
    if v is None:
        return None
    if v != v:                                           # NaN
        return None
    s = str(v).strip()
    return s or None


def classify(row):
    if _s(row.get("emergency")) in ("ambulance_station", "mountain_rescue"):
        return "rescue"
    if _s(row.get("amenity")) == "hospital" or _s(row.get("healthcare")) == "hospital":
        return "hospital"
    if _s(row.get("amenity")) == "clinic" or _s(row.get("healthcare")) in ("clinic", "centre"):
        return "clinic"
    return "doctors"


def fetch(lat, lon):
    try:
        gdf = ox.features.features_from_point((lat, lon), tags=TAGS, dist=RADIUS_M)
    except Exception as e:
        if type(e).__name__ == "InsufficientResponseError":
            return []                                    # nothing mapped in this radius
        raise
    if gdf.empty:
        return []
    centers = gdf.to_crs(UTM_NEPAL).geometry.centroid.to_crs(4326)
    out = []
    for ((el, oid), row), pt in zip(gdf.iterrows(), centers):
        kind = classify(row)
        out.append({
            "id": f"{el}{oid}",
            "name": _s(row.get("name:en")) or _s(row.get("name")) or f"Unnamed {kind}",
            "type": kind,
            "lat": round(pt.y, 5),
            "lon": round(pt.x, 5),
            "phone": _s(row.get("phone")) or _s(row.get("contact:phone")),
            "emergency": _s(row.get("emergency")) == "yes",
        })
    return out


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    failed = []
    print(f"Building healthcare dataset for {len(DESTINATIONS)} destinations (radius {RADIUS_M // 1000} km)\n")

    for name, (lat, lon, _arch, _alt) in DESTINATIONS.items():
        f = CACHE / f"{_slugify(name)}.json"
        if f.exists():
            print(f"  skip (cached): {name}")
            continue
        try:
            recs = fetch(lat, lon)
        except Exception as e:
            print(f"  FAILED: {name} -> {type(e).__name__}")
            failed.append(name)
            time.sleep(PAUSE_S)
            continue
        f.write_text(json.dumps(recs), encoding="utf-8")
        print(f"  saved: {name} ({len(recs)} facilities)")
        time.sleep(PAUSE_S)

    # merge every cached destination file, de-duplicated by OSM id
    merged = {}
    for name in DESTINATIONS:
        f = CACHE / f"{_slugify(name)}.json"
        if f.exists():
            for rec in json.loads(f.read_text(encoding="utf-8")):
                merged[rec["id"]] = rec
    facilities = sorted(merged.values(), key=lambda r: r["id"])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(facilities, ensure_ascii=False), encoding="utf-8")

    by_type = {}
    for r in facilities:
        by_type[r["type"]] = by_type.get(r["type"], 0) + 1
    print(f"\nWrote {len(facilities)} facilities -> {OUT}")
    print("By type:", by_type)

    gaps = []
    for name, (lat, lon, _a, _b) in DESTINATIONS.items():
        if not facilities:
            break
        nearest = min(haversine((lat, lon), (r["lat"], r["lon"])) for r in facilities)
        if nearest > 10:
            gaps.append(f"{name} ({nearest:.0f} km)")
    if gaps:
        print("No facility within 10 km of:", ", ".join(gaps))
    if failed:
        print("\nFailed (re-run to retry just these):", ", ".join(failed))


if __name__ == "__main__":
    main()