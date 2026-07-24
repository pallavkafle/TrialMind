from fastapi import FastAPI
from routes import budget

app = FastAPI(title="TrailMind Intelligence Service")

app.include_router(budget.router)

@app.get("/")
def health_check():
    return {"status": "running"}