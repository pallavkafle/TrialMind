const helmet = require("helmet");
const cors = require("cors");
const rateLimit = require("express-rate-limit");

const limiter = (windowMs, limit, message, extra = {}) =>
  rateLimit({
    windowMs,
    limit,
    standardHeaders: true,
    legacyHeaders: false,
    message: { success: false, message },
    skip: () => process.env.RATE_LIMIT_OFF === "1", // for load tests only
    ...extra,
  });

const helmetMw = helmet();

// the app is not a browser client, so cross-origin access is off unless CORS_ORIGIN lists origins
const corsMw = cors({
  origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(",").map((s) => s.trim()) : false,
});

// set FORCE_HTTPS=1 behind a TLS-terminating proxy to reject plain-HTTP requests
const httpsOnly = (req, res, next) => {
  if (process.env.FORCE_HTTPS !== "1") return next();
  if (req.secure || req.headers["x-forwarded-proto"] === "https") return next();
  return res.status(426).json({ success: false, message: "HTTPS is required" });
};

const apiLimiter = limiter(15 * 60 * 1000, 1000, "Too many requests, please slow down");
// only failed attempts count, so normal logins are never blocked
const authLimiter = limiter(15 * 60 * 1000, 20, "Too many attempts, try again in 15 minutes", {
  skipSuccessfulRequests: true,
});
const mlLimiter = limiter(60 * 1000, 60, "Too many requests, please wait a moment");
const itineraryLimiter = limiter(60 * 1000, 10, "Itinerary requests are limited to 10 per minute");
const reviewLimiter = limiter(60 * 60 * 1000, 20, "Review limit reached, try again later");

module.exports = {
  helmetMw, corsMw, httpsOnly, apiLimiter, authLimiter, mlLimiter, itineraryLimiter, reviewLimiter,
};