"""B9-1: data consistency, budget maths, saved model, day allocation, place-name matching.
Run from the ml-service folder:  venv\\Scripts\\python.exe -m pytest
"""
import json
import math

import joblib
import pandas as pd
import pytest
from fastapi import HTTPException

import app as svc
from itinerary import allocate_days, resolve_destinations
from routing.destinations import DESTINATIONS, GAMMA

NAMES = sorted(DESTINATIONS)
TREK = "Everest Base Camp"
NO_PORTER = "Chitwan National Park"


def req(**over):
    base = dict(
        starting_city="Pokhara", destination=TREK, duration_days=12, group_size=4,
        travel_month="October", nationality_category="Foreign", traveler_type="Group",
        accommodation_tier="Standard", transport_mode="Flight",
        use_guide=True, use_porter=True, skip_ml=True,
    )
    base.update(over)
    return svc.BudgetRequest(**base)


def run(**over):
    return svc.predict(req(**over))


# ------------------------------------------------------------------ data
def test_there_are_27_destinations():
    assert len(DESTINATIONS) == 27


def test_all_registries_list_the_same_destinations():
    names = set(DESTINATIONS)
    assert set(svc.COMPONENTS) == names
    assert set(svc.DEST_META) == names
    assert set(svc.DEST_COORD) == names


@pytest.mark.parametrize("name", NAMES)
def test_destination_is_well_formed(name):
    lat, lon, archetype, altitude = DESTINATIONS[name][:4]
    assert 26.0 <= lat <= 30.6
    assert 80.0 <= lon <= 88.3
    assert archetype in GAMMA
    assert 50 <= altitude <= 6500


@pytest.mark.parametrize("name", NAMES)
def test_the_two_coordinate_registries_agree(name):
    a, b = DESTINATIONS[name], svc.DEST_COORD[name]
    assert abs(a[0] - b[0]) < 0.05
    assert abs(a[1] - b[1]) < 0.05


# ------------------------------------------------------------------ transport and distance
def test_transport_fare_steps():
    assert svc.transport_fare(0) == 0
    assert svc.transport_fare(100) == pytest.approx(500)
    assert svc.transport_fare(200) == pytest.approx(1000)
    assert svc.transport_fare(300) == pytest.approx(1000 + 100 * 18)
    assert svc.transport_fare(150) < svc.transport_fare(250)


def test_haversine_kathmandu_to_pokhara():
    d = svc.haversine(svc.CITIES["Kathmandu"], svc.CITIES["Pokhara"])
    assert 139 <= d <= 146


def test_transport_cost_follows_the_rounded_distance():
    r = run()
    straight = svc.haversine(svc.CITIES["Pokhara"], svc.DEST_COORD[TREK])
    assert r.start_to_dest_km >= straight          # road curvature only adds distance
    assert r.breakdown.transport_npr == pytest.approx(svc.transport_fare(r.start_to_dest_km) * 4, abs=0.02)


# ------------------------------------------------------------------ budget
def test_reference_everest_budget():
    runs = {t: run(accommodation_tier=t) for t in ("Budget", "Standard", "Luxury")}
    matches = [t for t, r in runs.items() if r.itemized_total_npr == pytest.approx(451507.36, abs=0.01)]
    assert matches, {t: r.itemized_total_npr for t, r in runs.items()}
    r = runs[matches[0]]
    assert r.breakdown.permit_npr == pytest.approx(16580.16, abs=0.01)
    assert r.per_person_npr == pytest.approx(112876.84, abs=0.01)


def test_breakdown_adds_up_to_the_total():
    r = run()
    b = r.breakdown
    parts = b.permit_npr + b.guide_npr + b.porter_npr + b.lodging_npr + b.meals_npr + b.transport_npr
    assert r.itemized_total_npr == pytest.approx(parts, abs=0.02)
    assert r.per_person_npr == pytest.approx(r.itemized_total_npr / 4, abs=0.02)


def test_group_size_scales_per_person_costs_but_not_the_guide():
    a, b = run(group_size=2), run(group_size=4)
    for name in ("permit_npr", "lodging_npr", "meals_npr"):
        assert getattr(b.breakdown, name) == pytest.approx(2 * getattr(a.breakdown, name), abs=0.02)
    assert b.breakdown.guide_npr == pytest.approx(a.breakdown.guide_npr, abs=0.02)
    assert b.breakdown.porter_npr == pytest.approx(a.breakdown.porter_npr, abs=0.02)


def test_trip_length_scales_guide_lodging_and_meals_but_not_the_permit():
    a, b = run(duration_days=6), run(duration_days=12)
    for name in ("guide_npr", "porter_npr", "lodging_npr", "meals_npr"):
        assert getattr(b.breakdown, name) == pytest.approx(2 * getattr(a.breakdown, name), abs=0.02)
    assert b.breakdown.permit_npr == pytest.approx(a.breakdown.permit_npr, abs=0.02)


def test_guide_switch():
    on, off = run(use_guide=True), run(use_guide=False)
    assert off.breakdown.guide_npr == 0
    assert off.itemized_total_npr == pytest.approx(on.itemized_total_npr - on.breakdown.guide_npr, abs=0.02)


def test_porter_only_where_a_porter_exists():
    assert run(use_porter=False).breakdown.porter_npr == 0
    assert run(use_porter=True).porter_available is True
    flat = run(destination=NO_PORTER, use_porter=True)
    assert flat.porter_available is False
    assert flat.breakdown.porter_npr == 0


def test_unknown_destination_or_city_is_rejected():
    with pytest.raises(HTTPException) as e1:
        run(destination="Atlantis")
    assert e1.value.status_code == 400
    with pytest.raises(HTTPException) as e2:
        run(starting_city="Gotham")
    assert e2.value.status_code == 400


def test_every_destination_and_city_gives_a_sane_budget():
    for dest in svc.COMPONENTS:
        for city in svc.CITIES:
            r = run(destination=dest, starting_city=city, duration_days=5, group_size=2)
            assert math.isfinite(r.itemized_total_npr) and r.itemized_total_npr > 0, (dest, city)
            assert r.start_to_dest_km >= 0, (dest, city)


# ------------------------------------------------------------------ saved model
def test_saved_model_and_metrics():
    metrics = json.loads((svc.BASE / "models" / "metrics.json").read_text(encoding="utf-8"))
    assert metrics["final_model"] == "RandomForest"
    assert metrics["test_r2"] >= 0.95
    assert len(metrics["baseline_comparison"]) >= 3

    model = joblib.load(svc.MODEL_PATH)
    altitude, restricted = svc.DEST_META[TREK]
    row = pd.DataFrame([{
        "Duration_Days": 12, "Group_Size": 4, "Max_Altitude_Meters": altitude, "Is_Restricted_Area": restricted,
        "Destination": TREK, "Travel_Month": "October", "Nationality_Category": "Foreign",
        "Traveler_Type": "Group", "Accommodation_Tier": "Standard", "Transport_Mode": "Flight",
    }])
    pred = float(model.predict(row)[0])
    assert math.isfinite(pred) and 0 < pred < 10_000_000


# ------------------------------------------------------------------ itinerary helpers
def test_day_allocation_adds_up():
    for n in range(1, 11):
        stops = NAMES[:n]
        for total in range(n, 31):
            alloc = allocate_days(stops, total)
            assert len(alloc) == n
            assert sum(alloc) == total, (stops, total, alloc)
            assert min(alloc) >= 1, (stops, total, alloc)


def test_trekking_gets_more_days_than_a_cultural_stop():
    alloc = allocate_days(["Pashupatinath", TREK], 10)
    assert alloc[1] > alloc[0]


def test_place_names_in_text_are_found_in_order():
    assert resolve_destinations("Visit Boudhanath, then Pashupatinath") == ["Boudhanath", "Pashupatinath"]
    assert resolve_destinations("pashupatinath and boudhanath") == ["Pashupatinath", "Boudhanath"]
    assert resolve_destinations("a trip to everest base camp") == [TREK]
    assert resolve_destinations("something with no places at all") == []


@pytest.mark.parametrize("name", NAMES)
def test_every_destination_name_is_recognised(name):
    assert name in resolve_destinations(name)