from fastapi import APIRouter
from pydantic import BaseModel
import pickle
import pandas as pd

router = APIRouter()

with open("models/budget_model.pkl", "rb") as f:
    saved = pickle.load(f)
    model = saved["model"]
    columns = saved["columns"]

class BudgetRequest(BaseModel):
    region: str
    month: int
    days: int
    group_size: int
    accommodation_tier: str  # budget | mid | luxury
    transport_mode: str      # bus | jeep | flight | trek

@router.post("/predict-budget")
def predict_budget(req: BudgetRequest):
    input_df = pd.DataFrame([req.dict()])
    input_encoded = pd.get_dummies(input_df)
    input_encoded = input_encoded.reindex(columns=columns, fill_value=0)

    prediction = model.predict(input_encoded)[0]
    return {
        "predicted_cost_npr": round(float(prediction), 2),
        "per_person": round(float(prediction) / req.group_size, 2)
    }