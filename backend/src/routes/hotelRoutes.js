const express = require("express");
const protect = require("../middleware/auth");
const { nearby } = require("../controllers/hotelController");

const router = express.Router();

router.get("/nearby", protect, nearby);

module.exports = router;