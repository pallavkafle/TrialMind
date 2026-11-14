const Review = require("../models/Review");
const destinations = require("../data/destinations.json");
const { callMl } = require("./mlClient");
const { computePermits, hasPermitData } = require("./permitService");

const PRIORITY = [
  "Everest Base Camp", "Annapurna Circuit", "Pashupatinath", "Boudhanath", "Chitwan National Park",
  "Lumbini", "Poon Hill (Ghorepani)", "Nagarkot", "Swayambhunath", "Bhaktapur Durbar Square",
  "Langtang Valley", "Mardi Himal", "Bandipur Trails", "Gokyo Lakes", "Manaslu Circuit", "Muktinath",
  "Upper Mustang", "Bardia National Park", "Dhampus / Australian Camp", "Shivapuri National Park",
  "Janakpur (Janaki Mandir)", "Sagarmatha National Park", "Champadevi / Chandragiri Trail",
  "Ghalegaun", "Bhujung Village", "Sirubari", "Amaltari Community Homestay",
];
// typical trip length per destination (days); anything not listed uses 2
const TYPICAL_DAYS = {
  "Everest Base Camp": 12, "Annapurna Circuit": 15, "Langtang Valley": 8, "Mardi Himal": 5,
  "Manaslu Circuit": 14, "Gokyo Lakes": 12, "Upper Mustang": 10, "Muktinath": 5,
  "Poon Hill (Ghorepani)": 4, "Sagarmatha National Park": 10, "Chitwan National Park": 3,
  "Bardia National Park": 3,
};
const GROUP = 2;
const BASIS = "Standard stay, public bus, from Kathmandu, group of 2, October";
const TTL_MS = 6 * 60 * 60 * 1000;
const cache = new Map(); // destination -> { at, value }

// "from NPR X per person": the same calculation as the Budget screen (permits from the permit checker)
const typicalBudget = async (d) => {
  const hit = cache.get(d.name);
  if (hit && Date.now() - hit.at < TTL_MS) return hit.value;
  const days = TYPICAL_DAYS[d.name] || 2;
  try {
    const r = await callMl("/predict", {
      method: "POST",
      timeoutMs: 8000,
      body: {
        starting_city: "Kathmandu", destination: d.name, duration_days: days, group_size: GROUP,
        travel_month: "October", nationality_category: "Foreign", traveler_type: "Group",
        accommodation_tier: "Standard", transport_mode: "Public Bus",
        use_guide: true, use_porter: true, skip_ml: true,
      },
    });
    let total = r.itemized_total_npr;
    if (hasPermitData(d.name)) {
      const p = computePermits({ destinations: [d.name], days, group: GROUP, monthIdx: 9, category: "Foreign" });
      total = total - r.breakdown.permit_npr + p.total;
    }
    const value = { days, per_person_npr: Math.round(total / GROUP), basis: BASIS };
    cache.set(d.name, { at: Date.now(), value });
    return value;
  } catch (err) {
    return null; // ML service down: the card just has no price
  }
};

const getFeatured = async ({ limit, category }) => {
  const rank = (n) => (PRIORITY.indexOf(n) === -1 ? 999 : PRIORITY.indexOf(n));
  let list = [...destinations].sort((a, b) => rank(a.name) - rank(b.name));
  if (category) list = list.filter((d) => d.category.toLowerCase() === category.toLowerCase());
  list = list.slice(0, limit);

  const stats = await Review.aggregate([
    { $match: { destination: { $in: list.map((d) => d.name) } } },
    {
      $group: {
        _id: "$destination",
        count: { $sum: 1 },
        avgRating: { $avg: "$rating" },
        avgPositive: { $avg: "$sentiment.positiveProbability" },
      },
    },
  ]);
  const byName = new Map(stats.map((s) => [s._id, s]));
  const budgets = await Promise.all(list.map(typicalBudget));

  return list.map((d, i) => {
    const s = byName.get(d.name);
    return {
      name: d.name,
      category: d.category,
      altitude_m: d.altitude_m,
      latitude: d.latitude,
      longitude: d.longitude,
      is_restricted: d.is_restricted,
      review_count: s ? s.count : 0,
      avg_rating: s ? Math.round(s.avgRating * 10) / 10 : null,
      score_100: s && s.avgPositive != null ? Math.round(s.avgPositive * 1000) / 10 : null,
      typical: budgets[i],
    };
  });
};

const clearTypicalCache = () => cache.clear();

module.exports = { getFeatured, clearTypicalCache };