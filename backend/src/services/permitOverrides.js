const PermitOverride = require("../models/PermitOverride");
const { setPermitOverrides } = require("./permitService");

// reads the admin's permit edits from MongoDB into memory (at startup and after every admin change)
const loadPermitOverrides = async () => {
  try {
    const items = await PermitOverride.find({});
    setPermitOverrides(items.map((o) => o.toObject()));
    console.log(`Permit overrides loaded: ${items.length}`);
  } catch (err) {
    console.error("Could not load permit overrides:", err.message);
  }
};

module.exports = { loadPermitOverrides };