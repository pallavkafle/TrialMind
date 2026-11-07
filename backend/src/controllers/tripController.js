const Trip = require("../models/Trip");
const asyncHandler = require("../utils/asyncHandler");

const shape = (t, full = false) => ({
  id: t._id.toString(),
  title: t.title,
  stops: t.stops,
  startingCity: t.startingCity,
  durationDays: t.durationDays,
  groupSize: t.groupSize,
  totalNpr: t.totalNpr,
  createdAt: t.createdAt,
  ...(full ? { request: t.request, itinerary: t.itinerary } : {}),
});

const bad = (res, message) => res.status(400).json({ success: false, message });

// POST /api/trips   body: { title?, request, itinerary }  (itinerary = /api/ml/itinerary response data)
const createTrip = asyncHandler(async (req, res) => {
  const { title, request = {}, itinerary } = req.body;

  if (!itinerary || !Array.isArray(itinerary.days) || itinerary.days.length === 0)
    return bad(res, "itinerary.days is required");
  if (!itinerary.route || !Array.isArray(itinerary.route.stops) || itinerary.route.stops.length === 0)
    return bad(res, "itinerary.route.stops is required");
  if (!itinerary.budget || typeof itinerary.budget.total_npr !== "number")
    return bad(res, "itinerary.budget.total_npr is required");
  if (!Number.isInteger(request.group_size))
    return bad(res, "request.group_size is required");

  const trip = await Trip.create({
    user: req.user._id,
    title: (typeof title === "string" && title.trim()) || itinerary.title || "My trip",
    stops: itinerary.route.stops,
    startingCity: request.starting_city,
    durationDays: itinerary.days.length,
    groupSize: request.group_size,
    totalNpr: itinerary.budget.total_npr,
    request,
    itinerary,
  });

  res.status(201).json({ success: true, data: shape(trip, true) });
});

// GET /api/trips   (summaries, newest first)
const listTrips = asyncHandler(async (req, res) => {
  const trips = await Trip.find({ user: req.user._id })
    .select("title stops startingCity durationDays groupSize totalNpr createdAt")
    .sort({ createdAt: -1 })
    .limit(100);
  res.json({ success: true, count: trips.length, data: trips.map((t) => shape(t)) });
});

// GET /api/trips/:id
const getTrip = asyncHandler(async (req, res) => {
  const trip = await Trip.findOne({ _id: req.params.id, user: req.user._id });
  if (!trip) return res.status(404).json({ success: false, message: "Trip not found" });
  res.json({ success: true, data: shape(trip, true) });
});

// DELETE /api/trips/:id
const deleteTrip = asyncHandler(async (req, res) => {
  const trip = await Trip.findOneAndDelete({ _id: req.params.id, user: req.user._id });
  if (!trip) return res.status(404).json({ success: false, message: "Trip not found" });
  res.json({ success: true, message: "Trip deleted" });
});

module.exports = { createTrip, listTrips, getTrip, deleteTrip };