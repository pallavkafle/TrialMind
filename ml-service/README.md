# TrailMind intelligence service (FastAPI)

Budget (Random Forest + itemized rates), routing (A*, nearest-neighbour + 2-opt over 27 destinations),
itineraries (Gemini inside a fixed day skeleton, deterministic fallback), review sentiment and safety flags (DistilBERT + hazard rules).

## Run
    python -m venv venv && venv\Scripts\activate
    pip install -r requirements.txt
    copy .env.example .env      (then add your Gemini key)
    python -m uvicorn app:app --reload --port 8000
Docs: http://127.0.0.1:8000/docs

## Data rebuild commands (need internet, run once)
    python -m routing.build_osm_cache       walking graphs (cache is not in git)
    python -m routing.build_micro_cache     routing/micro_distances.json (committed)
    python -m routing.build_healthcare      backend/src/data/healthcare.json
    python -m routing.build_hotels          backend/src/data/hotels.json and hubs.json
    python train_model.py                   models/*.joblib
Evaluation: python -m evaluation.eval_safety