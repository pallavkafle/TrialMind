const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const destinations = require("../src/data/destinations.json");

const EXPECTED = [
  "Amaltari Community Homestay", "Annapurna Circuit", "Bandipur Trails", "Bardia National Park",
  "Bhaktapur Durbar Square", "Bhujung Village", "Boudhanath", "Champadevi / Chandragiri Trail",
  "Chitwan National Park", "Dhampus / Australian Camp", "Everest Base Camp", "Ghalegaun", "Gokyo Lakes",
  "Janakpur (Janaki Mandir)", "Langtang Valley", "Lumbini", "Manaslu Circuit", "Mardi Himal", "Muktinath",
  "Nagarkot", "Pashupatinath", "Poon Hill (Ghorepani)", "Sagarmatha National Park",
  "Shivapuri National Park", "Sirubari", "Swayambhunath", "Upper Mustang",
];
const CATEGORIES = ["Cultural", "Trekking", "Hiking", "Homestay", "Wildlife"];
const ML_APP = path.join(__dirname, "..", "..", "ml-service", "app.py");
const METRICS = path.join(__dirname, "..", "..", "ml-service", "models", "metrics.json");

describe("destination list", () => {
  it("has exactly the 27 destinations the ML service knows", () => {
    assert.equal(destinations.length, 27);
    assert.deepEqual(destinations.map((d) => d.name).sort(), EXPECTED.slice().sort());
  });

  it("every destination has valid fields", () => {
    for (const d of destinations) {
      assert.ok(typeof d.name === "string" && d.name.length > 0);
      assert.ok(CATEGORIES.includes(d.category), `${d.name}: category ${d.category}`);
      assert.ok(d.latitude >= 26 && d.latitude <= 30.6, `${d.name}: latitude`);
      assert.ok(d.longitude >= 80 && d.longitude <= 88.3, `${d.name}: longitude`);
      assert.ok(Number.isInteger(d.altitude_m) && d.altitude_m >= 50 && d.altitude_m <= 6500, `${d.name}: altitude`);
      assert.equal(typeof d.is_restricted, "boolean", `${d.name}: is_restricted`);
      assert.equal(typeof d.has_porter, "boolean", `${d.name}: has_porter`);
    }
  });

  it("matches the destination names the model was trained on", (t) => {
    if (!fs.existsSync(METRICS)) return t.skip("ml-service/models/metrics.json not found");
    const trained = JSON.parse(fs.readFileSync(METRICS, "utf8")).categorical_values.Destination;
    assert.deepEqual(destinations.map((d) => d.name).sort(), trained.slice().sort());
  });

  it("matches the ML service on altitude and restricted areas", (t) => {
    if (!fs.existsSync(ML_APP)) return t.skip("ml-service/app.py not found");
    const text = fs.readFileSync(ML_APP, "utf8");
    const block = text.slice(text.indexOf("DEST_META = {"), text.indexOf("COMPONENTS = {"));
    const meta = new Map();
    for (const m of block.matchAll(/"([^"]+)":\s*\((\d+),\s*(\d)\)/g)) meta.set(m[1], [Number(m[2]), Number(m[3])]);
    assert.equal(meta.size, 27, "could not read all 27 destinations from app.py");
    for (const d of destinations) {
      const got = meta.get(d.name);
      assert.ok(got, `${d.name} is missing in the ML service`);
      assert.equal(got[0], d.altitude_m, `${d.name}: altitude differs`);
      assert.equal(got[1], d.is_restricted ? 1 : 0, `${d.name}: restricted flag differs`);
    }
  });
});