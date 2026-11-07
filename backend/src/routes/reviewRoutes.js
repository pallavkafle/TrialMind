const express = require("express");
const protect = require("../middleware/auth");
const { createReview, listReviews, myReviews, deleteReview } = require("../controllers/reviewController");

const router = express.Router();

router.get("/", listReviews);               // public
router.post("/", protect, createReview);
router.get("/mine", protect, myReviews);
router.delete("/:id", protect, deleteReview);

module.exports = router;