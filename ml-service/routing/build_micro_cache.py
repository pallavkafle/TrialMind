"""
Precompute the micro-exploration (walking) distance for every destination (report eq. 4.3)
from the cached OSM walking graphs and save it to routing/micro_distances.json.
Run ONCE (and again only if destinations or the OSM cache change), then commit the JSON.

Usage (from ml-service, venv active):  python -m routing.build_micro_cache
Needs routing/osm_cache/*.graphml  (python -m routing.build_osm_cache). Keep that folder.
"""
import json
from datetime import date

from .destinations import DESTINATIONS
from .router import MICRO_FILE, _micro_from_graph


def main():
    values = {}
    print(f"{'destination':34s} {'km':>6s}  source")
    for name in DESTINATIONS:
        km, source = _micro_from_graph(name)
        values[name] = {"km": km, "source": source}
        print(f"{name:34s} {km:6.2f}  {source}")

    data = {
        "generated": date.today().isoformat(),
        "method": "entry node to farthest reachable node on the cached OSM walking graph (eq. 4.3); "
                  "deterministic 1.20-1.50 km fallback when the graph is missing, empty or disconnected",
        "values": values,
    }
    MICRO_FILE.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    n_osm = sum(1 for v in values.values() if v["source"] == "osm")
    print(f"\nWrote {MICRO_FILE}")
    print(f"{n_osm} values from OSM graphs, {len(values) - n_osm} deterministic fallback")


if __name__ == "__main__":
    main()