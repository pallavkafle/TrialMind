const Favorite = require("../models/Favorite");
const asyncHandler = require("../utils/asyncHandler");
const { getDestinationNames } = require("../services/destinationCache");

// GET /api/favorites -> { success, count, data: ["Lumbini", ...] } newest first
const listFavorites = asyncHandler(async (req, res) => {
  const items = await Favorite.find({ user: req.user._id }).sort({ createdAt: -1 }).limit(200);
  res.json({ success: true, count: items.length, data: items.map((f) => f.destination) });
});

// POST /api/favorites/toggle   body: { destination } -> { success, data: { destination, favorite } }
const toggleFavorite = asyncHandler(async (req, res) => {
  const destination = typeof req.body.destination === "string" ? req.body.destination.trim() : "";
  if (!destination) return res.status(400).json({ success: false, message: "destination is required" });
  if (!(await getDestinationNames()).has(destination))
    return res.status(400).json({ success: false, message: `Unknown destination: ${destination}` });

  const removed = await Favorite.findOneAndDelete({ user: req.user._id, destination });
  if (removed) return res.json({ success: true, data: { destination, favorite: false } });

  try {
    await Favorite.create({ user: req.user._id, destination });
  } catch (err) {
    if (err.code !== 11000) throw err; // a double tap already created it
  }
  res.json({ success: true, data: { destination, favorite: true } });
});

module.exports = { listFavorites, toggleFavorite };