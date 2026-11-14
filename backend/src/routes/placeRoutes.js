const express = require("express");
const { featured } = require("../controllers/placeController");

const router = express.Router();

router.get("/featured", featured); // public: Home works even if the login has expired

module.exports = router;