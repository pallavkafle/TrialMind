const mongoose = require("mongoose");

const favoriteSchema = new mongoose.Schema(
  {
    user: { type: mongoose.Schema.Types.ObjectId, ref: "User", required: true },
    destination: { type: String, required: true, trim: true },
  },
  { timestamps: true }
);
favoriteSchema.index({ user: 1, destination: 1 }, { unique: true });

module.exports = mongoose.model("Favorite", favoriteSchema);