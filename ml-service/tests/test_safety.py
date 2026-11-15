"""B9-2: review safety rules (hazard keywords with clause-scoped negation), the flag logic,
the author-labelled review set, and an opt-in test of the sentiment model."""
import json
import os
from pathlib import Path

import pytest
from pydantic import ValidationError

from safety import BatchIn, ReviewIn, classify_safety, find_hazards

DATA = Path(__file__).resolve().parents[1] / "evaluation" / "review_testset.json"

model_only = pytest.mark.skipif(
    os.getenv("TRAILMIND_MODEL_TESTS") != "1",
    reason="slow: set TRAILMIND_MODEL_TESTS=1 to run the sentiment-model tests",
)


def hard(text):
    return set(find_hazards(text)[0])


def soft(text):
    return set(find_hazards(text)[1])


# ------------------------------------------------------------------ hazards that must be found
HARD_FOUND = [
    ("A landslide blocked the trail near the village.", {"landslide"}),
    ("Heavy avalanche warnings and a rockfall on day two", {"avalanche", "rockfall"}),
    ("We got altitude sickness above 4000 m", {"altitude sickness"}),
    ("Mountain sickness hit two of us", {"altitude sickness"}),
    ("Two trekkers had hypothermia", {"hypothermia"}),
    ("He got frostbite on his fingers", {"frostbite"}),
    ("There was an injury on the descent", {"injury"}),
    ("Someone was injured by falling stones", {"injury"}),
    ("A serious accident happened", {"accident"}),
    ("We were stranded for two days", {"stranded"}),
    ("The bridge collapsed", {"collapse"}),
    ("The path is dangerous after rain", {"dangerous"}),
    ("The old suspension bridge felt unsafe", {"dangerous"}),
    ("A blizzard hit us at the pass", {"blizzard"}),
    ("A sudden snowstorm closed the pass", {"blizzard"}),
    ("The earthquake damaged the lodge", {"earthquake"}),
    ("River flooded the road", {"flood"}),
    ("The trail has a lot of rock fall", {"rockfall"}),
    ("AVALANCHE RISK IS HIGH", {"avalanche"}),
]


@pytest.mark.parametrize("text,expected", HARD_FOUND)
def test_hard_hazards_are_found(text, expected):
    assert hard(text) == expected


# ------------------------------------------------------------------ negated mentions are ignored
NEGATED = [
    "There was no landslide on the whole trek.",
    "We did not see any avalanche.",
    "I never felt unsafe.",
    "Zero injuries in our group.",
    "Without any accident we reached base camp.",
    "We didn't have any altitude sickness.",
    "There wasn\u2019t any flood damage.",
    "Nothing dangerous happened.",
    "No frostbite, no hypothermia, all fine.",
    "The guide said there is no risk of avalanche.",
    "We could not find any rockfall.",
    "Neither an avalanche nor a landslide.",
]


@pytest.mark.parametrize("text", NEGATED)
def test_negated_hazards_are_ignored(text):
    assert hard(text) == set()


# ------------------------------------------------------------------ negation stays inside its clause (the B2 fix)
SCOPED = [
    ("There was no rain but a landslide blocked the road", {"landslide"}),
    ("No problems, avalanche warnings were common", {"avalanche"}),
    ("It was not crowded however a rockfall scared us", {"rockfall"}),
    ("Not dangerous but the rockfall was scary", {"rockfall"}),
    ("No delays and the bridge collapsed", {"collapse"}),
    ("no one told us about the avalanche risk", {"avalanche"}),   # the "no" is outside the 4-word window
]


@pytest.mark.parametrize("text,expected", SCOPED)
def test_negation_does_not_leak_into_the_next_clause(text, expected):
    assert hard(text) == expected


# ------------------------------------------------------------------ harmless text raises nothing
HARMLESS = [
    "Beautiful views and friendly staff",
    "We accidentally left a bag at the lodge",
    "The altitude was high but I felt great",
    "A rocky trail with a nice waterfall",
    "We loved the hot springs",
    "Great food, very clean rooms.",
    "",
]


@pytest.mark.parametrize("text", HARMLESS)
def test_harmless_text_has_no_hazards(text):
    assert find_hazards(text) == ([], [])


# ------------------------------------------------------------------ soft keywords
def test_soft_keywords():
    assert soft("The rescue team was friendly") == {"rescue"}
    assert soft("They arranged an emergency evacuation") == {"emergency", "evacuation"}
    assert soft("Helicopter evacuation was needed") == {"evacuation"}
    assert soft("No emergency happened") == set()
    assert hard("The rescue team was friendly") == set()


# ------------------------------------------------------------------ final flag
FLAGS = [
    ("POSITIVE", 0.99, [], [], "OK"),
    ("NEGATIVE", 0.90, [], [], "CAUTION"),
    ("NEGATIVE", 0.85, [], [], "CAUTION"),
    ("NEGATIVE", 0.84, [], [], "OK"),
    ("POSITIVE", 0.99, ["flood"], [], "WARNING"),
    ("NEGATIVE", 0.60, ["flood"], [], "WARNING"),
    ("POSITIVE", 0.90, [], ["rescue"], "CAUTION"),
    ("NEGATIVE", 0.50, [], ["rescue"], "WARNING"),
    ("POSITIVE", 0.90, ["flood"], ["rescue"], "WARNING"),
]


@pytest.mark.parametrize("label,conf,hard_hits,soft_hits,expected", FLAGS)
def test_safety_flag(label, conf, hard_hits, soft_hits, expected):
    assert classify_safety(label, conf, hard_hits, soft_hits) == expected


# ------------------------------------------------------------------ the author-labelled review set (no model)
def test_rules_on_the_review_set():
    rows = json.loads(DATA.read_text(encoding="utf-8"))
    assert len(rows) >= 20
    tp = fp = fn = 0
    problems = []
    for r in rows:
        found, _ = find_hazards(r["text"])
        pred, gold = bool(found), bool(r["hard"])
        if pred and gold:
            tp += 1
        elif pred:
            fp += 1
            problems.append(f"FALSE ALARM {found}: {r['text']}")
        elif gold:
            fn += 1
            problems.append(f"MISSED HAZARD: {r['text']}")
    precision = tp / (tp + fp) if tp + fp else 1.0
    recall = tp / (tp + fn) if tp + fn else 1.0
    assert recall >= 0.90, problems
    assert precision >= 0.90, problems


# ------------------------------------------------------------------ input limits
def test_review_text_limits():
    ReviewIn(text="abc")
    ReviewIn(text="x" * 2000)
    with pytest.raises(ValidationError):
        ReviewIn(text="ab")
    with pytest.raises(ValidationError):
        ReviewIn(text="x" * 2001)


def test_batch_limits():
    BatchIn(reviews=["a fine review"] * 100)
    with pytest.raises(ValidationError):
        BatchIn(reviews=[])
    with pytest.raises(ValidationError):
        BatchIn(reviews=["a fine review"] * 101)


# ------------------------------------------------------------------ sentiment model (opt-in, slow)
@model_only
def test_sentiment_model_on_obvious_reviews():
    from safety import _get_pipe
    cases = [
        ("The trek was wonderful and the views were amazing", "POSITIVE"),
        ("Terrible experience, dirty rooms and rude staff", "NEGATIVE"),
    ]
    raws = _get_pipe()([t for t, _ in cases], truncation=True, max_length=512)
    for (text, expected), raw in zip(cases, raws):
        assert str(raw["label"]).upper() == expected, text


@model_only
def test_a_hazard_overrides_a_positive_review():
    from safety import _get_pipe, _to_out
    text = "A landslide blocked the trail, but the guide was great."
    raw = _get_pipe()([text], truncation=True, max_length=512)[0]
    out = _to_out(text, raw)
    assert out.safety_flag == "WARNING"
    assert "landslide" in out.matched_keywords