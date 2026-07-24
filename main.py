from fastapi import FastAPI
from routes import budget, route_opt, itinerary, safety

app = FastAPI(title="TrailMind Intelligence Service")

app.include_router(budget.router)
app.include_router(route_opt.router)
app.include_router(itinerary.router)
app.include_router(safety.router)

@app.get("/")
def health_check():
    return {"status": "running"}