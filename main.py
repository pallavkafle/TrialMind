from fastapi import FastAPI

app = FastAPI(title="TrailMind Intelligence Service")

@app.get("/")
def health_check():
    return {"status": "running"}