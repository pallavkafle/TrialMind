// WMO weather codes (used by Open-Meteo) -> short text + emoji.
// Emoji are written as escapes so the file stays plain ASCII.
const E = {
  sun: "\u2600\uFE0F",
  mostly: "\uD83C\uDF24\uFE0F",
  partly: "\u26C5",
  cloud: "\u2601\uFE0F",
  fog: "\uD83C\uDF2B\uFE0F",
  drizzle: "\uD83C\uDF26\uFE0F",
  rain: "\uD83C\uDF27\uFE0F",
  snow: "\uD83C\uDF28\uFE0F",
  storm: "\u26C8\uFE0F",
  unknown: "\u2753",
};

const TABLE = {
  0: ["Clear sky", E.sun],
  1: ["Mostly clear", E.mostly],
  2: ["Partly cloudy", E.partly],
  3: ["Overcast", E.cloud],
  45: ["Fog", E.fog],
  48: ["Freezing fog", E.fog],
  51: ["Light drizzle", E.drizzle],
  53: ["Drizzle", E.drizzle],
  55: ["Heavy drizzle", E.drizzle],
  56: ["Light freezing drizzle", E.drizzle],
  57: ["Freezing drizzle", E.drizzle],
  61: ["Light rain", E.rain],
  63: ["Rain", E.rain],
  65: ["Heavy rain", E.rain],
  66: ["Light freezing rain", E.rain],
  67: ["Freezing rain", E.rain],
  71: ["Light snow", E.snow],
  73: ["Snow", E.snow],
  75: ["Heavy snow", E.snow],
  77: ["Snow grains", E.snow],
  80: ["Light showers", E.drizzle],
  81: ["Showers", E.rain],
  82: ["Heavy showers", E.rain],
  85: ["Light snow showers", E.snow],
  86: ["Heavy snow showers", E.snow],
  95: ["Thunderstorm", E.storm],
  96: ["Thunderstorm with hail", E.storm],
  99: ["Severe thunderstorm with hail", E.storm],
};

const SNOW_CODES = new Set([71, 73, 75, 77, 85, 86]);

const describeCode = (code) => {
  const t = TABLE[code];
  return t ? { summary: t[0], emoji: t[1] } : { summary: "Unknown", emoji: E.unknown };
};

const isStorm = (code) => code >= 95;
const isSnow = (code) => SNOW_CODES.has(code);

module.exports = { describeCode, isStorm, isSnow };