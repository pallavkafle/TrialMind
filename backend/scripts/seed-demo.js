/* TrailMind demo data (B11). Fills 4 demo accounts through the real API.
 * Usage (MongoDB, ml-service and backend running), from the backend folder:
 *   node scripts/seed-demo.js           create what is missing (safe to run again)
 *   node scripts/seed-demo.js --reset   remove what the demo accounts created (trips, reviews, saved places)
 * Needs Node 22 (built-in fetch). No packages. The reviews are SAMPLE data written for testing. */
const args = process.argv.slice(2);
const RESET = args.includes("--reset");
const at = args.indexOf("--base");
const BASE = (at >= 0 ? args[at + 1] : process.env.BASE_URL || "http://localhost:5000").replace(/\/$/, "");

const PASSWORD = "Demo@1234";
const USERS = [
  { name: "Demo Traveller", email: "demo@example.com", nationality: "DE" },
  { name: "Sita Sharma", email: "sita.demo@example.com", nationality: "NP" },
  { name: "Alex Meyer", email: "alex.demo@example.com", nationality: "US" },
  { name: "Priya Nair", email: "priya.demo@example.com", nationality: "IN" },
];

const COMMON = {
  starting_city: "Pokhara", group_size: 4, travel_month: "October", nationality_category: "Foreign",
  traveler_type: "Group", accommodation_tier: "Standard", transport_mode: "Public Bus",
  use_guide: true, use_porter: false, use_llm: false,
};
const TRIPS = [
  {
    title: "Temples and jungle (reference trip)",
    req: { ...COMMON, query: "Pashupatinath and Boudhanath, then a safari in Chitwan", duration_days: 7,
      stops: ["Pashupatinath", "Boudhanath", "Chitwan National Park"] },
  },
  {
    title: "Everest Base Camp trek",
    req: { ...COMMON, query: "Everest Base Camp trek", duration_days: 12, transport_mode: "Flight", use_porter: true,
      stops: ["Everest Base Camp"] },
  },
  {
    title: "Lumbini and Bardia (family)",
    req: { ...COMMON, query: "A relaxed family trip to Lumbini and Bardia", duration_days: 6, group_size: 3,
      starting_city: "Kathmandu", accommodation_tier: "Budget", stops: ["Lumbini", "Bardia National Park"] },
  },
];

// destination, stars, text. Written for the demo; they are not real traveller reviews.
const REVIEWS = {
  "sita.demo@example.com": [
    ["Lumbini", 5, "Peaceful and clean. We spent a full day walking between the monasteries and the Maya Devi temple area. Cycling is a lovely way to see it and the value is very good."],
    ["Pashupatinath", 4, "Very crowded during the evening aarti but the atmosphere is special. Go early in the morning if you want quiet. Agree the guide price at the gate before you start."],
    ["Chitwan National Park", 5, "We saw rhinos on the jungle walk and a gharial from the canoe. The guides were knowledgeable and careful with the group."],
    ["Poon Hill (Ghorepani)", 4, "The sunrise over the Annapurna range was worth the early start. The last stretch of steps is tiring but there are plenty of tea houses."],
  ],
  "alex.demo@example.com": [
    ["Everest Base Camp", 5, "A hard but unforgettable trek. We had no altitude sickness thanks to our guide's slow pace. Book the Lukla flight early because flights get cancelled in bad weather."],
    ["Annapurna Circuit", 3, "Beautiful trek, but a landslide blocked the trail near Tal for a day and we had to wait for the path to be cleared. Check the road and trail conditions before you leave."],
    ["Langtang Valley", 4, "Quiet trail with friendly lodges and good food. The valley gets cold at night, so bring a warm sleeping bag."],
    ["Upper Mustang", 5, "Dry, dramatic landscapes unlike anywhere else in Nepal. The permit is easy to arrange through an agency. The jeep ride is bumpy but the villages are beautiful."],
  ],
  "priya.demo@example.com": [
    ["Bhaktapur Durbar Square", 4, "Beautiful old buildings and a lively pottery square. The entry ticket is checked at the gate, so keep it with you."],
    ["Nagarkot", 2, "Overpriced and disappointing. Our room was dirty, the food was cold and the staff were rude."],
    ["Mardi Himal", 5, "Quiet trail with a stunning ridge walk. The teahouses are basic but the staff are kind. Carry enough water."],
    ["Bardia National Park", 4, "Far less crowded than Chitwan. We saw deer and heard a tiger at night but did not see one. The road journey is long."],
  ],
};
const FAVORITES = ["Poon Hill (Ghorepani)", "Everest Base Camp", "Lumbini"]; // saved in this order, so Lumbini is the newest

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const tally = { created: 0, skipped: 0, failed: 0 };
const say = (kind, text) => {
  tally[kind]++;
  console.log(`  ${{ created: "+", skipped: "=", failed: "!" }[kind]} ${text}`);
};

async function api(method, path, { token, body } = {}) {
  for (let attempt = 0; ; attempt++) {
    const res = await fetch(BASE + path, {
      method,
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: AbortSignal.timeout(120000),
    });
    let json = null;
    try { json = await res.json(); } catch (e) { /* not JSON */ }
    const own = json && /already written|Daily review limit|Trip limit/i.test(json.message || "");
    if (res.status === 429 && !own && attempt < 4) {
      const wait = Math.min(Number(res.headers.get("retry-after")) || 15, 60);
      console.log(`    (rate limit, waiting ${wait}s)`);
      await sleep(wait * 1000 + 500);
      continue;
    }
    return { status: res.status, ok: res.ok, json };
  }
}
const why = (r) => (r.json && r.json.message) || `HTTP ${r.status}`;

async function ensureUser(u) {
  let r = await api("POST", "/api/auth/login", { body: { email: u.email, password: PASSWORD } });
  if (r.status === 401) {
    r = await api("POST", "/api/auth/register", { body: { name: u.name, email: u.email, password: PASSWORD, nationality: u.nationality } });
    if (r.status !== 201) throw new Error(`could not create ${u.email}: ${why(r)}`);
    say("created", `account ${u.email}`);
  } else if (r.status !== 200) {
    throw new Error(`could not log in as ${u.email}: ${why(r)}`);
  } else {
    say("skipped", `account ${u.email} (exists)`);
  }
  return r.json.token;
}

async function resetUser(u, token) {
  const data = async (p) => (((await api("GET", p, { token })).json || {}).data) || [];
  for (const t of await data("/api/trips")) { await api("DELETE", `/api/trips/${t.id}`, { token }); say("created", `${u.email}: removed trip "${t.title}"`); }
  for (const r of await data("/api/reviews/mine")) { await api("DELETE", `/api/reviews/${r.id}`, { token }); say("created", `${u.email}: removed review of ${r.destination}`); }
  for (const d of await data("/api/favorites")) { await api("POST", "/api/favorites/toggle", { token, body: { destination: d } }); say("created", `${u.email}: removed saved place ${d}`); }
}

async function seedTrips(u, token) {
  const have = new Set((((await api("GET", "/api/trips", { token })).json || {}).data || []).map((t) => t.title));
  for (const t of TRIPS) {
    if (have.has(t.title)) { say("skipped", `trip "${t.title}" (exists)`); continue; }
    const it = await api("POST", "/api/ml/itinerary", { token, body: t.req });
    if (!it.ok) { say("failed", `trip "${t.title}": ${why(it)}`); continue; }
    const saved = await api("POST", "/api/trips", { token, body: { title: t.title, request: t.req, itinerary: it.json.data } });
    saved.status === 201 ? say("created", `trip "${t.title}"`) : say("failed", `trip "${t.title}": ${why(saved)}`);
  }
}

async function seedReviews(u, token) {
  const mine = (((await api("GET", "/api/reviews/mine", { token })).json || {}).data || []);
  for (const [destination, rating, text] of REVIEWS[u.email] || []) {
    if (mine.some((r) => r.destination === destination && r.text === text)) { say("skipped", `review of ${destination} (exists)`); continue; }
    const r = await api("POST", "/api/reviews", { token, body: { destination, rating, text } });
    if (r.status === 201 || r.status === 200) {
      const s = r.json && r.json.data && r.json.data.sentiment;
      say("created", `review of ${destination}${s ? ` [${s.label}, ${s.safetyFlag}]` : " [no sentiment: ML service was not reachable]"}`);
    } else {
      say("failed", `review of ${destination}: ${why(r)}`);
    }
  }
}

async function seedFavorites(u, token) {
  const have = new Set((((await api("GET", "/api/favorites", { token })).json || {}).data || []));
  for (const d of FAVORITES) {
    if (have.has(d)) { say("skipped", `saved place ${d} (exists)`); continue; }
    const r = await api("POST", "/api/favorites/toggle", { token, body: { destination: d } });
    r.ok ? say("created", `saved place ${d}`) : say("failed", `saved place ${d}: ${why(r)}`);
  }
}

(async () => {
  console.log(`TrailMind demo data -> ${BASE}${RESET ? "   (RESET)" : ""}`);
  try {
    const h = await fetch(`${BASE}/api/ml/health`, { signal: AbortSignal.timeout(15000) });
    if (!h.ok) console.log("  WARNING: the ML service is not answering; trips and review analysis will fail.");
  } catch (e) {
    console.error(`Cannot reach the backend at ${BASE}. Start it first (npm run dev).`);
    process.exit(1);
  }

  for (const u of USERS) {
    console.log(`\n${u.name} <${u.email}>`);
    const token = await ensureUser(u);
    if (RESET) { await resetUser(u, token); continue; }
    if (u.email === "demo@example.com") { await seedTrips(u, token); await seedFavorites(u, token); }
    await seedReviews(u, token);
  }

  console.log(`\n==== ${RESET ? "removed" : "created"} ${tally.created}, already there ${tally.skipped}, failed ${tally.failed} ====`);
  if (!RESET) {
    console.log(`Log in with demo@example.com / ${PASSWORD}   (Germany: Foreign prices)`);
    console.log(`Other accounts: sita.demo@ (Nepal, Domestic), alex.demo@ (USA), priya.demo@ (India, SAARC), same password`);
  }
  process.exit(tally.failed ? 1 : 0);
})().catch((e) => {
  console.error("\nStopped:", e.message);
  process.exit(1);
});