import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
import pickle

np.random.seed(42)
n = 800

# --- Real anchors (Nepal Tourism Statistics 2025) ---
AVG_DAILY_SPEND_USD = 33.09          # Table 1
USD_TO_NPR = 133                     # ASSUMPTION - update with live FX in prod
AVG_DAILY_SPEND_NPR = AVG_DAILY_SPEND_USD * USD_TO_NPR   # ~4401 NPR/day baseline

regions_weighted = (
    ["Annapurna"] * 25 + ["Khumbu"] * 20 + ["Langtang"] * 15 +
    ["Manaslu"] * 10 + ["Upper Mustang"] * 8 + ["Kanchanjunga"] * 7 +
    ["Lower Dolpa"] * 6 + ["Humla"] * 5 + ["Pokhara"] * 4
)  # weighted by Table 5.2 popularity

purposes_weighted = (
    ["Holiday"] * 602 + ["Pilgrimage"] * 144 +
    ["Trekking"] * 142 + ["Other"] * 112
)  # Table 4.1 real split, per 1000

accom_tiers = ["budget", "mid", "luxury"]          # ASSUMPTION - no tier pricing in report
transport_modes = ["bus", "jeep", "flight", "trek"]  # ASSUMPTION

df = pd.DataFrame({
    "region": np.random.choice(regions_weighted, n),
    "purpose": np.random.choice(purposes_weighted, n),
    "month": np.random.randint(1, 13, n),
    # length of stay centered on real avg (16.34), with spread
    "days": np.clip(np.random.normal(16.34, 6, n), 1, 30).round().astype(int),
    "group_size": np.random.randint(1, 8, n),
    "accommodation_tier": np.random.choice(accom_tiers, n),
    "transport_mode": np.random.choice(transport_modes, n),
})

# --- Cost model anchored to real avg daily spend ---
tier_multiplier = {"budget": 0.6, "mid": 1.0, "luxury": 2.4}   # ASSUMPTION
transport_flat = {"bus": 500, "jeep": 1500, "flight": 6000, "trek": 300}  # ASSUMPTION
purpose_multiplier = {"Trekking": 1.3, "Pilgrimage": 0.9, "Holiday": 1.0, "Other": 1.1}  # ASSUMPTION

df["cost_npr"] = (
    df["days"] * AVG_DAILY_SPEND_NPR * df["accommodation_tier"].map(tier_multiplier)
    * df["purpose"].map(purpose_multiplier)
    + df["transport_mode"].map(transport_flat)
    + df["group_size"] * 200
    + np.random.normal(0, 800, n)
).clip(lower=500)

X = pd.get_dummies(df.drop(columns=["cost_npr"]))
y = df["cost_npr"]

model = RandomForestRegressor(n_estimators=150, random_state=42)
model.fit(X, y)

with open("models/budget_model.pkl", "wb") as f:
    pickle.dump({"model": model, "columns": X.columns.tolist()}, f)

print("Model trained. Columns:", X.columns.tolist())
print(f"Avg predicted cost: {model.predict(X).mean():.0f} NPR")