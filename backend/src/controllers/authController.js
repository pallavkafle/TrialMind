const jwt = require("jsonwebtoken");
const User = require("../models/User");
const asyncHandler = require("../utils/asyncHandler");
const { isValidCountry } = require("../utils/country");

const signToken = (userId) =>
  jwt.sign({ id: userId }, process.env.JWT_SECRET, {
    expiresIn: process.env.JWT_EXPIRES_IN || "7d",
  });

// POST /api/auth/register
const register = asyncHandler(async (req, res) => {
  const { name, email, password, nationality } = req.body;

  if (!name || !email || !password) {
    return res
      .status(400)
      .json({ success: false, message: "Name, email and password are required" });
  }

  if (password.length < 8) {
    return res.status(400).json({
      success: false,
      message: "Password must be at least 8 characters",
    });
  }

  if (nationality !== undefined && !isValidCountry(nationality)) {
    return res
      .status(400)
      .json({ success: false, message: "Nationality must be a valid 2-letter country code" });
  }

  const exists = await User.findOne({ email: email.toLowerCase() });
  if (exists) {
    return res
      .status(409)
      .json({ success: false, message: "An account with that email already exists" });
  }

  const user = await User.create({
    name,
    email,
    passwordHash: password, // pre-save hook hashes this
    nationality,
  });

  res.status(201).json({
    success: true,
    token: signToken(user._id),
    user: user.toSafeJSON(),
  });
});

// POST /api/auth/login
const login = asyncHandler(async (req, res) => {
  const { email, password } = req.body;

  if (!email || !password) {
    return res
      .status(400)
      .json({ success: false, message: "Email and password are required" });
  }

  // passwordHash is select:false, so ask for it explicitly
  const user = await User.findOne({ email: email.toLowerCase() }).select(
    "+passwordHash"
  );

  // same message for both cases — don't leak which emails exist
  if (!user || !(await user.comparePassword(password))) {
    return res
      .status(401)
      .json({ success: false, message: "Invalid email or password" });
  }

  res.status(200).json({
    success: true,
    token: signToken(user._id),
    user: user.toSafeJSON(),
  });
});

// GET /api/auth/me   (protected)
const me = asyncHandler(async (req, res) => {
  res.status(200).json({ success: true, user: req.user.toSafeJSON() });
});

// PUT /api/auth/me   body: { name?, nationality? }   (protected)
const updateMe = asyncHandler(async (req, res) => {
  const { name, nationality } = req.body;
  const bad = (message) => res.status(400).json({ success: false, message });
  if (name === undefined && nationality === undefined)
    return bad("Send a name or a nationality to update");

  const update = {};
  if (name !== undefined) {
    const clean = typeof name === "string" ? name.trim() : "";
    if (clean.length < 2 || clean.length > 60) return bad("Name must be 2 to 60 characters");
    update.name = clean;
  }
  if (nationality !== undefined) {
    if (!isValidCountry(nationality)) return bad("Nationality must be a valid 2-letter country code");
    update.nationality = String(nationality).trim().toUpperCase();
  }

  const user = await User.findByIdAndUpdate(req.user._id, update, { new: true, runValidators: true });
  res.status(200).json({ success: true, user: user.toSafeJSON() });
});

module.exports = { register, login, me, updateMe };