const asyncHandler = require("../utils/asyncHandler");
const { callMl } = require("../services/mlClient");
const { nationalityCategory } = require("../utils/nationality");

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
  const qs = new URLSearchParams();
  if (req.query.category) qs.set("category", String(req.query.category));
  if (req.query.q) qs.set("q", String(req.query.q));
  const suffix = qs.toString() ? `?${qs}` : "";
  res.json({ success: true, data: await callMl(`/destinations${suffix}`) });
});

// POST /api/ml/budget
const budget = asyncHandler(async (req, res) => {
  const data = await callMl("/predict", { method: "POST", body: withNationality(req) });
  res.json({ success: true, data });
});

// POST /api/ml/route
const route = asyncHandler(async (req, res) => {
  const data = await callMl("/route", { method: "POST", body: req.body });
  res.json({ success: true, data });
});

// POST /api/ml/itinerary   (Gemini can be slow, so a longer timeout)
const itinerary = asyncHandler(async (req, res) => {
  const data = await callMl("/itinerary", {
    method: "POST",
    body: withNationality(req),
    timeoutMs: 90000,
  });
  res.json({ success: true, data });
});

// POST /api/ml/analyze-review
const analyzeReview = asyncHandler(async (req, res) => {
  const data = await callMl("/analyze-review", { method: "POST", body: req.body, timeoutMs: 60000 });
  res.json({ success: true, data });
});

module.exports = { health, destinations, budget, route, itinerary, analyzeReview };