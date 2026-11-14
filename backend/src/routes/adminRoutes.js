const express = require("express");
const protect = require("../middleware/auth");
const requireAdmin = require("../middleware/admin");
const a = require("../controllers/adminController");

const router = express.Router();

router.use(protect, requireAdmin);
router.get("/stats", a.stats);
router.get("/reviews", a.listReviews);
router.delete("/reviews/:id", a.deleteReview);
router.get("/permits", a.listPermits);
router.patch("/permits/:id", a.updatePermit);
router.delete("/permits/:id/override", a.resetPermit);

module.exports = router;