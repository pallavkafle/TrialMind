from fastapi import FastAPI
from routes import budget, route_opt

app = FastAPI(title="TrailMind Intelligence Service")

app.include_router(budget.router)
app.include_router(route_opt.router)

@app.get("/")
def health_check():
    return {"status": "running"}