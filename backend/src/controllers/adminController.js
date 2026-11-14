const asyncHandler = require("../utils/asyncHandler");
const User = require("../models/User");
const Trip = require("../models/Trip");
const Review = require("../models/Review");
const Favorite = require("../models/Favorite");
const PermitOverride = require("../models/PermitOverride");
const { PERMITS } = require("../data/permits");
const { effectivePermit } = require("../services/permitService");
const { loadPermitOverrides } = require("../services/permitOverrides");
const { clearTypicalCache } = require("../services/featuredService");

const bad = (res, message, status = 400) => res.status(status).json({ success: false, message });
const FEE_KEYS = ["nepali", "saarc", "foreign"];

// GET /api/admin/stats
const stats = asyncHandler(async (req, res) => {
  const [users, admins, trips, reviews, favorites, flags, rating] = await Promise.all([
    User.countDocuments(),
    User.countDocuments({ role: "admin" }),
    Trip.countDocuments(),
    Review.countDocuments(),
    Favorite.countDocuments(),
    Review.aggregate([{ $group: { _id: "$sentiment.safetyFlag", n: { $sum: 1 } } }]),
    Review.aggregate([{ $group: { _id: null, avg: { $avg: "$rating" } } }]),
  ]);
  res.json({
    success: true,
    data: {
      users, admins, trips, reviews, favorites,
      reviews_by_safety_flag: Object.fromEntries(flags.map((f) => [f._id || "UNANALYZED", f.n])),
      avg_rating: rating[0] ? Math.round(rating[0].avg * 10) / 10 : null,
    },
  });
});

// GET /api/admin/reviews?destination=&flag=WARNING&limit=20
const listReviews = asyncHandler(async (req, res) => {
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 20, 1), 100);
  const filter = {};
  if (req.query.destination) filter.destination = String(req.query.destination);
  if (req.query.flag) filter["sentiment.safetyFlag"] = String(req.query.flag).toUpperCase();
  const items = await Review.find(filter).sort({ createdAt: -1 }).limit(limit).populate("user", "name email");
  res.json({
    success: true,
    count: items.length,
    data: items.map((r) => ({
      id: r._id.toString(),
      destination: r.destination,
      rating: r.rating,
      text: r.text,
      sentiment: r.sentiment && r.sentiment.label
        ? { label: r.sentiment.label, safetyFlag: r.sentiment.safetyFlag, matchedKeywords: r.sentiment.matchedKeywords }
        : null,
      author: r.user ? { name: r.user.name, email: r.user.email } : null,
      createdAt: r.createdAt,
    })),
  });
});

// DELETE /api/admin/reviews/:id   (moderation: any review)
const deleteReview = asyncHandler(async (req, res) => {
  const r = await Review.findByIdAndDelete(req.params.id);
  if (!r) return bad(res, "Review not found", 404);
  res.json({ success: true, message: "Review deleted" });
});

const describePermit = (id, override) => {
  const p = effectivePermit(id);
  return {
    id,
    name: p.name,
    authority: p.authority,
    fee: p.fee || null,
    restricted: Boolean(p.restricted),
    confidence: p.confidence,
    note: p.note,
    overridden: Boolean(override),
    updated_at: override ? override.updatedAt : null,
  };
};

// GET /api/admin/permits
const listPermits = asyncHandler(async (req, res) => {
  const overrides = new Map((await PermitOverride.find({})).map((o) => [o.permitId, o]));
  const ids = Object.keys(PERMITS);
  res.json({ success: true, count: ids.length, data: ids.map((id) => describePermit(id, overrides.get(id))) });
});

// PATCH /api/admin/permits/:id   body: { fee?: { nepali?, saarc?, foreign? }, confidence?, note? }
const updatePermit = asyncHandler(async (req, res) => {
  const id = req.params.id;
  const base = PERMITS[id];
  if (!base) return bad(res, "Unknown permit", 404);
  const { fee, confidence, note } = req.body;
  if (fee === undefined && confidence === undefined && note === undefined)
    return bad(res, "Send fee, confidence or note");

  const set = {};
  if (fee !== undefined) {
    if (!base.fee) return bad(res, "Restricted-area fees are USD formulas: edit them in data/permits.js");
    if (typeof fee !== "object" || fee === null || Array.isArray(fee)) return bad(res, "fee must be an object");
    for (const [k, v] of Object.entries(fee)) {
      if (!FEE_KEYS.includes(k)) return bad(res, `Unknown fee key: ${k}`);
      if (v !== null && !(Number.isFinite(v) && v >= 0 && v <= 1000000))
        return bad(res, `fee.${k} must be null or a number from 0 to 1000000`);
      set[`fee.${k}`] = v;
    }
  }
  if (confidence !== undefined) {
    if (!["high", "medium", "low"].includes(confidence)) return bad(res, "confidence must be high, medium or low");
    set.confidence = confidence;
  }
  if (note !== undefined) {
    if (typeof note !== "string" || note.length > 400) return bad(res, "note must be text up to 400 characters");
    set.note = note.trim();
  }
  set.updatedBy = req.user._id;

  const doc = await PermitOverride.findOneAndUpdate(
    { permitId: id },
    { $set: set },
    { new: true, upsert: true, runValidators: true }
  );
  await loadPermitOverrides();
  clearTypicalCache();
  res.json({ success: true, data: describePermit(id, doc) });
});

// DELETE /api/admin/permits/:id/override   (back to the defaults in data/permits.js)
const resetPermit = asyncHandler(async (req, res) => {
  const id = req.params.id;
  if (!PERMITS[id]) return bad(res, "Unknown permit", 404);
  await PermitOverride.deleteOne({ permitId: id });
  await loadPermitOverrides();
  clearTypicalCache();
  res.json({ success: true, data: describePermit(id, null) });
});

module.exports = { stats, listReviews, deleteReview, listPermits, updatePermit, resetPermit };