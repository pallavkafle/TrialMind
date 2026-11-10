const asyncHandler = require("../utils/asyncHandler");
const { getDestinationMap } = require("../services/destinationCache");
const { getForecast } = require("../services/weatherService");

const num = (v) => (v === undefined || String(v).trim() === "" ? NaN : Number(v));
const bad = (res, message) => res.status(400).json({ success: false, message });

// GET /api/weather?lat=..&lon=..      or      GET /api/weather?destination=..     (login required)
const forecast = asyncHandler(async (req, res) => {
  let lat, lon, destination;

  if (req.query.lat !== undefined || req.query.lon !== undefined) {
    lat = num(req.query.lat);
    lon = num(req.query.lon);
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
      return bad(res, "lat and lon must be valid coordinates");
  } else if (req.query.destination) {
    const d = (await getDestinationMap()).get(String(req.query.destination));
    if (!d) return bad(res, `Unknown destination: ${req.query.destination}`);
    ({ lat, lon } = d);
    destination = String(req.query.destination);
  } else {
    return bad(res, "Provide lat and lon, or a destination");
  }

  const data = await getForecast(lat, lon);
  res.json({ success: true, data: { location: { lat, lon, destination }, ...data } });
});

module.exports = { forecast };