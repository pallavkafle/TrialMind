// Usage (from the backend folder):  node scripts/make-admin.js you@example.com        -> admin
//                                   node scripts/make-admin.js you@example.com user   -> back to user
require("dotenv").config();
const mongoose = require("mongoose");
const User = require("../src/models/User");

(async () => {
  const email = (process.argv[2] || "").toLowerCase();
  const role = process.argv[3] === "user" ? "user" : "admin";
  if (!email) throw new Error("Usage: node scripts/make-admin.js <email> [user]");
  await mongoose.connect(process.env.MONGO_URI);
  const user = await User.findOneAndUpdate({ email }, { role }, { new: true });
  if (!user) throw new Error(`No user with email ${email}`);
  console.log(`${user.email} is now: ${user.role}`);
  await mongoose.disconnect();
})().catch((e) => {
  console.error(e.message);
  process.exit(1);
});