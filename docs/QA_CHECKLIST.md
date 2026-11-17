# TrailMind: QA Checklist (Walk Every Screen Once)

Numbers marked **golden** come from `docs/golden.json` (run `node scripts/golden.js` in `backend` to regenerate them). Verify on a real phone over mobile data/Wi-Fi using the Cloudflare tunnel.

## 0. Before You Start
* Run `.\start-trailmind.bat` from the project root and confirm MongoDB, `ml-service`, `backend`, and the `trailmind` Cloudflare tunnel all print `OK`.
* Confirm `https://trailmind.pallavkafle.com.np/api/health` returns `200 OK` and `http://localhost:8000/health` shows `model_loaded: true`.
* Tests pass: `cd ml-service` then `venv\Scripts\python.exe -m pytest` is green; `cd backend` then `npm test` is green.
* Golden data verified: `node scripts/seed-demo.js` ran; `node scripts/golden.js` shows no FAIL and no changes.
* Fresh `app-debug.apk` installed on a real phone with location/GPS enabled and Settings pointing to `https://trailmind.pallavkafle.com.np/`.

## 1. Start and Login
* Cold start: blue splash about 1 s, then Login (or Home if already logged in), no white flash.
* Wrong password: red message, stays on Login.
* `demo@example.com` / `Demo@1234`: opens Home, drawer shows "Demo Traveller".
* Register a new account with a country picked: lands on Home; Profile shows that country.
* Login and Register stay light even in dark mode.

## 2. Home
* Greeting matches the time of day; sun/moon button switches the theme and remembers it after restart.
* Tiles: Hotels, Currency, Emergency, Places, Trekking, Permits all open.
* Search bar ("Where do you want to go?") opens Places with the keyboard up.
* Popular Places shows real cards: rating with a review count, "from NPR ... per person" with the basis note below the row, no invented figures.
* Drawer: Home, My Trips, Saved Places, Budget, Profile, Settings, Map, Logout all work.

## 3. AI Trip Guide (Chat)
* Chip "Temples and a safari" then **Understand my trip**: the card reads "a trip to Pashupatinath, Boudhanath and Chitwan National Park, for 7 days, for 4 people, in October, starting from Pokhara; standard accommodation, by public bus, with a guide", tag "Read by our rules".
* Fields are filled and editable; missing ones are amber (try "somewhere with big mountain views, about 10 days").
* Reading a place we do not have ("Mount Kailash and Lumbini") keeps Lumbini and says "We don't have: Mount Kailash" (needs the AI to be up).
* **Create itinerary** (Nationality Foreign): opens the itinerary; Back keeps your edits; Edit my text returns to the text.
* "Prefer a form? Use guided questions" opens the form.

## 4. Guided Questions and Itinerary
* Reference trip (Pashupatinath, Boudhanath, Chitwan National Park, 7 days, group 4, Pokhara, Foreign, Group, Standard, Public Bus, guide on, porter off):
  * Route **starts at Chitwan National Park**.
  * Permit line **NPR 11,952**.
  * Total NPR and km match **golden** `itinerary.ref.total_npr` / `itinerary.ref.total_km`.
  * Day-by-day cards, budget breakdown, an amber note (not red text) if the AI was not used.
* **View route on map**: numbered markers, red line, "straight lines" note.
* **Save trip**, then My Trips shows it; open it; delete it (confirmation box).
* More than 10 places, or more places than days, is refused with a clear message.

## 5. Budget Screen
* Pokhara to Everest Base Camp, 12 days, group 4, October, **Foreign**, Group, Standard, Flight, guide and porter on:
  * Permit **NPR 24,000**.
  * Total **NPR 458,927**, per person **NPR 114,732**.
  * ML cross-check shown separately from the itemized total.
* Same trip as **Domestic**: permit is lower; nothing shows "undefined".
* Porter switch has no effect on a destination without porters (for example Chitwan).

## 6. Permits
* Everest Base Camp, Foreign, 4 people: **NPR 24,000** total, each permit line has a confidence tag.
* Domestic: one missing fee is shown as "needs checking", not as 0.
* Manaslu Circuit, 14 days, October, Foreign: the restricted-area permit line is **NPR 31,160** per person.
* Two places that share a permit: the shared permit appears once.
* A destination with no permit data says so in words.

## 7. Places, Reviews and Weather
* Places list: category chips, search box, heart on each row.
* Annapurna Circuit: summary shows a **safety warning** (landslide review); the Everest review ("no altitude sickness") is **not** flagged.
* Nagarkot: shows the strongly negative review with a **caution**.
* Place page: 7-day weather card, visitor reviews, Places to stay nearby, View on map, heart in the top bar.
* Write a review as `demo@example.com` (for example Lumbini, 5 stars): it appears with a sentiment tag; delete it with the bin icon.
* Re-open Popular Places on Home: Lumbini now shows a rating.

## 8. Emergency
* Choose Nagarkot: nearest hospitals/clinics with distance; first result matches **golden** `health.nagarkot.first_name`.
* Type filters work; **Call** opens the dialer without calling; **Map** opens a map app.
* Numbers 100, 102, 101 and 1144 are shown; OpenStreetMap credit present.
* "Use my location" asks for permission; denying it shows a message and the destination list still works.

## 9. Hotels
* Nagarkot: first result matches **golden** `hotels.nagarkot.first_name` (about 0.1 km); no prices anywhere; note says contact the place.
* Area chips (Kathmandu, Pokhara, Sauraha, Lukla, Namche, Jomsom, Nepalgunj, Itahari, Surkhet) show the hub name in the header.
* Type and distance chips refine the same area.
* Bardia National Park: "nothing mapped within the distance, the map data may be incomplete" plus the nearest mapped place (about 200 km).

## 10. Currency
* Converter converts both ways; the rates list loads; the budget rate used by the app is 152 NPR per USD.

## 11. Map
* All 27 destinations as coloured circles; tapping shows name, category and altitude.
* Hospitals and Hotels chips draw squares; Search this area reloads around the map centre; zoom buttons stay usable.
* Tile credit "(c) OpenStreetMap contributors" visible.

## 12. Profile, Saved Places, Settings
* Saved Places lists the 3 seeded places, newest first (Lumbini on top); removing one with the heart removes it from the list.
* Profile: change the name, pick a country, Save; the drawer name and the default price group update.
* Settings: Light, Dark and Follow my phone work; credits open their websites; Clear saved data asks first; version shown.

## 13. Offline and Failure Behaviour
* Open Places, a place, Currency, Emergency and Hotels (Nagarkot) once while online.
* Stop the backend: the amber "Offline: showing saved data" pill appears and the same screens still show their data.
* A place never opened shows a clear "Cannot reach the server" message, not a crash.
* Hearting a place while offline reverts with a short message.
* Start the backend again; open Currency: fresh data and the pill disappears.
* Stop only the ml-service: Places still loads (destination list is kept in Node); an itinerary shows a friendly error; reviews can still be posted.
* Log out clears saved data (Places shows an error until you are online once).

## 14. Security and Limits
* A made-up or expired token returns to Login with a message.
* The Android app shows no passwords or tokens in Logcat (filter `okhttp`).
* 6 or more reviews for one place by one user are refused with a clear message.
* No secret appears in git: `git ls-files | findstr /i ".env"` lists only the `.env.example` files.

## 15. Look and Feel
* Every screen readable in dark mode; no white boxes or invisible text.
* Status bar and navigation bar icons readable in both themes.
* Rotate the phone on Home and the itinerary: no crash.
* A 30-minute free-use session produces no crash.