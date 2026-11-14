const asyncHandler = require("../utils/asyncHandler");
const { callMl } = require("../services/mlClient");
const { nationalityCategory } = require("../utils/nationality");
const DESTINATIONS = require("../data/destinations.json");
const { computePermits, monthIndex, hasPermitData, AS_OF } = require("../services/permitService");

// if the app doesn't send a nationality category, derive it from the user's profile
const withNationality = (req) => ({
  ...req.body,
  nationality_category:
    req.body.nationality_category || nationalityCategory(req.user.nationality),
});

// GET /api/ml/health   (public)
const health = asyncHandler(async (req, res) => {
  res.json({ success: true, data: await callMl("/health") });
});

// GET /api/ml/destinations?category=Trekking&q=everest   (public)
const destinations = asyncHandler(async (req, res) => {
  const category = String(req.query.category || "").toLowerCase();
  const q = String(req.query.q || "").toLowerCase();
  const data = DESTINATIONS.filter(
    (d) =>
      (!category || d.category.toLowerCase() === category) &&
      (!q || d.name.toLowerCase().includes(q))
  );
  res.json({ success: true, data });
});

// POST /api/ml/budget
const round2 = (v) => Math.round(v * 100) / 100;

// One source of truth for permit fees: the permit checker (a permit shared by several
// destinations is counted once). Destinations the checker has no data for keep the ML
// service's dataset-based estimate.
const checkedPermits = ({ stops, estimateByStop, days, group, month, category }) => {
  const monthIdx = monthIndex(month);
  if (monthIdx < 0) return null;
  const covered = stops.filter(hasPermitData);
  const estimatedStops = stops.filter((s) => !hasPermitData(s));
  const r = computePermits({ destinations: covered, days, group, monthIdx, category });
  const estimated = estimatedStops.reduce((sum, s) => sum + (estimateByStop[s] || 0), 0);
  return {
    permit_npr: round2(r.total + estimated),
    detail: {
      as_of: AS_OF,
      source: "permit checker",
      permits: r.permits.map((p) => ({
        id: p.id, name: p.name, confidence: p.confidence,
        fee_per_person_npr: p.fee_per_person_npr, fee_total_npr: p.fee_total_npr,
      })),
      estimated_destinations: estimatedStops,
      needs_verification: r.needsVerification,
    },
  };
};

const budget = asyncHandler(async (req, res) => {
  const body = withNationality(req);
  const data = await callMl("/predict", { method: "POST", body });
  const p = checkedPermits({
    stops: [body.destination],
    estimateByStop: { [body.destination]: data.breakdown.permit_npr },
    days: body.duration_days,
    group: body.group_size,
    month: body.travel_month,
    category: body.nationality_category,
  });
  if (p) {
    const delta = p.permit_npr - data.breakdown.permit_npr;
    data.breakdown.permit_npr = p.permit_npr;
    data.itemized_total_npr = round2(data.itemized_total_npr + delta);
    data.per_person_npr = round2(data.itemized_total_npr / body.group_size);
    data.permit_check = p.detail;
  }
  res.json({ success: true, data });
});

// POST /api/ml/route
const route = asyncHandler(async (req, res) => {
  const data = await callMl("/route", { method: "POST", body: req.body });
  res.json({ success: true, data });
});

// POST /api/ml/itinerary   (Gemini can be slow, so a longer timeout)
const itinerary = asyncHandler(async (req, res) => {
  const body = withNationality(req);
  const data = await callMl("/itinerary", { method: "POST", body, timeoutMs: 90000 });
  const group = Number(body.group_size) || 2; // 2 = the ML service default
  const p = checkedPermits({
    stops: data.route.stops,
    estimateByStop: data.budget.permit_by_stop || {},
    days: data.days.length,
    group,
    month: body.travel_month || "October",
    category: body.nationality_category,
  });
  if (p) {
    const b = data.budget;
    const delta = p.permit_npr - b.permit_npr;
    b.permit_npr = p.permit_npr;
    b.total_npr = round2(b.total_npr + delta);
    b.per_person_npr = round2(b.total_npr / group);
    data.permit_check = p.detail;
  }
  res.json({ success: true, data });
});

// POST /api/ml/analyze-review
const analyzeReview = asyncHandler(async (req, res) => {
  const data = await callMl("/analyze-review", { method: "POST", body: req.body, timeoutMs: 60000 });
  res.json({ success: true, data });
});

// POST /api/ml/parse-trip   body: { text }   (reads a free-text trip request into checked fields)
const parseTrip = asyncHandler(async (req, res) => {
  const text = typeof req.body.text === "string" ? req.body.text.trim() : "";
  if (text.length < 3 || text.length > 1000)
    return res.status(400).json({ success: false, message: "Describe your trip in 3 to 1000 characters" });
  const data = await callMl("/parse-trip", { method: "POST", body: { text }, timeoutMs: 45000 });
  res.json({ success: true, data });
});

module.exports = { health, destinations, budget, route, itinerary, analyzeReview, parseTrip };