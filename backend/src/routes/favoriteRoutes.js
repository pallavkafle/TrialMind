const express = require("express");
const protect = require("../middleware/auth");
const { listFavorites, toggleFavorite } = require("../controllers/favoriteController");

const router = express.Router();

router.use(protect);
router.get("/", listFavorites);
router.post("/toggle", toggleFavorite);

module.exports = router;