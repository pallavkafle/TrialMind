const mongoose = require("mongoose");

const reviewSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true, index: true },
    destination: { type: String, required: true, trim: true, index: true },
    rating: { type: Number, required: true, min: 1, max: 5 },
    text: { type: String, required: true, trim: true, minlength: 3, maxlength: 2000 },
    // filled by the ML service; stays empty if it was unreachable
    sentiment: {
      label: String,
      confidence: Number,
      positiveProbability: Number,
      safetyFlag: String,
      matchedKeywords: [String],
    },
  },
  { timestamps: true }
);

module.exports = mongoose.model("Review", reviewSchema);