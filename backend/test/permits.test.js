const { describe, it, afterEach } = require("node:test");
const assert = require("node:assert/strict");

const {
  PERMITS, DESTINATION_PERMITS, DESTINATION_NOTES, ADVISORIES, AS_OF, USD_TO_NPR,
} = require("../src/data/permits");
const destinations = require("../src/data/destinations.json");
const {
  computePermits, monthIndex, hasPermitData, setPermitOverrides, effectivePermit, MONTHS, KEY,
} = require("../src/services/permitService");

const NAMES = destinations.map((d) => d.name);
const OCT = monthIndex("October");
const EBC = "Everest Base Camp";

const calc = (dests, over = {}) =>
  computePermits({ destinations: dests, days: 12, group: 4, monthIdx: OCT, category: "Foreign", ...over });
const permitOf = (result, id) => result.permits.find((p) => p.id === id);
const destinationFor = (id) =>
  Object.keys(DESTINATION_PERMITS).find((d) => DESTINATION_PERMITS[d].includes(id));
const restrictedId = (baseDays) => {
  const hit = Object.entries(PERMITS).find(([, p]) => p.restricted && p.restricted.baseDays === baseDays);
  return hit ? hit[0] : undefined;
};
const restrictedFee = (id, days, month, category = "Foreign", group = 1) => {
  const dest = destinationFor(id);
  assert.ok(dest, `no destination uses the permit ${id}`);
  const r = computePermits({ destinations: [dest], days, group, monthIdx: monthIndex(month), category });
  return permitOf(r, id);
};

describe("permit data tables", () => {
  it("states its date and uses the same exchange rate as the budget service", () => {
    assert.match(AS_OF, /^\d{4}-\d{2}-\d{2}$/);
    assert.equal(USD_TO_NPR, 152);
    assert.ok(Array.isArray(ADVISORIES) && ADVISORIES.length > 0);
  });

  it("every permit is well formed", () => {
    for (const [id, p] of Object.entries(PERMITS)) {
      assert.ok(p.name && p.authority, `${id}: name and authority`);
      assert.ok(["high", "medium", "low"].includes(p.confidence), `${id}: confidence`);
      assert.equal(typeof p.note, "string", `${id}: a note is needed (otherwise the app would print "undefined")`);
      if (p.restricted) {
        const r = p.restricted;
        assert.ok(Number.isInteger(r.baseDays) && r.baseDays > 0, `${id}: baseDays`);
        assert.ok(
          Array.isArray(r.peakMonths) && r.peakMonths.every((m) => Number.isInteger(m) && m >= 1 && m <= 12),
          `${id}: peakMonths`
        );
        for (const k of ["peak", "off"]) {
          assert.ok(r.base[k] > 0, `${id}: base.${k}`);
          assert.ok(r.extraPerDay[k] > 0, `${id}: extraPerDay.${k}`);
        }
      } else {
        for (const k of ["nepali", "saarc", "foreign"]) {
          const v = p.fee[k];
          assert.ok(v === null || (typeof v === "number" && v >= 0), `${id}: fee.${k}`);
        }
      }
    }
  });

  it("destinations only list real permits", () => {
    for (const [dest, ids] of Object.entries(DESTINATION_PERMITS)) {
      assert.ok(Array.isArray(ids), dest);
      for (const id of ids) assert.ok(PERMITS[id], `${dest} lists the unknown permit ${id}`);
    }
  });

  it("permit and note tables only use real destinations", () => {
    for (const dest of Object.keys(DESTINATION_PERMITS)) assert.ok(NAMES.includes(dest), `unknown destination: ${dest}`);
    for (const dest of Object.keys(DESTINATION_NOTES)) assert.ok(NAMES.includes(dest), `unknown destination: ${dest}`);
  });
});

describe("reference fees", () => {
  it("Everest Base Camp for 4 foreign travellers is NPR 24,000", () => {
    const r = calc([EBC]);
    assert.equal(r.perPerson, 6000);
    assert.equal(r.total, 24000);
    const ids = r.permits.map((p) => p.id);
    assert.ok(ids.includes("sagarmatha_np") && ids.includes("khumbu_rm"));
    for (const p of r.permits) assert.equal(p.fee_total_npr, p.fee_per_person_npr * 4);
  });

  it("for Nepali travellers one Everest fee is missing and is flagged for checking", () => {
    const r = calc([EBC], { category: "Domestic" });
    const expected = DESTINATION_PERMITS[EBC].reduce((s, id) => s + (PERMITS[id].fee.nepali || 0), 0);
    assert.equal(r.perPerson, expected);
    assert.equal(permitOf(r, "khumbu_rm").fee_per_person_npr, null);
    assert.equal(permitOf(r, "khumbu_rm").fee_total_npr, null);
    assert.ok(r.needsVerification.includes(PERMITS.khumbu_rm.name));
  });

  it("low-confidence permits are flagged for checking", () => {
    const dest = destinationFor("tims");
    assert.ok(dest, "no destination uses the TIMS card");
    assert.ok(calc([dest]).needsVerification.includes(PERMITS.tims.name));
  });
});

describe("shared permits are counted once", () => {
  const plain = (ids) => ids.every((id) => !PERMITS[id].restricted);
  const findPair = () => {
    const names = Object.keys(DESTINATION_PERMITS);
    for (let i = 0; i < names.length; i++) {
      for (let j = i + 1; j < names.length; j++) {
        const a = DESTINATION_PERMITS[names[i]];
        const b = DESTINATION_PERMITS[names[j]];
        const shared = a.filter((id) => b.includes(id));
        if (shared.length && plain([...a, ...b]) && shared.some((id) => (PERMITS[id].fee.foreign || 0) > 0)) {
          return [names[i], names[j], a, b];
        }
      }
    }
    return null;
  };

  it("two destinations that share a permit pay for it once", () => {
    const pair = findPair();
    assert.ok(pair, "no two destinations share a permit with a fee");
    const [d1, d2, a, b] = pair;
    const union = [...new Set([...a, ...b])];
    const both = calc([d1, d2], { group: 1 });
    assert.deepEqual(both.permits.map((p) => p.id).sort(), union.slice().sort());
    assert.equal(both.perPerson, union.reduce((s, id) => s + (PERMITS[id].fee.foreign || 0), 0));
    assert.ok(both.perPerson < calc([d1], { group: 1 }).perPerson + calc([d2], { group: 1 }).perPerson);
  });

  it("naming a destination twice changes nothing", () => {
    assert.equal(calc([EBC, EBC]).total, calc([EBC]).total);
    assert.equal(calc([EBC, EBC]).permits.length, calc([EBC]).permits.length);
  });
});

describe("restricted-area permits", () => {
  const SEVEN = restrictedId(7);
  const TEN = restrictedId(10);
  const NPR = (usd) => usd * 152;

  it("both restricted permits exist and are used by a destination", () => {
    assert.ok(SEVEN && destinationFor(SEVEN), "restricted permit with a 7-day base");
    assert.ok(TEN && destinationFor(TEN), "restricted permit with a 10-day base");
  });

  it("the 7-day permit is cheaper off season and dearer in September to November", () => {
    assert.equal(restrictedFee(SEVEN, 7, "October").fee_per_person_npr, NPR(100));
    assert.equal(restrictedFee(SEVEN, 7, "September").fee_per_person_npr, NPR(100));
    assert.equal(restrictedFee(SEVEN, 7, "March").fee_per_person_npr, NPR(75));
    assert.equal(restrictedFee(SEVEN, 7, "August").fee_per_person_npr, NPR(75));
    assert.equal(restrictedFee(SEVEN, 7, "December").fee_per_person_npr, NPR(75));
  });

  it("extra days cost extra, and a shorter trip is not refunded", () => {
    assert.equal(restrictedFee(SEVEN, 14, "October").fee_per_person_npr, 31160);
    assert.equal(restrictedFee(SEVEN, 10, "March").fee_per_person_npr, NPR(75 + 3 * 10));
    assert.equal(restrictedFee(SEVEN, 10, "November").fee_per_person_npr, NPR(100 + 3 * 15));
    assert.equal(restrictedFee(SEVEN, 14, "December").fee_per_person_npr, NPR(75 + 7 * 10));
    assert.equal(restrictedFee(SEVEN, 3, "October").fee_per_person_npr, NPR(100));
  });

  it("the 10-day permit does not depend on the month", () => {
    for (const m of MONTHS) assert.equal(restrictedFee(TEN, 10, m).fee_per_person_npr, NPR(500), m);
    assert.equal(restrictedFee(TEN, 12, "October").fee_per_person_npr, NPR(500 + 2 * 50));
    assert.equal(restrictedFee(TEN, 30, "May").fee_per_person_npr, NPR(500 + 20 * 50));
  });

  it("the fee is per person and the total follows the group size", () => {
    const p = restrictedFee(SEVEN, 14, "October", "Foreign", 4);
    assert.equal(p.fee_per_person_npr, 31160);
    assert.equal(p.fee_total_npr, 4 * 31160);
  });

  it("non-foreign travellers get no fee, a note and a flag for checking", () => {
    for (const category of ["Domestic", "SAARC"]) {
      const p = restrictedFee(SEVEN, 7, "October", category);
      assert.equal(p.fee_per_person_npr, null);
      assert.equal(p.fee_total_npr, null);
      assert.match(p.note, /Fee for non-foreign travellers not found/);
      assert.ok(!p.note.includes("undefined"));
      const r = computePermits({ destinations: [destinationFor(SEVEN)], days: 7, group: 2, monthIdx: OCT, category });
      assert.ok(r.needsVerification.includes(p.name));
    }
  });
});

describe("categories, groups and months", () => {
  it("the category names map to the fee columns", () => {
    assert.deepEqual(KEY, { Domestic: "nepali", SAARC: "saarc", Foreign: "foreign" });
  });

  it("when all three fees are known, foreign is at least SAARC, which is at least Nepali", () => {
    for (const [id, p] of Object.entries(PERMITS)) {
      if (p.restricted) continue;
      const { nepali, saarc, foreign } = p.fee;
      if (nepali === null || saarc === null || foreign === null) continue;
      assert.ok(foreign >= saarc && saarc >= nepali, id);
    }
  });

  it("an unknown category gets no fee", () => {
    const r = calc([EBC], { category: "Alien" });
    assert.equal(r.perPerson, 0);
    for (const p of r.permits) assert.equal(p.fee_per_person_npr, null);
  });

  it("the number of days never changes a normal permit", () => {
    assert.equal(calc([EBC], { days: 3 }).total, calc([EBC], { days: 30 }).total);
  });

  it("totals follow the group size", () => {
    assert.equal(calc([EBC], { group: 1 }).total, 6000);
    assert.equal(calc([EBC], { group: 10 }).total, 60000);
  });

  it("a destination without permit data says so", () => {
    assert.equal(hasPermitData("Atlantis"), false);
    const r = calc(["Atlantis"]);
    assert.deepEqual(r.permits, []);
    assert.equal(r.total, 0);
    assert.equal(r.perDestination[0].notes[0], "No permits found in the data for this destination.");
    const known = NAMES.filter((n) => hasPermitData(n));
    assert.ok(known.length > 0 && known.includes(EBC));
  });

  it("month names are read in any case, with spaces", () => {
    assert.equal(MONTHS.length, 12);
    MONTHS.forEach((m, i) => assert.equal(monthIndex(m), i));
    assert.equal(monthIndex("october"), 9);
    assert.equal(monthIndex("  OCTOBER "), 9);
    for (const bad of ["Octember", "", undefined, null, 10]) assert.equal(monthIndex(bad), -1);
  });
});

describe("admin overrides", () => {
  afterEach(() => setPermitOverrides([]));

  it("replace only the fields that are given and never change the defaults", () => {
    setPermitOverrides([{ permitId: "acap", fee: { foreign: 5000 } }]);
    const eff = effectivePermit("acap");
    assert.equal(eff.fee.foreign, 5000);
    assert.equal(eff.fee.saarc, PERMITS.acap.fee.saarc);
    assert.equal(PERMITS.acap.fee.foreign, 3000);
    const dest = destinationFor("acap");
    assert.ok(dest, "no destination uses ACAP");
    assert.equal(permitOf(calc([dest]), "acap").fee_per_person_npr, 5000);
    assert.equal(permitOf(calc([dest], { category: "SAARC" }), "acap").fee_per_person_npr, PERMITS.acap.fee.saarc);
  });

  it("can mark a fee as not found", () => {
    setPermitOverrides([{ permitId: "acap", fee: { foreign: null } }]);
    const r = calc([destinationFor("acap")]);
    assert.equal(permitOf(r, "acap").fee_per_person_npr, null);
    assert.ok(r.needsVerification.includes(PERMITS.acap.name));
  });

  it("can change the confidence and the note", () => {
    setPermitOverrides([{ permitId: "acap", confidence: "low", note: "Checked by the admin." }]);
    const eff = effectivePermit("acap");
    assert.equal(eff.confidence, "low");
    assert.equal(eff.note, "Checked by the admin.");
    assert.ok(calc([destinationFor("acap")]).needsVerification.includes(PERMITS.acap.name));
  });

  it("ignore permits that do not exist", () => {
    setPermitOverrides([{ permitId: "ghost", fee: { foreign: 1 } }]);
    assert.equal(effectivePermit("ghost"), undefined);
    assert.equal(calc([EBC]).total, 24000);
  });

  it("cannot change the fee of a restricted-area permit", () => {
    const id = restrictedId(7);
    const before = restrictedFee(id, 7, "October").fee_per_person_npr;
    setPermitOverrides([{ permitId: id, fee: { foreign: 1 } }]);
    assert.equal(restrictedFee(id, 7, "October").fee_per_person_npr, before);
  });

  it("an empty list brings the defaults back", () => {
    setPermitOverrides([{ permitId: "acap", fee: { foreign: 5000 } }]);
    setPermitOverrides([]);
    assert.equal(effectivePermit("acap"), PERMITS.acap);
    assert.equal(calc([EBC]).total, 24000);
  });
});