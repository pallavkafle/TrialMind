// profile nationality (country code) -> the category the ML service expects
const SAARC = new Set(["AF", "BD", "BT", "IN", "MV", "PK", "LK"]);

const nationalityCategory = (code) => {
  const c = String(code || "").trim().toUpperCase();
  if (c === "NP") return "Domestic";
  if (SAARC.has(c)) return "SAARC";
  return "Foreign";
};

module.exports = { nationalityCategory };