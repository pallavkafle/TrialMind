const express = require("express");
const protect = require("../middleware/auth");
const ml = require("../controllers/mlController");

const router = express.Router();

router.get("/health", ml.health);
router.get("/destinations", ml.destinations);

router.post("/budget", protect, ml.budget);
router.post("/route", protect, ml.route);
router.post("/itinerary", protect, ml.itinerary);
router.post("/analyze-review", protect, ml.analyzeReview);

module.exports = router;