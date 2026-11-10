const express = require("express");
const protect = require("../middleware/auth");
const { forecast } = require("../controllers/weatherController");

const router = express.Router();

router.get("/", protect, forecast);

module.exports = router;