const express = require("express");
const protect = require("../middleware/auth");
const { checkPermits } = require("../controllers/permitController");

const router = express.Router();

router.post("/check", protect, checkPermits);

module.exports = router;