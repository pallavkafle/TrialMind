const asyncHandler = require("../utils/asyncHandler");
const { getFeatured } = require("../services/featuredService");

// GET /api/places/featured?limit=8&category=Trekking   (public)
const featured = asyncHandler(async (req, res) => {
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 8, 1), 27);
  const category = req.query.category ? String(req.query.category) : "";
  const places = await getFeatured({ limit, category });
  res.json({ success: true, data: { count: places.length, places, as_of: new Date().toISOString() } });
});

module.exports = { featured };