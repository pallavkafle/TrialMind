/* TrailMind golden numbers (B11). Runs fixed scenarios against the LIVE system.
 * From the backend folder (services running, demo accounts seeded):
 *   node scripts/golden.js            check: the numbers fixed by B4, plus anything that changed since --record
 *   node scripts/golden.js --record   save the live numbers to docs/golden.json
 * Optional: DEMO_EMAIL, DEMO_PASSWORD, BASE_URL. Needs Node 22; no packages. */
const fs = require("fs");
const path = require("path");

const args = process.argv.slice(2);
const RECORD = args.includes("--record");
const BASE = (process.env.BASE_URL || "http://localhost:5000").replace(/\/$/, "");
const EMAIL = process.env.DEMO_EMAIL || "demo@example.com";
const PASSWORD = process.env.DEMO_PASSWORD || "Demo@1234";
const FILE = path.join(__dirname, "..", "..", "docs", "golden.json");

// fixed by B4 (given by the backend Claude)
const KNOWN = {
  "budget.ebc.permit_npr": 24000,
  "budget.ebc.total_npr": 458927.2,
  "budget.ebc.per_person_npr": 114731.8,
  "itinerary.ref.permit_npr": 11952,
  "itinerary.ref.first_stop": "Chitwan National Park",
};

const EBC = {
  starting_city: "Pokhara", destination: "Everest Base Camp", duration_days: 12, group_size: 4, travel_month: "October",
  nationality_category: "Foreign", traveler_type: "Group", accommodation_tier: "Standard", transport_mode: "Flight",
  use_guide: true, use_porter: true,
};
const ITIN = {
  query: "Pashupatinath and Boudhanath, then a safari in Chitwan", stops: ["Pashupatinath", "Boudhanath", "Chitwan National Park"],
  starting_city: "Pokhara", duration_days: 7, group_size: 4, travel_month: "October", nationality_category: "Foreign",
  traveler_type: "Group", accommodation_tier: "Standard", transport_mode: "Public Bus", use_guide: true, use_porter: false, use_llm: false,
};
const SENTENCE = "Visit Pashupatinath and Boudhanath, then a safari in Chitwan, 7 days, 4 people, October, from Pokhara, " +
  "standard hotels, by bus, with a guide but no porter";

async function call(method, p, token, body) {
  const res = await fetch(BASE + p, {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(120000),
  });
  let json = null;
  try { json = await res.json(); } catch (e) { /* not JSON */ }
  return { ok: res.ok, status: res.status, json };
}

const live = {};
let errors = 0;
const set = (k, v) => { live[k] = v === undefined ? null : v; };
const fail = (name, r) => { errors++; set(`${name}.error`, `HTTP ${r.status} ${(r.json && r.json.message) || ""}`.trim()); };
const same = (a, b) => (typeof a === "number" && typeof b === "number" ? Math.abs(a - b) < 0.011 : JSON.stringify(a) === JSON.stringify(b));
const show = (v) => (typeof v === "number" ? v.toLocaleString("en-US", { maximumFractionDigits: 2 }) : JSON.stringify(v));

(async () => {
  const login = await call("POST", "/api/auth/login", null, { email: EMAIL, password: PASSWORD }).catch(() => null);
  if (!login || !login.ok) {
    console.error(`Could not log in as ${EMAIL}. Start the backend and run: node scripts/seed-demo.js`);
    process.exit(1);
  }
  const token = login.json.token;

  const budget = async (name, body) => {
    const r = await call("POST", "/api/ml/budget", token, body);
    if (!r.ok) return fail(name, r);
    const d = r.json.data;
    set(`${name}.permit_npr`, d.breakdown.permit_npr);
    set(`${name}.total_npr`, d.itemized_total_npr);
    set(`${name}.per_person_npr`, d.per_person_npr);
    set(`${name}.km`, d.start_to_dest_km);
    set(`${name}.guide_npr`, d.breakdown.guide_npr);
    set(`${name}.porter_npr`, d.breakdown.porter_npr);
    set(`${name}.lodging_npr`, d.breakdown.lodging_npr);
    set(`${name}.meals_npr`, d.breakdown.meals_npr);
    set(`${name}.transport_npr`, d.breakdown.transport_npr);
    set(`${name}.ml_estimate_npr`, d.ml_estimate_npr);
  };
  await budget("budget.ebc", EBC);
  await budget("budget.ebc_domestic", { ...EBC, nationality_category: "Domestic" });
  await budget("budget.manaslu_14d", { ...EBC, destination: "Manaslu Circuit", duration_days: 14, group_size: 1 });

  const it = await call("POST", "/api/ml/itinerary", token, ITIN);
  if (it.ok) {
    const d = it.json.data;
    set("itinerary.ref.stops", d.route.stops);
    set("itinerary.ref.first_stop", d.route.stops[0]);
    set("itinerary.ref.days", d.days.length);
    set("itinerary.ref.days_per_stop", d.days_per_stop);
    set("itinerary.ref.total_km", d.route.total_km);
    set("itinerary.ref.permit_npr", d.budget.permit_npr);
    set("itinerary.ref.total_npr", d.budget.total_npr);
    set("itinerary.ref.per_person_npr", d.budget.per_person_npr);
    set("itinerary.ref.source", d.source);
  } else fail("itinerary.ref", it);

  const pt = await call("POST", "/api/ml/parse-trip", token, { text: SENTENCE });
  if (pt.ok) {
    const d = pt.json.data;
    set("parse.stops", d.stops);
    set("parse.days", d.duration_days);
    set("parse.group", d.group_size);
    set("parse.month", d.travel_month);
    set("parse.start", d.starting_city);
    set("parse.tier", d.accommodation_tier);
    set("parse.transport", d.transport_mode);
    set("parse.guide", d.use_guide);
    set("parse.porter", d.use_porter);
    set("parse.source", d.source);
  } else fail("parse", pt);

  const hotels = await call("GET", "/api/hotels/nearby?destination=Nagarkot&limit=3", token);
  if (hotels.ok) {
    const d = hotels.json.data;
    const first = (d.hotels || [])[0] || {};
    set("hotels.nagarkot.count", d.count);
    set("hotels.nagarkot.first_name", first.name);
    set("hotels.nagarkot.first_km", first.distance_km);
  } else fail("hotels.nagarkot", hotels);

  const fac = await call("GET", "/api/health-facilities/nearest?destination=Nagarkot&limit=3");
  if (fac.ok) {
    const list = fac.json.data.facilities || [];
    set("health.nagarkot.count", list.length);
    set("health.nagarkot.first_name", (list[0] || {}).name);
    set("health.nagarkot.first_km", (list[0] || {}).distance_km);
  } else fail("health.nagarkot", fac);

  // ---- compare ----
  let bad = 0;
  console.log("\nFixed by B4:");
  for (const [k, want] of Object.entries(KNOWN)) {
    const ok = same(live[k], want);
    console.log(`  ${ok ? "OK  " : "FAIL"} ${k}: ${show(live[k])}${ok ? "" : `   (expected ${show(want)})`}`);
    if (!ok) bad++;
  }

  if (RECORD) {
    fs.mkdirSync(path.dirname(FILE), { recursive: true });
    fs.writeFileSync(FILE, JSON.stringify({ recorded_at: new Date().toISOString(), values: live }, null, 2) + "\n");
    console.log(`\nRecorded ${Object.keys(live).length} values to docs/golden.json\n`);
    for (const [k, v] of Object.entries(live)) console.log(`  ${k}: ${show(v)}`);
  } else if (fs.existsSync(FILE)) {
    const saved = JSON.parse(fs.readFileSync(FILE, "utf8"));
    let changed = 0;
    for (const [k, v] of Object.entries(saved.values)) {
      if (!same(live[k], v)) { changed++; console.log(`  CHANGED ${k}: was ${show(v)}, now ${show(live[k])}`); }
    }
    console.log(`\nCompared with the recording of ${saved.recorded_at}: ${changed ? changed + " value(s) changed" : "no changes"}`);
    bad += changed;
  } else {
    console.log("\nNo docs/golden.json yet. Run with --record first.");
  }
  if (errors) console.log(`\n${errors} request(s) failed; see the *.error values above.`);
  process.exit(bad || errors ? 1 : 0);
})().catch((e) => {
  console.error("Stopped:", e.message);
  process.exit(1);
});