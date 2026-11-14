"""
TrailMind - lodging dataset builder v2 (hotel search).
Named OSM lodgings (hotel, guest_house, hostel, motel, chalet, apartment, alpine_hut, camp_site)
around every destination AND the main tourist hubs (Thamel, Pokhara Lakeside, Sauraha, Lukla,
Namche, Jomsom and the start cities). Non-lodgings (schools, clinics, banks ...) are dropped.
Writes backend/src/data/hotels.json and backend/src/data/hubs.json. Run ONCE with internet.
Resumable: centres already cached in ml-service/cache/hotels are skipped (delete that folder to rebuild).

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

PAUSE_S = 5
UTM_NEPAL = 32645            # only used to compute polygon centroids
TAGS = {"tourism": ["hotel", "guest_house", "hostel", "motel", "chalet",
                    "apartment", "alpine_hut", "camp_site"]}

# tourist hubs: name -> (lat, lon)
HUBS = {
    "Kathmandu (Thamel)": (27.7154, 85.3123), "Pokhara (Lakeside)": (28.2090, 83.9590),
    "Sauraha (Chitwan)": (27.5780, 84.5030), "Lukla": (27.6869, 86.7314),
    "Namche Bazaar": (27.8069, 86.7140), "Jomsom": (28.7806, 83.7239),
    "Nepalgunj": (28.0500, 81.6167), "Itahari": (26.6646, 87.2718), "Surkhet": (28.6006, 81.6339),
}
# name -> (lat, lon, radius_m, keep nearest N)
CENTRES = {n: (v[0], v[1], 10000, 150) for n, v in DESTINATIONS.items()}
CENTRES.update({n: (lat, lon, 6000, 300) for n, (lat, lon) in HUBS.items()})

NOT_LODGING_AMENITY = {"school", "college", "university", "kindergarten", "hospital", "clinic",
                       "doctors", "pharmacy", "bank", "place_of_worship", "police", "townhall",
                       "library", "community_centre"}
NOT_LODGING_NAME = re.compile(r"\b(school|college|university|hospital|clinic|pharmacy|kindergarten)\b", re.I)

ROOT = Path(__file__).resolve().parents[2]               # .../trailmind
CACHE = Path(__file__).resolve().parents[1] / "cache" / "hotels"
DATA = ROOT / "backend" / "src" / "data"


def _s(v):
    if v is None or v != v:                              # None or NaN
        return None
    s = str(v).strip()
    return s or None


def _stars(v):
    s = _s(v)
    m = re.match(r"\d", s) if s else None
    return int(m.group(0)) if m and 1 <= int(m.group(0)) <= 5 else None


def fetch(lat, lon, radius, cap):
    try:
        gdf = ox.features.features_from_point((lat, lon), tags=TAGS, dist=radius)
    except Exception as e:
        if type(e).__name__ == "InsufficientResponseError":
            return []
        raise
    if gdf.empty:
        return []
    centers = gdf.to_crs(UTM_NEPAL).geometry.centroid.to_crs(4326)
    out = []
    for ((el, oid), row), pt in zip(gdf.iterrows(), centers):
        name = _s(row.get("name:en")) or _s(row.get("name"))
        if not name or NOT_LODGING_NAME.search(name):
            continue
        if _s(row.get("amenity")) in NOT_LODGING_AMENITY or _s(row.get("shop")) or _s(row.get("healthcare")):
            continue
        out.append({
            "id": f"{el}{oid}", "name": name, "type": _s(row.get("tourism")) or "hotel",
            "lat": round(pt.y, 5), "lon": round(pt.x, 5), "stars": _stars(row.get("stars")),
            "phone": _s(row.get("phone")) or _s(row.get("contact:phone")),
            "website": _s(row.get("website")) or _s(row.get("contact:website")),
            "_d": haversine((lat, lon), (pt.y, pt.x)),
        })
    out.sort(key=lambda r: r["_d"])
    out = out[:cap]
    for r in out:
        r.pop("_d")
    return out


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    failed = []
    print(f"Building lodging dataset for {len(CENTRES)} centres "
          f"({len(DESTINATIONS)} destinations + {len(HUBS)} hubs)\n")
    for name, (lat, lon, radius, cap) in CENTRES.items():
        f = CACHE / f"{_slugify(name)}.json"
        if f.exists():
            print(f"  skip (cached): {name}")
            continue
        try:
            recs = fetch(lat, lon, radius, cap)
        except Exception as e:
            print(f"  FAILED: {name} -> {type(e).__name__}")
            failed.append(name)
            time.sleep(PAUSE_S)
            continue
        f.write_text(json.dumps(recs), encoding="utf-8")
        print(f"  saved: {name} ({len(recs)} lodgings)")
        time.sleep(PAUSE_S)

    merged = {}
    for name in CENTRES:
        f = CACHE / f"{_slugify(name)}.json"
        if f.exists():
            for rec in json.loads(f.read_text(encoding="utf-8")):
                merged[rec["id"]] = rec
    lodgings = sorted(merged.values(), key=lambda r: r["id"])
    DATA.mkdir(parents=True, exist_ok=True)
    (DATA / "hotels.json").write_text(json.dumps(lodgings, ensure_ascii=False), encoding="utf-8")
    hubs = [{"name": n, "latitude": lat, "longitude": lon} for n, (lat, lon) in HUBS.items()]
    (DATA / "hubs.json").write_text(json.dumps(hubs, ensure_ascii=False, indent=2), encoding="utf-8")

    by_type = {}
    for r in lodgings:
        by_type[r["type"]] = by_type.get(r["type"], 0) + 1
    print(f"\nWrote {len(lodgings)} lodgings ({(DATA / 'hotels.json').stat().st_size / 1000:,.0f} KB) "
          f"and {len(hubs)} hubs -> {DATA}")
    print("By type:", by_type)

    sparse = []
    for name, (lat, lon, radius, _cap) in CENTRES.items():
        n = sum(1 for r in lodgings if haversine((lat, lon), (r["lat"], r["lon"])) <= radius / 1000)
        if n < 5:
            sparse.append(f"{name} ({n})")
    if sparse:
        print("Fewer than 5 mapped lodgings near:", ", ".join(sparse))
    if failed:
        print("\nFailed (re-run to retry just these):", ", ".join(failed))


if __name__ == "__main__":
    main()