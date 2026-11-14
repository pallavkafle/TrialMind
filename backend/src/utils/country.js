// valid ISO 3166-1 alpha-2 country codes, checked with the Intl data built into Node
const names = new Intl.DisplayNames(["en"], { type: "region" });

const isValidCountry = (code) => {
  const c = String(code || "").trim().toUpperCase();
  if (!/^[A-Z]{2}$/.test(c) || c === "ZZ") return false;
  try {
    return names.of(c) !== c;
  } catch (err) {
    return false;
  }
};

module.exports = { isValidCountry };