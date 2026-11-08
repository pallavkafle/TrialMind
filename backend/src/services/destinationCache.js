const { callMl } = require("./mlClient");

const TTL_MS = 10 * 60 * 1000;
let cache = { map: null, at: 0 };

// name -> { lat, lon }, from the ML service, cached for 10 minutes
const getDestinationMap = async () => {
  if (cache.map && Date.now() - cache.at < TTL_MS) return cache.map;
  try {
    const list = await callMl("/destinations");
    cache = {
      map: new Map(list.map((d) => [d.name, { lat: d.latitude, lon: d.longitude }])),
      at: Date.now(),
    };
  } catch (err) {
    if (!cache.map) throw err; // nothing cached: surface the ML error
  }
  return cache.map; // on failure with a stale cache, keep using it
};

const getDestinationNames = async () => new Set((await getDestinationMap()).keys());

module.exports = { getDestinationMap, getDestinationNames };