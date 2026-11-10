const { USD_TO_NPR } = require("../data/permits");

const URL = "https://open.er-api.com/v6/latest/USD";
const TTL_MS = 6 * 60 * 60 * 1000;           // the service updates once a day
const STALE_MAX_MS = 7 * 24 * 60 * 60 * 1000; // if it is down, reuse saved rates up to 7 days old
let cache = null;                             // { at, data }

// quote_unit follows the Nepali convention: INR per 100, JPY per 10
const CURRENCIES = [
  { code: "USD", name: "US Dollar", unit: 1 },
  { code: "EUR", name: "Euro", unit: 1 },
  { code: "GBP", name: "British Pound", unit: 1 },
  { code: "INR", name: "Indian Rupee", unit: 100 },
  { code: "CNY", name: "Chinese Yuan", unit: 1 },
  { code: "JPY", name: "Japanese Yen", unit: 10 },
  { code: "AUD", name: "Australian Dollar", unit: 1 },
  { code: "CAD", name: "Canadian Dollar", unit: 1 },
  { code: "CHF", name: "Swiss Franc", unit: 1 },
];

class CurrencyError extends Error {
  constructor(message, statusCode = 502) {
    super(message);
    this.statusCode = statusCode; // picked up by errorHandler
  }
}

const fetchRates = async () => {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 10000);
  let res;
  try {
    res = await fetch(URL, { signal: controller.signal });
  } catch (err) {
    if (err.name === "AbortError") throw new CurrencyError("Exchange rate service timed out", 504);
    throw new CurrencyError("Exchange rate service is not reachable right now", 503);
  } finally {
    clearTimeout(timer);
  }

  const body = await res.json().catch(() => null);
  if (!res.ok || !body || body.result !== "success" || !body.rates || !(body.rates.NPR > 0)) {
    throw new CurrencyError("Exchange rate service returned an error", 502);
  }

  const nprPerUsd = body.rates.NPR;
  const rates = CURRENCIES.filter((c) => body.rates[c.code] > 0).map((c) => {
    const nprPerUnit = nprPerUsd / body.rates[c.code]; // value of 1 unit of the currency in NPR
    return {
      code: c.code,
      name: c.name,
      quote_unit: c.unit,
      npr_per_unit: Math.round(nprPerUnit * 10000) / 10000,
      npr_for_quote_unit: Math.round(nprPerUnit * c.unit * 100) / 100,
    };
  });

  return {
    base: "NPR",
    updated_at: body.time_last_update_utc || null,
    next_update_at: body.time_next_update_utc || null,
    rates,
    budget_rate_usd_npr: USD_TO_NPR, // the fixed rate TrailMind budgets use
    source: "ExchangeRate-API (open access)",
    attribution_url: "https://www.exchangerate-api.com",
    note: "Mid-market reference rates, updated once a day. Banks and money changers add a margin, so expect a little less than these figures.",
    fetched_at: new Date().toISOString(),
  };
};

const getRates = async () => {
  const now = Date.now();
  if (cache && now - cache.at < TTL_MS) return { ...cache.data, stale: false };
  try {
    const data = await fetchRates();
    cache = { at: now, data };
    return { ...data, stale: false };
  } catch (err) {
    if (cache && now - cache.at < STALE_MAX_MS) return { ...cache.data, stale: true };
    throw err;
  }
};

module.exports = { getRates };