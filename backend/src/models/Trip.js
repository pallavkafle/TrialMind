const mongoose = require("mongoose");

const tripSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
    title: { type: String, required: [true, "Title is required"], trim: true, maxlength: 120 },
    stops: { type: [String], default: [] },
    startingCity: { type: String, trim: true },
    durationDays: { type: Number, required: true, min: 1, max: 30 },
    groupSize: { type: Number, required: true, min: 1, max: 20 },
    totalNpr: { type: Number, required: true, min: 0 },
    request: { type: mongoose.Schema.Types.Mixed, default: {} },    // what the traveller asked for
    itinerary: { type: mongoose.Schema.Types.Mixed, required: true }, // full /itinerary response
  },
  { timestamps: true }
);

module.exports = mongoose.model("Trip", tripSchema);