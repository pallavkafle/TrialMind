const mongoose = require("mongoose");

const permitOverrideSchema = new mongoose.Schema(
  {
    permitId: { type: String, required: true, unique: true },
    fee: { type: mongoose.Schema.Types.Mixed, default: {} }, // any of nepali / saarc / foreign: number or null
    confidence: { type: String, enum: ["high", "medium", "low"] },
    note: { type: String, maxlength: 400 },
    updatedBy: { type: mongoose.Schema.Types.ObjectId, ref: "User" },
  },
  { timestamps: true, minimize: false }
);

module.exports = mongoose.model("PermitOverride", permitOverrideSchema);