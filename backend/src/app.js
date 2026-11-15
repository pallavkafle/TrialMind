const express = require("express");
const authRoutes = require("./routes/authRoutes");
const mlRoutes = require("./routes/mlRoutes");
const tripRoutes = require("./routes/tripRoutes");
const reviewRoutes = require("./routes/reviewRoutes");
const permitRoutes = require("./routes/permitRoutes");
const healthRoutes = require("./routes/healthRoutes");
const weatherRoutes = require("./routes/weatherRoutes");
const currencyRoutes = require("./routes/currencyRoutes");
const hotelRoutes = require("./routes/hotelRoutes");
const favoriteRoutes = require("./routes/favoriteRoutes");
const placeRoutes = require("./routes/placeRoutes");
const adminRoutes = require("./routes/adminRoutes");
const errorHandler = require("./middleware/errorHandler");
const {
  helmetMw, corsMw, httpsOnly, apiLimiter, authLimiter, mlLimiter, itineraryLimiter, reviewLimiter,
} = require("./middleware/security");

const app = express();

app.disable("x-powered-by");
app.use(httpsOnly);
app.use(helmetMw);
app.use(corsMw);
app.use("/api", apiLimiter);
app.use(express.json({ limit: "200kb" }));
app.use(express.urlencoded({ extended: true }));

// quick sanity check from the browser / emulator
app.get("/api/health", (req, res) =>
  res.json({ success: true, message: "TrailMind API is running" })
);

app.use("/api/auth/login", authLimiter);
app.use("/api/auth/register", authLimiter);
app.use("/api/auth", authRoutes);
app.use("/api/ml", mlLimiter);
app.use("/api/ml/itinerary", itineraryLimiter);
app.use("/api/ml", mlRoutes);
app.use("/api/trips", tripRoutes);
app.post("/api/reviews", reviewLimiter);
app.use("/api/reviews", reviewRoutes);
app.use("/api/permits", permitRoutes);
app.use("/api/health-facilities", healthRoutes);
app.use("/api/weather", weatherRoutes);
app.use("/api/currency", currencyRoutes);
app.use("/api/hotels", hotelRoutes);
app.use("/api/favorites", favoriteRoutes);
app.use("/api/places", placeRoutes);
app.use("/api/admin", adminRoutes);

// unmatched routes
app.use((req, res) =>
  res.status(404).json({ success: false, message: `Route ${req.originalUrl} not found` })
);

app.use(errorHandler);

module.exports = app;