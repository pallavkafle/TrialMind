"""
Evaluation of the review safety module on an author-labelled test set
(evaluation/review_testset.json).

Usage (from ml-service, venv active):
    python -m evaluation.eval_safety              # hazard rules + sentiment model
    python -m evaluation.eval_safety --no-model   # hazard rules only (instant)

Part 1 needs no model: precision/recall of HARD hazard detection and the soft-keyword rate.
Part 2 runs DistilBERT: accuracy on the clearly POSITIVE/NEGATIVE reviews (mixed ones are
skipped) and the distribution of final safety flags.
The test set was written by the project authors together with the rules, so Part 1 is a
regression test, not an independent benchmark. Part 2 (the model) is independent of the rules.
"""
import argparse
import json
from collections import Counter
from pathlib import Path

from safety import classify_safety, find_hazards

DATA = Path(__file__).with_name("review_testset.json")


def pct(n, d):
    return f"{100 * n / d:.1f}%" if d else "n/a"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-model", action="store_true")
    args = ap.parse_args()

    rows = json.loads(DATA.read_text(encoding="utf-8"))
    n_hard = sum(1 for r in rows if r["hard"])
    n_soft = sum(1 for r in rows if r["soft"])
    print(f"Test set: {len(rows)} reviews ({n_hard} with a hard hazard, {n_soft} with a soft keyword)\n")

    tp = fp = fn = tn = 0
    soft_hit = soft_unexpected = 0
    problems = []
    for r in rows:
        hard, soft = find_hazards(r["text"])
        pred, gold = bool(hard), r["hard"]
        if pred and gold:
            tp += 1
        elif pred and not gold:
            fp += 1
            problems.append(f"  FALSE ALARM {hard}: {r['text']}")
        elif gold and not pred:
            fn += 1
            problems.append(f"  MISSED HAZARD: {r['text']}")
        else:
            tn += 1
        if r["soft"] and soft:
            soft_hit += 1
        elif soft and not r["soft"]:
            soft_unexpected += 1

    print("PART 1  hard-hazard keyword rules (no model)")
    print(f"  precision {pct(tp, tp + fp)}   recall {pct(tp, tp + fn)}   (TP {tp}, FP {fp}, FN {fn}, TN {tn})")
    print(f"  soft keywords found in {soft_hit}/{n_soft} soft cases, {soft_unexpected} unexpected soft hits")
    for line in problems:
        print(line)

    if args.no_model:
        return

    from safety import _get_pipe
    print("\nPART 2  sentiment model (DistilBERT SST-2), loading ...")
    preds = _get_pipe()([r["text"] for r in rows], truncation=True, max_length=512)

    labelled = [(r, p) for r, p in zip(rows, preds) if r["sentiment"]]
    correct = sum(1 for r, p in labelled if str(p["label"]).upper() == r["sentiment"])
    print(f"  sentiment accuracy {pct(correct, len(labelled))} on {len(labelled)} clear reviews "
          f"({len(rows) - len(labelled)} mixed reviews skipped)")
    for r, p in labelled:
        if str(p["label"]).upper() != r["sentiment"]:
            print(f"  WRONG ({p['label']} {p['score']:.2f}, expected {r['sentiment']}): {r['text']}")

    flags = Counter()
    for r, p in zip(rows, preds):
        hard, soft = find_hazards(r["text"])
        flags[classify_safety(str(p["label"]).upper(), float(p["score"]), hard, soft)] += 1
    print(f"  final safety flags over all reviews: {dict(flags)}")


if __name__ == "__main__":
    main()