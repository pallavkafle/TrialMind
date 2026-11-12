const asyncHandler = require("../utils/asyncHandler");
const { getDestinationNames } = require("../services/destinationCache");
const { nationalityCategory } = require("../utils/nationality");
const {
  AS_OF, ADVISORIES, MONTHS, KEY, monthIndex, computePermits,
} = require("../services/permitService");

const bad = (res, message) => res.status(400).json({ success: false, message });

// POST /api/permits/check
// body: { destinations[], duration_days, group_size, travel_month, nationality_category? }
const checkPermits = asyncHandler(async (req, res) => {
  const { destinations, duration_days, group_size, travel_month } = req.body;

  if (!Array.isArray(destinations) || destinations.length < 1 || destinations.length > 10)
    return bad(res, "destinations must be a list of 1 to 10 names");
  const days = Number(duration_days);
  if (!Number.isInteger(days) || days < 1 || days > 30) return bad(res, "duration_days must be 1 to 30");
  const group = Number(group_size);
  if (!Number.isInteger(group) || group < 1 || group > 20) return bad(res, "group_size must be 1 to 20");
  const monthIdx = monthIndex(travel_month);
  if (monthIdx < 0) return bad(res, "travel_month must be a month name, e.g. October");

  const category = req.body.nationality_category || nationalityCategory(req.user.nationality);
  if (!Object.prototype.hasOwnProperty.call(KEY, category))
    return bad(res, "nationality_category must be Domestic, SAARC or Foreign");

  const names = await getDestinationNames();
  const unique = [...new Set(destinations)];
  const unknown = unique.filter((d) => !names.has(d));
  if (unknown.length) return bad(res, `Unknown destination(s): ${unknown.join(", ")}`);

  const r = computePermits({ destinations: unique, days, group, monthIdx, category });

  res.json({
    success: true,
    data: {
      as_of: AS_OF,
      nationality_category: category,
      group_size: group,
      duration_days: days,
      travel_month: MONTHS[monthIdx],
      permits: r.permits,
      total_per_person_npr: r.perPerson,
      total_npr: r.total,
      destinations: r.perDestination,
      needs_verification: r.needsVerification,
      advisories: ADVISORIES,
    },
  });
});

module.exports = { checkPermits };