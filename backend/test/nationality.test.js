const { describe, it } = require("node:test");
const assert = require("node:assert/strict");

const { nationalityCategory } = require("../src/utils/nationality");
const { isValidCountry } = require("../src/utils/country");

const SAARC = ["AF", "BD", "BT", "IN", "MV", "PK", "LK"];

describe("nationality category", () => {
  it("Nepal is Domestic", () => {
    for (const v of ["NP", "np", " Np "]) assert.equal(nationalityCategory(v), "Domestic");
  });

  it("the seven other SAARC countries are SAARC", () => {
    for (const c of SAARC) {
      assert.equal(nationalityCategory(c), "SAARC", c);
      assert.equal(nationalityCategory(c.toLowerCase()), "SAARC", c);
    }
  });

  it("everyone else is Foreign", () => {
    for (const c of ["US", "DE", "CN", "GB", "JP", "AU", "FR", "CA", "KR"]) {
      assert.equal(nationalityCategory(c), "Foreign", c);
    }
  });

  it("a missing or odd value is Foreign", () => {
    for (const v of [undefined, null, "", "   ", 42, "Nepal"]) assert.equal(nationalityCategory(v), "Foreign");
  });
});

describe("country code check", () => {
  it("accepts real country codes in any case, with spaces", () => {
    for (const c of ["NP", "np", " in ", "US", "DE", "JP", "GB", "FR", "AU", ...SAARC]) {
      assert.equal(isValidCountry(c), true, c);
    }
  });

  it("rejects everything else", () => {
    for (const c of ["", "N", "NPL", "ZZ", "12", "N1", "N P", null, undefined, 5, {}]) {
      assert.equal(isValidCountry(c), false, String(c));
    }
  });
});