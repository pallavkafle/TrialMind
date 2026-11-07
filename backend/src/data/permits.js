// Fee data researched 2026-11-07 from agency and official pages. Fees change:
// verify before publishing. confidence: "high" = official or several agreeing
// sources, "medium" = fewer sources or a missing category, "low" = sources disagree.
// A null fee means "not found". NPR unless the permit has a `restricted` block (USD).
const AS_OF = "2026-11-07";
const USD_TO_NPR = 152; // same rate as the ML budget service

const PERMITS = {
  sagarmatha_np: {
    name: "Sagarmatha National Park entry",
    authority: "DNPWC (Monjo gate or Kathmandu counter)",
    fee: { nepali: 100, saarc: 1500, foreign: 3000 },
    confidence: "high",
    note: "Some sources add 13% VAT for foreigners (about NPR 3,390).",
  },
  khumbu_rm: {
    name: "Khumbu Pasang Lhamu Rural Municipality fee",
    authority: "Local municipality (Lukla / Monjo)",
    fee: { nepali: null, saarc: null, foreign: 3000 },
    confidence: "medium",
    note: "Replaces TIMS in the Khumbu. Rates for SAARC and Nepali travellers not found.",
  },
  acap: {
    name: "Annapurna Conservation Area Permit (ACAP)",
    authority: "NTNC / Nepal Tourism Board",
    fee: { nepali: 100, saarc: 1000, foreign: 3000 },
    confidence: "high",
    note: "One source lists NPR 300 for Nepali citizens.",
  },
  tims: {
    name: "TIMS card",
    authority: "Nepal Tourism Board / registered agency",
    fee: { nepali: null, saarc: 1000, foreign: 2000 },
    confidence: "low",
    note: "Some sources list NPR 1,000 for agency-booked trekkers, and the system is moving online.",
  },
  langtang_np: {
    name: "Langtang National Park entry",
    authority: "DNPWC",
    fee: { nepali: null, saarc: 1500, foreign: 3000 },
    confidence: "medium",
    note: "13% VAT may apply for foreigners.",
  },
  mcap: {
    name: "Manaslu Conservation Area Permit (MCAP)",
    authority: "NTNC / Nepal Tourism Board",
    fee: { nepali: 100, saarc: 1000, foreign: 3000 },
    confidence: "medium",
    note: "",
  },
  manaslu_rap: {
    name: "Manaslu restricted area permit",
    authority: "Department of Immigration via a registered agency",
    restricted: {
      baseDays: 7, peakMonths: [9, 10, 11],
      base: { peak: 100, off: 75 }, extraPerDay: { peak: 15, off: 10 },
    },
    confidence: "medium",
    note: "USD, first 7 days then per extra day (extra-day rate not confirmed). Usually needs a licensed guide and at least two trekkers.",
  },
  mustang_rap: {
    name: "Upper Mustang restricted area permit",
    authority: "Department of Immigration via a registered agency",
    restricted: {
      baseDays: 10, peakMonths: [],
      base: { peak: 500, off: 500 }, extraPerDay: { peak: 50, off: 50 },
    },
    confidence: "medium",
    note: "USD, first 10 days then per extra day (extra-day rate not confirmed). Usually needs a licensed guide and at least two trekkers.",
  },
  chitwan_np: {
    name: "Chitwan National Park entry",
    authority: "Park entrance gate",
    fee: { nepali: 150, saarc: 1000, foreign: 2000 },
    confidence: "high",
    note: "Charged per entry: each day you enter the park may be charged again.",
  },
  bardia_np: {
    name: "Bardia National Park entry",
    authority: "Thakurdwara gate",
    fee: { nepali: 100, saarc: 1000, foreign: 2000 },
    confidence: "low",
    note: "Sources disagree (foreigners NPR 1,500-2,000, one lists USD 25 per day). Confirm at the gate.",
  },
  shivapuri_np: {
    name: "Shivapuri Nagarjun National Park entry",
    authority: "Park entrance gate",
    fee: { nepali: 100, saarc: 600, foreign: 1000 },
    confidence: "medium",
    note: "",
  },
  lumbini_site: {
    name: "Maya Devi Temple entry (Lumbini)",
    authority: "Lumbini Development Trust",
    fee: { nepali: null, saarc: 100, foreign: 200 },
    confidence: "medium",
    note: "Indian nationals are reported to enter free.",
  },
  bhaktapur_site: {
    name: "Bhaktapur Durbar Square entry",
    authority: "Bhaktapur Municipality",
    fee: { nepali: 0, saarc: 500, foreign: 1500 },
    confidence: "low",
    note: "Another source may list a higher fee for foreigners. Confirm at the gate.",
  },
};

const SHERPA = ["sagarmatha_np", "khumbu_rm"];

// permits a destination needs (cumulative across a multi-stop trip, deduplicated)
const DESTINATION_PERMITS = {
  "Everest Base Camp": SHERPA,
  "Gokyo Lakes": SHERPA,
  "Sagarmatha National Park": SHERPA,
  "Annapurna Circuit": ["acap", "tims"],
  "Mardi Himal": ["acap", "tims"],
  "Poon Hill (Ghorepani)": ["acap", "tims"],
  "Dhampus / Australian Camp": ["acap", "tims"],
  "Muktinath": ["acap", "tims"],
  "Upper Mustang": ["acap", "mustang_rap"],
  "Langtang Valley": ["langtang_np", "tims"],
  "Manaslu Circuit": ["mcap", "acap", "manaslu_rap"],
  "Chitwan National Park": ["chitwan_np"],
  "Bardia National Park": ["bardia_np"],
  "Shivapuri National Park": ["shivapuri_np"],
  "Lumbini": ["lumbini_site"],
  "Bhaktapur Durbar Square": ["bhaktapur_site"],
};

const DESTINATION_NOTES = {
  Pashupatinath: ["Entry fees for Kathmandu Valley heritage sites were not verified; check at the gate.", "Non-Hindus cannot enter the main temple."],
  Boudhanath: ["Entry fee not verified; check at the gate."],
  Swayambhunath: ["Entry fee not verified; check at the gate."],
  "Janakpur (Janaki Mandir)": ["No fee data found."],
  "Muktinath": ["Reached on the Annapurna Circuit; the ACAP and TIMS rules apply."],
};

const ADVISORIES = [
  "Trekking rules (licensed-guide and solo-trekking requirements, TIMS) have changed several times since 2023. Confirm with the Nepal Tourism Board or a registered agency before you travel.",
  "Fees are per person. Children under 10 are generally free in parks (not applied here).",
  `USD-priced permits use 1 USD = ${USD_TO_NPR} NPR, the same rate as the budget estimator.`,
];

module.exports = {
  AS_OF, USD_TO_NPR, PERMITS, DESTINATION_PERMITS, DESTINATION_NOTES, ADVISORIES,
};