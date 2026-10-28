import pandas as pd
import numpy as np
from sklearn.ensemble import RandomForestRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error, mean_absolute_error
import pickle

np.random.seed(42)
n = 1000

# --- Real anchors (NTB 2025) ---
regions_weighted = (
    ["Annapurna"] * 25 + ["Khumbu"] * 20 + ["Langtang"] * 15 +
    ["Manaslu"] * 10 + ["Upper Mustang"] * 8 + ["Kanchanjunga"] * 7 +
    ["Lower Dolpa"] * 6 + ["Humla"] * 5 + ["Pokhara"] * 4
)
purposes_weighted = ["Holiday"] * 602 + ["Pilgrimage"] * 144 + ["Trekking"] * 142 + ["Other"] * 112
accom_tiers = ["budget", "mid", "luxury"]
transport_modes = ["bus", "jeep", "flight", "trek"]

# Archetype classification per region (per report's γ categories)
region_archetype = {
    "Annapurna": "Trekking", "Khumbu": "Trekking", "Langtang": "Trekking",
    "Manaslu": "Trekking", "Upper Mustang": "Trekking", "Kanchanjunga": "Trekking",
    "Lower Dolpa": "Trekking", "Humla": "Trekking", "Pokhara": "Cultural"
}

df = pd.DataFrame({
    "region": np.random.choice(regions_weighted, n),
    "purpose": np.random.choice(purposes_weighted, n),
    "month": np.random.randint(1, 13, n),
    "days": np.clip(np.random.normal(16.34, 6, n), 1, 30).round().astype(int),
    "group_size": np.random.randint(1, 8, n),
    "accommodation_tier": np.random.choice(accom_tiers, n),
    "transport_mode": np.random.choice(transport_modes, n),
})
df["archetype"] = df["region"].map(region_archetype)

# Trekking-trail walking distance approximation (not straight-line flight distance)
region_km = {
    "Annapurna": 160, "Khumbu": 130, "Langtang": 90, "Manaslu": 180,
    "Upper Mustang": 200, "Kanchanjunga": 220, "Lower Dolpa": 250,
    "Humla": 280, "Pokhara": 30
}
df["distance_km"] = df["region"].map(region_km)

# Per-day baseline cost by tier (teahouse/hotel + food) — this was missing
tier_daily_cost = {"budget": 2500, "mid": 4500, "luxury": 9000}  # NPR/day/person

def transit_cost(row):
    d = row["distance_km"]
    if row["archetype"] == "Trekking":
        # Alpine trek: always apply tiered rate regardless of 150km threshold on distance
        first_leg = min(d, 100) * 5.00
        remaining = max(d - 100, 0) * 18.00
        return first_leg + remaining
    else:
        return d * 5.00

FIXED_FEES = {"Trekking": 4500, "Wildlife": 3000, "Cultural": 1200}
tier_multiplier = {"budget": 0.7, "mid": 1.0, "luxury": 2.2}

df["c_transit"] = df.apply(transit_cost, axis=1)
df["c_fixed"] = df["archetype"].map(FIXED_FEES).fillna(1200)
df["c_daily"] = df["days"] * df["accommodation_tier"].map(tier_daily_cost)

df["cost_npr"] = (
    df["c_transit"] * df["accommodation_tier"].map(tier_multiplier)
    + df["c_fixed"]
    + df["c_daily"] * df["group_size"]  # daily lodging scales with group size too
    + np.random.normal(0, 2000, n)
).clip(lower=1500)
# --- Stage 2: Random Forest trained on Algorithm-1-generated data ---
feature_cols = ["region", "purpose", "month", "days", "group_size", "accommodation_tier", "transport_mode"]
X = pd.get_dummies(df[feature_cols])
y = df["cost_npr"]

X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

model = RandomForestRegressor(n_estimators=150, random_state=42)
model.fit(X_train, y_train)

preds = model.predict(X_test)
rmse = mean_squared_error(y_test, preds) ** 0.5
mae = mean_absolute_error(y_test, preds)
print(f"RMSE: {rmse:.2f} NPR")
print(f"MAE: {mae:.2f} NPR")

with open("models/budget_model.pkl", "wb") as f:
    pickle.dump({"model": model, "columns": X.columns.tolist()}, f)

print("Model trained and saved.")