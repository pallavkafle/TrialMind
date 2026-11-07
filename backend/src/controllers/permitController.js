const asyncHandler = require("../utils/asyncHandler");
const { getDestinationNames } = require("../services/destinationCache");
const { nationalityCategory } = require("../utils/nationality");
const {
  AS_OF, USD_TO_NPR, PERMITS, DESTINATION_PERMITS, DESTINATION_NOTES, ADVISORIES,
} = require("../data/permits");

const MONTHS = ["january", "february", "march", "april", "may", "june",
  "july", "august", "september", "october", "november", "december"];
const KEY = { Domestic: "nepali", SAARC: "saarc", Foreign: "foreign" };

const bad = (res, message) => res.status(400).json({ success: false, message });

const describe = (id, category, days, monthIdx) => {
  const p = PERMITS[id];
  let fee = null;
  let note = p.note;
  if (p.restricted) {
    if (category === "Foreign") {
      const r = p.restricted;
      const k = r.peakMonths.includes(monthIdx + 1) ? "peak" : "off";
      const extra = Math.max(0, days - r.baseDays);
      fee = Math.round((r.base[k] + extra * r.extraPerDay[k]) * USD_TO_NPR);
    } else {
      note = `${note} Fee for non-foreign travellers not found; check with the Department of Immigration.`.trim();
    }
  } else {
    fee = p.fee[KEY[category]];
    if (fee === undefined) fee = null;
  }
  return { id, name: p.name, authority: p.authority, confidence: p.confidence, note, fee_per_person_npr: fee };
};

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
  const monthIdx = MONTHS.indexOf(String(travel_month || "").trim().toLowerCase());
  if (monthIdx < 0) return bad(res, "travel_month must be a month name, e.g. October");

  const category = req.body.nationality_category || nationalityCategory(req.user.nationality);
  if (!Object.prototype.hasOwnProperty.call(KEY, category))
    return bad(res, "nationality_category must be Domestic, SAARC or Foreign");

  const names = await getDestinationNames();
  const unique = [...new Set(destinations)];
  const unknown = unique.filter((d) => !names.has(d));
  if (unknown.length) return bad(res, `Unknown destination(s): ${unknown.join(", ")}`);

  const ids = new Set();
  const perDestination = unique.map((d) => {
    const permitIds = DESTINATION_PERMITS[d] || [];
    permitIds.forEach((id) => ids.add(id));
    const notes = DESTINATION_NOTES[d] || [];
    return {
      destination: d,
      permit_ids: permitIds,
      notes: permitIds.length || notes.length ? notes : ["No permits found in the data for this destination."],
    };
  });

  const permits = [...ids].map((id) => {
    const p = describe(id, category, days, monthIdx);
    return { ...p, fee_total_npr: p.fee_per_person_npr === null ? null : p.fee_per_person_npr * group };
  });

  const perPerson = permits.reduce((s, p) => s + (p.fee_per_person_npr || 0), 0);

  res.json({
    success: true,
    data: {
      as_of: AS_OF,
      nationality_category: category,
      group_size: group,
      duration_days: days,
      travel_month: MONTHS[monthIdx],
      permits,
      total_per_person_npr: perPerson,
      total_npr: perPerson * group,
      destinations: perDestination,
      needs_verification: permits
        .filter((p) => p.confidence === "low" || p.fee_per_person_npr === null)
        .map((p) => p.name),
      advisories: ADVISORIES,
    },
  });
});

module.exports = { checkPermits };