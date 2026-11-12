// The 27 curated destinations live in data/destinations.json, a snapshot of the ML service's
// list. Refresh it with `node scripts/sync-destinations.js` (ML service running) if destinations
// ever change. Keeping the list in Node means reviews, permits, weather, hotels and the hospital
// lookup keep working when the ML service is down.
const destinations = require("../data/destinations.json");

const map = new Map(destinations.map((d) => [d.name, { lat: d.latitude, lon: d.longitude }]));
const names = new Set(map.keys());

const getDestinationMap = async () => map;
const getDestinationNames = async () => names;

module.exports = { getDestinationMap, getDestinationNames };