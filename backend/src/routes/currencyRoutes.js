const express = require("express");
const protect = require("../middleware/auth");
const { rates } = require("../controllers/currencyController");

const router = express.Router();

router.get("/rates", protect, rates);

module.exports = router;