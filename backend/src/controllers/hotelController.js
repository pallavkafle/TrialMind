const asyncHandler = require("../utils/asyncHandler");
const { getDestinationMap } = require("../services/destinationCache");
const { haversineKm } = require("../utils/geo");

let LODGINGS = [];
try {
  LODGINGS = require("../data/hotels.json");
} catch (err) {
  console.warn("hotels.json not found. Run: python -m routing.build_hotels (in ml-service)");
}

const TYPES = ["hotel", "guest_house", "hostel", "motel", "chalet", "apartment", "alpine_hut", "camp_site"];

const num = (v) => (v === undefined || String(v).trim() === "" ? NaN : Number(v));
const bad = (res, message) => res.status(400).json({ success: false, message });

// GET /api/hotels/nearby?destination=..  OR  ?lat=..&lon=..
//     &type=hotel|guest_house|hostel|motel|chalet|apartment|alpine_hut|camp_site
//     &min_stars=1..5 &limit=10 &radius_km=10                         (login required)
const nearby = asyncHandler(async (req, res) => {
  if (LODGINGS.length === 0)
    return res.status(503).json({
      success: false,
      message: "Hotel data not built yet. Run: python -m routing.build_hotels (in ml-service)",
    });

  const { destination, type } = req.query;
  if (type && !TYPES.includes(String(type))) return bad(res, `type must be one of ${TYPES.join(", ")}`);

  let minStars = null;
  if (req.query.min_stars !== undefined) {
    minStars = num(req.query.min_stars);
    if (!Number.isInteger(minStars) || minStars < 1 || minStars > 5)
      return bad(res, "min_stars must be a whole number from 1 to 5");
  }

  let lat, lon, destName;
  if (destination) {
    const d = (await getDestinationMap()).get(String(destination));
    if (!d) return bad(res, `Unknown destination: ${destination}`);
    ({ lat, lon } = d);
    destName = String(destination);
  } else {
    if (req.query.lat === undefined && req.query.lon === undefined)
      return bad(res, "Provide destination or lat and lon");
    lat = num(req.query.lat);
    lon = num(req.query.lon);
    if (!Number.isFinite(lat) || !Number.isFinite(lon) || Math.abs(lat) > 90 || Math.abs(lon) > 180)
      return bad(res, "lat and lon must be valid coordinates");
  }

  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 10, 1), 30);
  const radius = Math.min(Math.max(num(req.query.radius_km) || 10, 1), 50);

  const pool = LODGINGS.filter(
    (h) => (!type || h.type === type) && (minStars === null || (h.stars !== null && h.stars >= minStars))
  );
  const ranked = pool
    .map((h) => ({ ...h, distance_km: Math.round(haversineKm(lat, lon, h.lat, h.lon) * 10) / 10 }))
    .sort((a, b) => a.distance_km - b.distance_km);
  const inRadius = ranked.filter((h) => h.distance_km <= radius);
  const hotels = inRadius.slice(0, limit);

  res.json({
    success: true,
    data: {
      origin: { lat, lon, destination: destName },
      radius_km: radius,
      count: hotels.length,
      total_in_radius: inRadius.length,
      hotels,
      nearest_overall: hotels.length === 0 ? ranked[0] || null : undefined,
      message:
        hotels.length === 0
          ? `No mapped lodging within ${radius} km. The map data may be incomplete, so ask locally.`
          : undefined,
      notes: [
        "Listings come from OpenStreetMap and may be incomplete or out of date.",
        "Prices and availability are not included: contact the property or use the budget estimator for typical costs.",
        "Distances are straight-line.",
      ],
      attribution: "Data © OpenStreetMap contributors (ODbL)",
      dataset_size: LODGINGS.length,
    },
  });
});

module.exports = { nearby };