const express = require("express");
const { nearest } = require("../controllers/healthController");

const router = express.Router();

router.get("/nearest", nearest); // public: must work without login in an emergency

module.exports = router;