# TrailMind Intelligence Service

Python/FastAPI microservice for TrailMind — Nepal-specific intelligent travel planning app (final-year major project, CT 707).

Handles ML-driven budget prediction, route optimization, AI-generated itineraries, and review safety/sentiment analysis. Sits between the API Gateway (Node.js) and the Android client in the overall architecture.

## Stack
- FastAPI + Uvicorn
- scikit-learn (RandomForestRegressor)
- LangChain + Google Gemini (`gemini-3.6-flash`)
- HuggingFace Transformers (DistilBERT sentiment pipeline)
- Pydantic for request/response validation

## Setup

```bash
git clone https://github.com/pallavkafle/TrialMind.git
cd TrialMind
python -m venv venv
venv\Scripts\activate        # Windows
source venv/bin/activate     # Mac/Linux

pip install -r requirements.txt
```

Create a `.env` file in the project root:
```
GEMINI_API_KEY=your_key_here
```
Get a key at https://aistudio.google.com/apikey

Train the budget model (required before first run):
```bash
python models/train_budget_model.py
```

Run the server:
```bash
uvicorn main:app --reload
```

API docs (Swagger UI): `http://127.0.0.1:8000/docs`

First run of `/analyze-review` downloads the DistilBERT sentiment model (~260MB) automatically — one-time only.

## Endpoints

### `POST /predict-budget`
Two-stage prediction: a deterministic Category-Aware Financial Estimator (tiered per-km transit rates + fixed archetype fees — permits, lodging, ranger fees depending on trip type) generates training labels, then a Random Forest Regressor is trained on top. Evaluated via RMSE/MAE (80/20 split).

**Request:**
```json
{
  "region": "Khumbu",
  "purpose": "Trekking",
  "month": 10,
  "days": 14,
  "group_size": 2,
  "accommodation_tier": "mid",
  "transport_mode": "flight"
}
```
- `region`: Annapurna | Khumbu | Langtang | Manaslu | Upper Mustang | Kanchanjunga | Lower Dolpa | Humla | Pokhara
- `purpose`: Holiday | Pilgrimage | Trekking | Other
- `accommodation_tier`: budget | mid | luxury
- `transport_mode`: bus | jeep | flight | trek

**Response:**
```json
{ "predicted_cost_npr": 132756.86, "per_person": 66378.43 }
```

Current model performance: RMSE 28,466 NPR / MAE 13,910 NPR.

### `POST /optimize-route`
Optimizes visit order for multiple stops using nearest-neighbor construction + 2-opt refinement. Distance uses Haversine scaled by an Archetype-Conditioned Curvature Factor (gamma) to approximate real trail/road distance vs. straight-line distance:

| Archetype | gamma |
|---|---|
| Trekking | 1.45 |
| Homestay | 1.40 |
| Cultural | 1.38 |
| Hiking | 1.35 |
| Wildlife | 1.25 |

**Request:**
```json
{
  "places": [
    {"name": "Lukla", "lat": 27.6869, "lon": 86.7314, "archetype": "Trekking"},
    {"name": "Namche Bazaar", "lat": 27.8069, "lon": 86.7139, "archetype": "Trekking"},
    {"name": "Tengboche", "lat": 27.8361, "lon": 86.7639, "archetype": "Trekking"}
  ],
  "start_index": 0
}
```

**Response:**
```json
{ "ordered_route": ["Lukla", "Namche Bazaar", "Tengboche"], "total_distance_km": 19.35 }
```

Note: this implements the Macro-Transit Spatial Layer only. The Micro-Exploration Spatial Layer (offline GraphML/OSM walking paths within a site) is not yet implemented.

### `POST /generate-itinerary`
Generates a day-by-day itinerary using Gemini via LangChain.

**Request:**
```json
{
  "region": "Khumbu",
  "days": 5,
  "purpose": "Trekking",
  "group_size": 2,
  "interests": ["mountain views", "local culture"]
}
```

**Response:**
```json
{ "itinerary": "Day 1: ...\nDay 2: ...\n..." }
```

### `POST /analyze-review`
Sentiment analysis on a single review, with keyword-based safety escalation.

**Request:**
```json
{ "text": "The trail was beautiful but there was a landslide near Tengboche that blocked the path for hours" }
```

**Response:**
```json
{ "label": "NEGATIVE", "confidence": 0.9941, "safety_flag": "WARNING" }
```
- `safety_flag`: `OK` | `CAUTION` | `WARNING` — WARNING triggers on safety keywords (avalanche, landslide, injury, rescue, etc.) regardless of sentiment polarity; CAUTION on high-confidence negative sentiment without safety keywords.

### `POST /analyze-reviews-batch`
Same as above, batched.

**Request:**
```json
{ "reviews": ["Amazing views, friendly guides", "Trail was closed due to flooding"] }
```

**Response:**
```json
{
  "results": [
    { "text": "Amazing views, friendly guides", "label": "POSITIVE", "confidence": 0.9998, "safety_flag": "OK" },
    { "text": "Trail was closed due to flooding", "label": "NEGATIVE", "confidence": 0.97, "safety_flag": "WARNING" }
  ]
}
```

## Project structure
```
TrialMind/
├── main.py
├── requirements.txt
├── .env                  # not committed
├── routes/
│   ├── __init__.py
│   ├── budget.py
│   ├── route_opt.py
│   ├── itinerary.py
│   └── safety.py
└── models/
    ├── train_budget_model.py
    └── budget_model.pkl   # generated, not committed
```

## Notes for integration (API Gateway / Android)
- All endpoints are `POST` except health check (`GET /`)
- No auth on this service yet — auth is expected to be handled upstream by the API Gateway before requests reach here
- `GEMINI_API_KEY` must be set or `/generate-itinerary` returns `500`
- Accommodation-tier and transport-mode cost multipliers in the budget model are currently assumptions (no public per-tier pricing data available) — pending real numbers from the Localized Lodging Directory (D7)

## Status
- ✅ `/predict-budget`
- ✅ `/optimize-route` (Macro-Transit layer only)
- ✅ `/generate-itinerary`
- ✅ `/analyze-review`, `/analyze-reviews-batch`
- ⬜ Micro-Exploration Spatial Layer (offline GraphML/OSM walking paths) — planned
