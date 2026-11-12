const {
  AS_OF, USD_TO_NPR, PERMITS, DESTINATION_PERMITS, DESTINATION_NOTES, ADVISORIES,
} = require("../data/permits");

const MONTHS = ["january", "february", "march", "april", "may", "june",
  "july", "august", "september", "october", "november", "december"];
const KEY = { Domestic: "nepali", SAARC: "saarc", Foreign: "foreign" };

const monthIndex = (name) => MONTHS.indexOf(String(name || "").trim().toLowerCase());
const hasPermitData = (destination) => (DESTINATION_PERMITS[destination] || []).length > 0;

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

// permits for a set of destinations; a permit shared by several destinations is counted once
const computePermits = ({ destinations, days, group, monthIdx, category }) => {
  const ids = new Set();
  const perDestination = destinations.map((d) => {
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

  return {
    permits,
    perDestination,
    perPerson,
    total: perPerson * group,
    needsVerification: permits
      .filter((p) => p.confidence === "low" || p.fee_per_person_npr === null)
      .map((p) => p.name),
  };
};

module.exports = { AS_OF, ADVISORIES, MONTHS, KEY, monthIndex, hasPermitData, computePermits };