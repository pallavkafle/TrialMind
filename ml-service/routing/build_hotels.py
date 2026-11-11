"""
TrailMind - hotel / lodge dataset builder (report: hotel search, Accommodation class).
Downloads NAMED lodging features (hotels, guest houses, hostels, teahouse-style lodges...)
from OpenStreetMap within RADIUS_M of each destination, keeps the MAX_PER_DEST nearest per
destination and writes backend/src/data/hotels.json. Run ONCE with internet.
Resumable: destinations already cached in ml-service/cache/hotels are skipped.

Usage (from ml-service, venv active):  python -m routing.build_hotels
Optional: set OVERPASS_URL to use a different Overpass server.
"""
import json
import os
import re
import time
from pathlib import Path

import osmnx as ox

from .destinations import DESTINATIONS
from .router import _slugify, haversine

if os.getenv("OVERPASS_URL"):
    ox.settings.overpass_url = os.getenv("OVERPASS_URL")

RADIUS_M = 10000
MAX_PER_DEST = 150
PAUSE_S = 5
UTM_NEPAL = 32645            # only used to compute polygon centroids
TAGS = {
    "tourism": ["hotel", "guest_house", "hostel", "motel", "chalet",
                "apartment", "alpine_hut", "camp_site"],
}

ROOT = Path(__file__).resolve().parents[2]               # .../trailmind
CACHE = Path(__file__).resolve().parents[1] / "cache" / "hotels"
OUT = ROOT / "backend" / "src" / "data" / "hotels.json"


def _s(v):
    if v is None:
        return None
    if v != v:                                           # NaN
        return None
    s = str(v).strip()
    return s or None


def _stars(v):
    s = _s(v)
    if not s:
        return None
    m = re.match(r"\d", s)
    if not m:
        return None
    n = int(m.group(0))
    return n if 1 <= n <= 5 else None


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
        name = _s(row.get("name:en")) or _s(row.get("name"))
        if not name:
            continue                                     # unnamed lodgings are useless in a list
        out.append({
            "id": f"{el}{oid}",
            "name": name,
            "type": _s(row.get("tourism")) or "hotel",
            "lat": round(pt.y, 5),
            "lon": round(pt.x, 5),
            "stars": _stars(row.get("stars")),
            "phone": _s(row.get("phone")) or _s(row.get("contact:phone")),
            "website": _s(row.get("website")) or _s(row.get("contact:website")),
            "_d": haversine((lat, lon), (pt.y, pt.x)),
        })
    out.sort(key=lambda r: r["_d"])
    out = out[:MAX_PER_DEST]
    for r in out:
        r.pop("_d")
    return out


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    failed = []
    print(f"Building lodging dataset for {len(DESTINATIONS)} destinations "
          f"(radius {RADIUS_M // 1000} km, nearest {MAX_PER_DEST} each)\n")

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
        print(f"  saved: {name} ({len(recs)} lodgings)")
        time.sleep(PAUSE_S)

    # merge every cached destination file, de-duplicated by OSM id
    merged = {}
    for name in DESTINATIONS:
        f = CACHE / f"{_slugify(name)}.json"
        if f.exists():
            for rec in json.loads(f.read_text(encoding="utf-8")):
                merged[rec["id"]] = rec
    lodgings = sorted(merged.values(), key=lambda r: r["id"])
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(lodgings, ensure_ascii=False), encoding="utf-8")

    by_type = {}
    for r in lodgings:
        by_type[r["type"]] = by_type.get(r["type"], 0) + 1
    print(f"\nWrote {len(lodgings)} lodgings -> {OUT} ({OUT.stat().st_size / 1000:,.0f} KB)")
    print("By type:", by_type)

    sparse = []
    for name, (lat, lon, _a, _b) in DESTINATIONS.items():
        n = sum(1 for r in lodgings if haversine((lat, lon), (r["lat"], r["lon"])) <= RADIUS_M / 1000)
        if n < 5:
            sparse.append(f"{name} ({n})")
    if sparse:
        print(f"Fewer than 5 mapped lodgings within {RADIUS_M // 1000} km of:", ", ".join(sparse))
    if failed:
        print("\nFailed (re-run to retry just these):", ", ".join(failed))


if __name__ == "__main__":
    main()