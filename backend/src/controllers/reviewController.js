const Review = require("../models/Review");
const asyncHandler = require("../utils/asyncHandler");
const { callMl } = require("../services/mlClient");
const { getDestinationNames } = require("../services/destinationCache");

const bad = (res, message) => res.status(400).json({ success: false, message });

const shape = (r, author) => ({
  id: r._id.toString(),
  destination: r.destination,
  rating: r.rating,
  text: r.text,
  sentiment: r.sentiment && r.sentiment.label
    ? {
        label: r.sentiment.label,
        confidence: r.sentiment.confidence,
        positiveProbability: r.sentiment.positiveProbability,
        safetyFlag: r.sentiment.safetyFlag,
        matchedKeywords: r.sentiment.matchedKeywords,
      }
    : null,
  author: author || (r.user && r.user.name) || undefined,
  createdAt: r.createdAt,
});

// POST /api/reviews   body: { destination, rating, text }
const createReview = asyncHandler(async (req, res) => {
  const { destination, rating, text } = req.body;

  if (typeof destination !== "string" || !destination.trim()) return bad(res, "destination is required");
  const stars = Number(rating);
  if (!Number.isInteger(stars) || stars < 1 || stars > 5)
    return bad(res, "rating must be a whole number from 1 to 5");
  const clean = typeof text === "string" ? text.trim() : "";
  if (clean.length < 3 || clean.length > 2000) return bad(res, "text must be 3 to 2000 characters");

  const names = await getDestinationNames();
  if (!names.has(destination.trim())) return bad(res, `Unknown destination: ${destination}`);

  let sentiment;
  try {
    const a = await callMl("/analyze-review", { method: "POST", body: { text: clean }, timeoutMs: 60000 });
    sentiment = {
      label: a.label,
      confidence: a.confidence,
      positiveProbability: a.positive_probability,
      safetyFlag: a.safety_flag,
      matchedKeywords: a.matched_keywords,
    };
  } catch (err) {
    console.error("Sentiment analysis failed:", err.message); // keep the review anyway
  }

  const review = await Review.create({
    user: req.user._id,
    destination: destination.trim(),
    rating: stars,
    text: clean,
    sentiment,
  });

  res.status(201).json({
    success: true,
    analysisAvailable: Boolean(sentiment),
    data: shape(review, req.user.name),
  });
});

// GET /api/reviews?destination=Lumbini&limit=20   (public)
const listReviews = asyncHandler(async (req, res) => {
  const destination = String(req.query.destination || "").trim();
  if (!destination) return bad(res, "destination query parameter is required");
  const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);

  const [items, stats] = await Promise.all([
    Review.find({ destination }).sort({ createdAt: -1 }).limit(limit).populate("user", "name"),
    Review.aggregate([
      { $match: { destination } },
      {
        $group: {
          _id: null,
          count: { $sum: 1 },
          avgRating: { $avg: "$rating" },
          analyzed: { $sum: { $cond: [{ $ifNull: ["$sentiment.label", false] }, 1, 0] } },
          positive: { $sum: { $cond: [{ $eq: ["$sentiment.label", "POSITIVE"] }, 1, 0] } },
          avgPositive: { $avg: "$sentiment.positiveProbability" },
          warnings: { $sum: { $cond: [{ $eq: ["$sentiment.safetyFlag", "WARNING"] }, 1, 0] } },
          cautions: { $sum: { $cond: [{ $eq: ["$sentiment.safetyFlag", "CAUTION"] }, 1, 0] } },
        },
      },
    ]),
  ]);

  const s = stats[0];
  const summary = {
    destination,
    count: s ? s.count : 0,
    avgRating: s ? Math.round(s.avgRating * 10) / 10 : null,
    analyzed: s ? s.analyzed : 0,
    positivePct: s && s.analyzed ? Math.round((1000 * s.positive) / s.analyzed) / 10 : null,
    score100: s && s.analyzed ? Math.round(s.avgPositive * 1000) / 10 : null,
    warningCount: s ? s.warnings : 0,
    cautionCount: s ? s.cautions : 0,
  };

  res.json({ success: true, summary, data: items.map((r) => shape(r)) });
});

// GET /api/reviews/mine
const myReviews = asyncHandler(async (req, res) => {
  const items = await Review.find({ user: req.user._id }).sort({ createdAt: -1 }).limit(100);
  res.json({ success: true, count: items.length, data: items.map((r) => shape(r, req.user.name)) });
});

// DELETE /api/reviews/:id   (own reviews only)
const deleteReview = asyncHandler(async (req, res) => {
  const r = await Review.findOneAndDelete({ _id: req.params.id, user: req.user._id });
  if (!r) return res.status(404).json({ success: false, message: "Review not found" });
  res.json({ success: true, message: "Review deleted" });
});

module.exports = { createReview, listReviews, myReviews, deleteReview };