require("dotenv").config();
const app = require("./app");
const connectDB = require("./config/db");
const { loadPermitOverrides } = require("./services/permitOverrides");

const PORT = process.env.PORT || 5000;

const start = async () => {
  if (!process.env.JWT_SECRET || process.env.JWT_SECRET.length < 16)
    console.warn("WARNING: JWT_SECRET is missing or shorter than 16 characters. Use a long random value.");
  await connectDB();
  await loadPermitOverrides();
  // 0.0.0.0 so the emulator and physical devices on your LAN can reach it
  app.listen(PORT, "0.0.0.0", () =>
    console.log(`Server running on http://localhost:${PORT}`)
  );
};

start();