# TrailMind

Android app for planning trips in Nepal: AI-written itineraries, optimized routes, itemized budgets, permit costs,
hospitals and hotels near you, weather, currency, reviews with safety flags, saved trips.
Himalaya College of Engineering, Tribhuvan University, major project CT 707.

## Architecture
    frontend/    Android (Kotlin, Jetpack Compose)  ->  talks only to the Node gateway
    backend/     Node.js + Express + MongoDB (JWT login, trips, reviews, permits, hotels, hospitals, weather, currency, admin)
    ml-service/  FastAPI (budget, routing, itinerary, sentiment)

## Run (three terminals + Android Studio)
1. MongoDB running, then in backend/: copy .env.example to .env, `npm install`, `npm run dev`  (port 5000)
2. In ml-service/: see ml-service/README.md  (port 8000)
3. Open frontend/ in Android Studio. The emulator reaches the backend at http://10.0.2.2:5000

## Data sources and credits
OpenStreetMap contributors (ODbL): walking graphs, hospitals, hotels. Open-Meteo.com (CC BY 4.0): weather.
ExchangeRate-API (open access): currency. Permit fees: researched from agency and official pages, verify before travel.
The budget dataset is semi-synthetic (built from published rate sheets), so model accuracy shows consistency with those sheets.

## Security notes
JWT sessions, bcrypt (cost 10), Helmet headers, rate limits on login and AI endpoints, no secrets in git.
For production, put the API behind HTTPS and set FORCE_HTTPS=1.