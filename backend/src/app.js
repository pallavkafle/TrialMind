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
const errorHandler = require("./middleware/errorHandler");

const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// quick sanity check from the browser / emulator
app.get("/api/health", (req, res) =>
  res.json({ success: true, message: "TrailMind API is running" })
);

app.use("/api/auth", authRoutes);
app.use("/api/ml", mlRoutes);
app.use("/api/trips", tripRoutes);
app.use("/api/reviews", reviewRoutes);
app.use("/api/permits", permitRoutes);
app.use("/api/health-facilities", healthRoutes);
app.use("/api/weather", weatherRoutes);
app.use("/api/currency", currencyRoutes);
app.use("/api/hotels", hotelRoutes);

// unmatched routes
app.use((req, res) =>
  res.status(404).json({ success: false, message: `Route ${req.originalUrl} not found` })
);

app.use(errorHandler);

module.exports = app;