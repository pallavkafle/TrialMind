const asyncHandler = require("../utils/asyncHandler");
const { getRates } = require("../services/currencyService");

// GET /api/currency/rates   (login required)
const rates = asyncHandler(async (req, res) => {
  res.json({ success: true, data: await getRates() });
});

module.exports = { rates };