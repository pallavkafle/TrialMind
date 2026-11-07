const express = require("express");
const protect = require("../middleware/auth");
const { createTrip, listTrips, getTrip, deleteTrip } = require("../controllers/tripController");

const router = express.Router();

router.use(protect);
router.post("/", createTrip);
router.get("/", listTrips);
router.get("/:id", getTrip);
router.delete("/:id", deleteTrip);

module.exports = router;