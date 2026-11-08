const asyncHandler = require("../utils/asyncHandler");
const { getDestinationMap } = require("../services/destinationCache");
const { haversineKm } = require("../utils/geo");

let FACILITIES = [];
try {
  FACILITIES = require("../data/healthcare.json");
} catch (err) {
  console.warn("healthcare.json not found. Run: python -m routing.build_healthcare (in ml-service)");
}

const TYPES = ["hospital", "clinic", "doctors", "rescue"];
const EMERGENCY_NUMBERS = [
  { service: "Police", number: "100" },
  { service: "Ambulance", number: "102" },
  { service: "Fire brigade", number: "101" },
  { service: "Tourist police", number: "1144" },
];

const num = (v) => (v === undefined || String(v).trim() === "" ? NaN : Number(v));
const bad = (res, message) => res.status(400).json({ success: false, message });

// GET /api/health-facilities/nearest?destination=..  OR  ?lat=..&lon=..
//     &type=hospital|clinic|doctors|rescue &limit=5 &radius_km=50      (public)
const nearest = asyncHandler(async (req, res) => {
  if (FACILITIES.length === 0)
    return res.status(503).json({
      success: false,
      message: "Healthcare data not built yet. Run: python -m routing.build_healthcare (in ml-service)",
    });

  const { destination, type } = req.query;
  if (type && !TYPES.includes(String(type)))
    return bad(res, `type must be one of ${TYPES.join(", ")}`);

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

  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 5, 1), 20);
  const radius = Math.min(Math.max(num(req.query.radius_km) || 50, 1), 300);

  const pool = type ? FACILITIES.filter((f) => f.type === type) : FACILITIES;
  const ranked = pool
    .map((f) => ({ ...f, distance_km: Math.round(haversineKm(lat, lon, f.lat, f.lon) * 10) / 10 }))
    .sort((a, b) => a.distance_km - b.distance_km);
  const within = ranked.filter((f) => f.distance_km <= radius).slice(0, limit);

  res.json({
    success: true,
    data: {
      origin: { lat, lon, destination: destName },
      radius_km: radius,
      count: within.length,
      facilities: within,
      nearest_overall: within.length === 0 ? ranked[0] || null : undefined,
      message:
        within.length === 0
          ? `No mapped facility within ${radius} km. The map data may be incomplete: call an emergency number.`
          : undefined,
      emergency_numbers: EMERGENCY_NUMBERS,
      notes: [
        "Distances are straight-line. Road or trail distance is longer, especially in the mountains.",
        "Facility data comes from OpenStreetMap and may be incomplete or out of date.",
      ],
      attribution: "Data © OpenStreetMap contributors (ODbL)",
      dataset_size: FACILITIES.length,
    },
  });
});

module.exports = { nearest };