const { callMl } = require("./mlClient");

const TTL_MS = 10 * 60 * 1000;
let cache = { names: null, at: 0 };

// valid destination names come from the ML service, cached for 10 minutes
const getDestinationNames = async () => {
  if (cache.names && Date.now() - cache.at < TTL_MS) return cache.names;
  try {
    const list = await callMl("/destinations");
    cache = { names: new Set(list.map((d) => d.name)), at: Date.now() };
  } catch (err) {
    if (!cache.names) throw err; // nothing cached: surface the ML error
  }
  return cache.names; // on failure with a stale cache, keep using it
};

module.exports = { getDestinationNames };