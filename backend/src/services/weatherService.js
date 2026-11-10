const { describeCode, isStorm, isSnow } = require("../utils/weatherCodes");

const BASE = "https://api.open-meteo.com/v1/forecast";
const TTL_MS = 30 * 60 * 1000;            // fresh for 30 minutes
const STALE_MAX_MS = 6 * 60 * 60 * 1000;  // if Open-Meteo is down, reuse an answer up to 6 hours old
const MAX_ENTRIES = 300;
const cache = new Map();                  // "lat,lon" rounded to 0.01 degree -> { at, data }

class WeatherError extends Error {
  constructor(message, statusCode = 502) {
    super(message);
    this.statusCode = statusCode; // picked up by errorHandler
  }
}

const num = (v) => (typeof v === "number" && Number.isFinite(v) ? v : null);

const dayLabel = (iso) =>
  new Date(`${iso}T00:00:00Z`).toLocaleDateString("en-GB", {
    weekday: "short", day: "numeric", month: "short", timeZone: "UTC",
  });

// plain rules, no AI: turn the numbers into short warnings
const buildAdvisories = (days) => {
  const pick = (pred) => days.filter(pred).map((d) => dayLabel(d.date)).join(", ");
  const out = [];

  const storm = pick((d) => isStorm(d.code));
  if (storm) out.push(`Thunderstorms forecast on ${storm}. Stay off exposed ridges and passes.`);
  const snow = pick((d) => isSnow(d.code));
  if (snow) out.push(`Snow forecast on ${snow}. Check trail and pass conditions before you go.`);
  const rain = pick((d) => (d.rain_chance_pct ?? 0) >= 60 || (d.rain_mm ?? 0) >= 10);
  if (rain) out.push(`Rain likely on ${rain}. Trails can be slippery and rivers can rise.`);
  const wind = pick((d) => (d.wind_max_kmh ?? 0) >= 40);
  if (wind) out.push(`Strong wind on ${wind}.`);
  const cold = pick((d) => d.temp_min_c !== null && d.temp_min_c <= 0);
  if (cold) out.push(`Freezing temperatures on ${cold}. Pack warm layers.`);

  return out;
};

const fetchForecast = async (lat, lon) => {
  const params = new URLSearchParams({
    latitude: lat.toFixed(4),
    longitude: lon.toFixed(4),
    current: "temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code",
    daily:
      "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum," +
      "precipitation_probability_max,wind_speed_10m_max",
    timezone: "auto",
    forecast_days: "7",
  });

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  let res;
  try {
    res = await fetch(`${BASE}?${params}`, { signal: controller.signal });
  } catch (err) {
    if (err.name === "AbortError") throw new WeatherError("Weather service timed out", 504);
    throw new WeatherError("Weather service is not reachable right now", 503);
  } finally {
    clearTimeout(timer);
  }

  const body = await res.json().catch(() => null);
  if (!res.ok || !body || !body.daily || !Array.isArray(body.daily.time)) {
    throw new WeatherError("Weather service returned an error", 502);
  }

  const c = body.current || {};
  const now = describeCode(c.weather_code);
  const d = body.daily;
  const daily = d.time.map((date, i) => {
    const code = d.weather_code ? d.weather_code[i] : null;
    const info = describeCode(code);
    return {
      date,
      code,
      summary: info.summary,
      emoji: info.emoji,
      temp_max_c: num(d.temperature_2m_max?.[i]),
      temp_min_c: num(d.temperature_2m_min?.[i]),
      rain_mm: num(d.precipitation_sum?.[i]),
      rain_chance_pct: num(d.precipitation_probability_max?.[i]),
      wind_max_kmh: num(d.wind_speed_10m_max?.[i]),
    };
  });

  return {
    timezone: body.timezone || null,
    elevation_m: num(body.elevation),
    current: {
      temperature_c: num(c.temperature_2m),
      humidity_pct: num(c.relative_humidity_2m),
      wind_kmh: num(c.wind_speed_10m),
      summary: now.summary,
      emoji: now.emoji,
    },
    daily,
    advisories: buildAdvisories(daily),
    fetched_at: new Date().toISOString(),
    attribution: "Weather data by Open-Meteo.com (CC BY 4.0)",
  };
};

const getForecast = async (lat, lon) => {
  const key = `${lat.toFixed(2)},${lon.toFixed(2)}`;
  const hit = cache.get(key);
  const now = Date.now();
  if (hit && now - hit.at < TTL_MS) return { ...hit.data, stale: false };

  try {
    const data = await fetchForecast(lat, lon);
    cache.delete(key);
    if (cache.size >= MAX_ENTRIES) cache.delete(cache.keys().next().value);
    cache.set(key, { at: now, data });
    return { ...data, stale: false };
  } catch (err) {
    if (hit && now - hit.at < STALE_MAX_MS) return { ...hit.data, stale: true };
    throw err;
  }
};

module.exports = { getForecast };