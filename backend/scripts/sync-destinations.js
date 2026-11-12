// Rebuilds backend/src/data/destinations.json from the running ML service.
// Usage (ML service running, from the backend folder):  node scripts/sync-destinations.js
require("dotenv").config();
const fs = require("fs");
const path = require("path");

const ML = (process.env.ML_SERVICE_URL || "http://127.0.0.1:8000").replace(/\/$/, "");

(async () => {
  const res = await fetch(`${ML}/destinations`);
  if (!res.ok) throw new Error(`ML service answered ${res.status}`);
  const list = await res.json();
  if (!Array.isArray(list) || list.length === 0) throw new Error("empty destination list");

  const out = list.map((d) => ({
    name: d.name,
    category: d.category,
    latitude: d.latitude,
    longitude: d.longitude,
    altitude_m: d.altitude_m,
    is_restricted: d.is_restricted,
    has_porter: d.has_porter,
  }));
  const file = path.join(__dirname, "..", "src", "data", "destinations.json");
  fs.writeFileSync(file, JSON.stringify(out, null, 2) + "\n");
  console.log(`Wrote ${out.length} destinations -> ${file}`);
})().catch((e) => {
  console.error("sync failed:", e.message);
  process.exit(1);
});