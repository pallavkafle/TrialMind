"""
B9-4: evidence for the report. Writes charts and RESULTS.md to docs/evidence/.

Run from the ml-service folder:
    venv\\Scripts\\python.exe -m evaluation.make_evidence             (about 1-3 minutes)
    venv\\Scripts\\python.exe -m evaluation.make_evidence --no-model  (skips the DistilBERT part)

Everything is computed here from the real code and data; no number is typed in by hand.
The Random Forest is retrained on the same 80/20 split (random_state=42) that train_model.py uses,
so the test-set numbers are honest (the saved model may have been fitted on all rows).
"""
import argparse
import heapq
import itertools
import json
import math
import platform
import random
import sys
import time
from collections import Counter
from datetime import date
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ML = Path(__file__).resolve().parents[1]
if str(ML) not in sys.path:
    sys.path.insert(0, str(ML))
OUT = ML.parent / "docs" / "evidence"
BLUE, ORANGE, GREY = "#1F5C8B", "#E07B00", "#8A94A6"


def npr(v):
    return f"{v:,.0f}"


def save(fig, name):
    OUT.mkdir(parents=True, exist_ok=True)
    fig.tight_layout()
    fig.savefig(OUT / name, dpi=150)
    plt.close(fig)
    print("  wrote", name)


def table(header, rows):
    out = ["| " + " | ".join(header) + " |", "|" + "|".join(["---"] * len(header)) + "|"]
    out += ["| " + " | ".join(str(c) for c in r) + " |" for r in rows]
    return out


def confusion(ax, m, rows, cols, title):
    ax.imshow(m, cmap="Blues")
    for i in range(m.shape[0]):
        for j in range(m.shape[1]):
            ax.text(j, i, str(int(m[i, j])), ha="center", va="center", fontsize=14,
                    color="white" if m[i, j] > m.max() / 2 else "black")
    ax.set_xticks(range(len(cols)))
    ax.set_xticklabels(cols)
    ax.set_yticks(range(len(rows)))
    ax.set_yticklabels(rows)
    ax.set_xlabel("Predicted")
    ax.set_ylabel("Actual")
    ax.set_title(title)


# ====================================================================== 1. budget model
def section_budget(md, facts):
    from sklearn.ensemble import RandomForestRegressor
    from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
    from sklearn.model_selection import train_test_split
    import train_model as tm

    saved = json.loads((ML / "models" / "metrics.json").read_text(encoding="utf-8"))
    rows = saved["baseline_comparison"]
    names = [r["model"] for r in rows]

    fig, ax = plt.subplots(1, 2, figsize=(11, 4.2))
    x, w = np.arange(len(rows)), 0.38
    ax[0].bar(x - w / 2, [r["rmse"] for r in rows], w, label="Test RMSE", color=BLUE)
    ax[0].bar(x + w / 2, [r["cv_rmse"] for r in rows], w, label="5-fold CV RMSE", color=ORANGE)
    ax[0].set_xticks(x)
    ax[0].set_xticklabels(names, rotation=15)
    ax[0].set_ylabel("NPR")
    ax[0].set_title("Error (lower is better)")
    ax[0].legend()
    ax[1].bar(names, [r["r2"] for r in rows], color=BLUE)
    ax[1].set_ylim(0.7, 1.0)
    ax[1].set_title("R2 on the test set (axis starts at 0.7)")
    ax[1].tick_params(axis="x", rotation=15)
    for i, r in enumerate(rows):
        ax[1].text(i, r["r2"] + 0.003, f"{r['r2']:.3f}", ha="center", fontsize=9)
    save(fig, "model_comparison.png")

    df = tm.load_data()
    X = df[tm.NUMERIC + tm.CATEGORICAL]
    y = df[tm.TARGET]
    X_tr, X_te, y_tr, y_te = train_test_split(X, y, test_size=0.2, random_state=42)
    print("  training the Random Forest on", len(X_tr), "rows ...")
    pipe = tm.build_pipeline(RandomForestRegressor(n_estimators=300, random_state=42, n_jobs=-1))
    pipe.fit(X_tr, y_tr)
    pred = pipe.predict(X_te)
    truth = y_te.to_numpy(dtype=float)
    rmse = float(np.sqrt(mean_squared_error(truth, pred)))
    mae = float(mean_absolute_error(truth, pred))
    r2 = float(r2_score(truth, pred))
    rel = np.abs(pred - truth) / np.maximum(np.abs(truth), 1.0)
    within10, within20 = float((rel <= 0.10).mean()), float((rel <= 0.20).mean())
    mean_cost = float(truth.mean())
    reproduced = abs(rmse - saved["test_rmse"]) < 1.0

    fig, ax = plt.subplots(1, 2, figsize=(11, 4.4))
    top = float(max(truth.max(), pred.max())) / 1000
    ax[0].scatter(truth / 1000, pred / 1000, s=10, alpha=0.4, color=BLUE)
    ax[0].plot([0, top], [0, top], color=ORANGE, lw=1.5, label="perfect prediction")
    ax[0].set_xlabel("Actual total cost (NPR thousands)")
    ax[0].set_ylabel("Predicted (NPR thousands)")
    ax[0].set_title(f"Random Forest, test set (n={len(truth)})")
    ax[0].legend()
    ax[0].text(0.03, 0.82, f"RMSE {npr(rmse)}\nMAE {npr(mae)}\nR2 {r2:.3f}", transform=ax[0].transAxes,
               bbox=dict(boxstyle="round", facecolor="white", edgecolor=GREY))
    ax[1].hist((pred - truth) / 1000, bins=40, color=BLUE)
    ax[1].axvline(0, color=ORANGE, lw=1.5)
    ax[1].set_xlabel("Prediction error (NPR thousands)")
    ax[1].set_ylabel("Number of test rows")
    ax[1].set_title("Residuals (predicted minus actual)")
    save(fig, "budget_model_fit.png")

    pre, reg = pipe[:-1], pipe[-1]
    cols = tm.NUMERIC + tm.CATEGORICAL
    totals = {}
    for fname, value in zip(pre.get_feature_names_out(), reg.feature_importances_):
        short = fname.split("__", 1)[-1]
        owner = next((c for c in sorted(cols, key=len, reverse=True) if short == c or short.startswith(c + "_")), short)
        totals[owner] = totals.get(owner, 0.0) + float(value)
    order = sorted(totals.items(), key=lambda kv: kv[1])
    fig, ax = plt.subplots(figsize=(7, 4))
    ax.barh([k for k, _ in order], [v * 100 for _, v in order], color=BLUE)
    ax.set_xlabel("Share of the Random Forest's importance (%)")
    ax.set_title("What drives the predicted cost (impurity-based)")
    save(fig, "feature_importance.png")

    best = min(rows, key=lambda r: r["rmse"])
    rf = next(r for r in rows if r["model"] == "RandomForest")
    md += ["## 1. Budget model (Random Forest)", ""]
    md += [f"Dataset: {len(df)} rows; trained on {len(X_tr)}, tested on {len(X_te)} (80/20 split, random_state=42). "
           f"Mean trip cost in the test set: NPR {npr(mean_cost)}.", ""]
    md += table(["Model", "Test RMSE (NPR)", "Test MAE (NPR)", "Test R2", "5-fold CV RMSE (NPR)"],
                [[r["model"], npr(r["rmse"]), npr(r["mae"]), f"{r['r2']:.3f}", npr(r["cv_rmse"])] for r in rows])
    md += ["", f"Final model in the app: **{saved['final_model']}**. Source: models/metrics.json (written by train_model.py).", "",
           "Retrained here on the same split, the Random Forest gives:", ""]
    md += table(["Metric", "Value"], [
        ["RMSE", npr(rmse)], ["MAE", f"{npr(mae)} ({100 * mae / mean_cost:.1f}% of the mean cost)"], ["R2", f"{r2:.3f}"],
        ["Predictions within 10% of the actual cost", f"{100 * within10:.1f}% of test rows"],
        ["Predictions within 20% of the actual cost", f"{100 * within20:.1f}% of test rows"],
        ["Reproduces metrics.json", "yes" if reproduced else f"NO (RMSE differs by {abs(rmse - saved['test_rmse']):,.0f})"]])
    md += ["", "**Notes to address in the report**", ""]
    if best["model"] != "RandomForest":
        md.append(f"- {best['model']} has the lowest test RMSE ({npr(best['rmse'])}) and CV RMSE ({npr(best['cv_rmse'])}); "
                  f"the Random Forest has {npr(rf['rmse'])} and {npr(rf['cv_rmse'])}. Do not claim the Random Forest is the most "
                  "accurate model; state the reason it was chosen, or be ready to answer the question.")
    if rf["cv_rmse"] > 1.2 * rf["rmse"]:
        md.append(f"- The Random Forest's cross-validated error ({npr(rf['cv_rmse'])}) is higher than its single-split test error "
                  f"({npr(rf['rmse'])}), so the single split looks optimistic; quote both numbers.")
    md.append("- If the dataset was built by the authors or is synthetic, say so. A high R2 on data from one source measures "
              "how well the model learned that data, not accuracy on real trips.")
    md.append("- In the app the itemized formula gives the budget; the Random Forest estimate is an independent cross-check.")
    md.append("- Feature importance is impurity-based, which can favour columns with many values (such as Destination).")
    md.append("")
    facts["rf"] = (rmse, mae, r2, reproduced)
    md_charts.extend(["model_comparison.png", "budget_model_fit.png", "feature_importance.png"])


# ====================================================================== 2. A* vs Dijkstra
def section_astar(md, facts):
    from routing import router as R
    from routing.destinations import DESTINATIONS

    G = R.build_graph()
    names = sorted(G)
    xy = {n: (DESTINATIONS[n][0], DESTINATIONS[n][1]) for n in names}

    def search(start, goal, use_heuristic):
        def h(n):
            return R.haversine(xy[n], xy[goal]) if use_heuristic else 0.0
        g = {start: 0.0}
        queue = [(h(start), start)]
        closed, expanded = set(), 0
        while queue:
            _, cur = heapq.heappop(queue)
            if cur in closed:
                continue
            if cur == goal:
                return g[goal], expanded
            closed.add(cur)
            expanded += 1
            for nbr, w in G[cur].items():
                t = g[cur] + w
                if t < g.get(nbr, math.inf):
                    g[nbr] = t
                    heapq.heappush(queue, (t + h(nbr), nbr))
        return math.inf, expanded

    a_exp, d_exp, mismatches, pairs = [], [], 0, 0
    for s in names:
        for t in names:
            if s == t:
                continue
            pairs += 1
            ca, ea = search(s, t, True)
            cd, ed = search(s, t, False)
            _, c_router = R.astar(G, s, t)
            if abs(ca - cd) > 1e-6 or abs(ca - c_router) > 1e-6:
                mismatches += 1
            a_exp.append(ea)
            d_exp.append(ed)
    ma, md_ = float(np.mean(a_exp)), float(np.mean(d_exp))
    saving = 100 * (1 - ma / md_) if md_ else 0.0
    edges = sum(len(v) for v in G.values())

    fig, ax = plt.subplots(1, 2, figsize=(10, 4))
    ax[0].bar(["A* (haversine)", "Dijkstra"], [ma, md_], color=[BLUE, GREY])
    ax[0].set_ylabel("Nodes expanded per query (mean)")
    ax[0].set_title(f"{pairs} queries, {len(names)} nodes")
    for i, v in enumerate([ma, md_]):
        ax[0].text(i, v + 0.1, f"{v:.1f}", ha="center")
    ax[1].boxplot([a_exp, d_exp])
    ax[1].set_xticklabels(["A*", "Dijkstra"])
    ax[1].set_title("Spread of nodes expanded")
    fig.suptitle(f"A* found the same cost as Dijkstra in {pairs - mismatches}/{pairs} queries")
    save(fig, "astar_vs_dijkstra.png")

    md += ["## 2. Route search: A* vs Dijkstra", "",
           f"Graph: {len(names)} destinations, {edges} directed edges (each node linked to its nearest neighbours, "
           "weights = straight-line distance x curvature factor). Edge weights are never smaller than the straight-line "
           "distance, so the haversine heuristic is admissible.", ""]
    md += table(["Measure", "A*", "Dijkstra"], [
        ["Nodes expanded per query (mean)", f"{ma:.1f}", f"{md_:.1f}"],
        ["Queries", pairs, pairs],
        ["Queries where the cost equals the router's A*", f"{pairs - mismatches}", f"{pairs - mismatches}"]])
    md += ["", f"A* expanded {saving:.1f}% fewer nodes. With only {len(names)} nodes the saving is small in absolute terms; "
           "the point is that the result is identical and never worse.", ""]
    facts["astar"] = (saving, mismatches, pairs)
    md_charts.append("astar_vs_dijkstra.png")


# ====================================================================== 3. TSP benchmark
def section_tsp(md, facts):
    from routing import router as R
    from routing.destinations import DESTINATIONS

    names = sorted(DESTINATIONS)
    M = {a: {b: R.macro_distance(a, b) for b in names if b != a} for a in names}

    def length(path):
        return sum(M[path[i]][path[i + 1]] for i in range(len(path) - 1))

    rng = random.Random(7)
    sizes, trials = list(range(4, 10)), 20
    stats = {}
    for n in sizes:
        nn_gap, opt_gap, hit = [], [], 0
        for _ in range(trials):
            stops = rng.sample(names, n)
            start = stops[0]
            order, opt_len, nn_len = R.optimize_multi_stop(stops, start)
            best = min(length([start] + list(p)) for p in itertools.permutations(stops[1:]))
            nn_gap.append(100 * (nn_len - best) / best)
            opt_gap.append(100 * (opt_len - best) / best)
            hit += opt_gap[-1] < 1e-6
        stats[n] = (float(np.mean(nn_gap)), float(np.mean(opt_gap)), 100 * hit / trials, float(max(opt_gap)))

    fig, ax = plt.subplots(1, 2, figsize=(11, 4))
    ax[0].plot(sizes, [stats[n][0] for n in sizes], "o-", color=GREY, label="Nearest neighbour")
    ax[0].plot(sizes, [stats[n][1] for n in sizes], "o-", color=BLUE, label="Nearest neighbour + 2-opt")
    ax[0].set_xlabel("Number of stops")
    ax[0].set_ylabel("Mean excess length over the optimum (%)")
    ax[0].set_title("Route length vs the true optimum")
    ax[0].legend()
    ax[1].bar([str(n) for n in sizes], [stats[n][2] for n in sizes], color=BLUE)
    ax[1].set_ylim(0, 105)
    ax[1].set_xlabel("Number of stops")
    ax[1].set_ylabel("Trips where 2-opt found the optimum (%)")
    ax[1].set_title(f"{trials} random trips per size")
    save(fig, "tsp_benchmark.png")

    md += ["## 3. Multi-stop route order: nearest neighbour, 2-opt and the optimum", "",
           f"For each size, {trials} random sets of destinations (fixed seed). The optimum is found by trying every order "
           "with the starting stop fixed (the route is a path, and distances depend on direction).", ""]
    md += table(["Stops", "NN excess (mean %)", "NN + 2-opt excess (mean %)", "2-opt found the optimum", "Worst 2-opt excess (%)"],
                [[n, f"{stats[n][0]:.1f}", f"{stats[n][1]:.1f}", f"{stats[n][2]:.0f}%", f"{stats[n][3]:.1f}"] for n in sizes])
    md += ["", "2-opt never makes the nearest-neighbour route longer. Itineraries allow at most 10 stops, so exact search is "
           "possible for small trips; the heuristic is what keeps larger trips fast.", ""]
    facts["tsp"] = stats
    md_charts.append("tsp_benchmark.png")


# ====================================================================== 4. review safety
def section_safety(md, facts, use_model):
    from safety import classify_safety, find_hazards

    rows = json.loads((ML / "evaluation" / "review_testset.json").read_text(encoding="utf-8"))
    tp = fp = fn = tn = 0
    for r in rows:
        pred, gold = bool(find_hazards(r["text"])[0]), bool(r["hard"])
        tp += pred and gold
        fp += pred and not gold
        fn += gold and not pred
        tn += (not pred) and (not gold)
    prec = tp / (tp + fp) if tp + fp else 1.0
    rec = tp / (tp + fn) if tp + fn else 1.0
    md += ["## 4. Review safety", "", f"Test set: {len(rows)} reviews written by the project authors together with the rules, "
           "so the hazard result is a regression check, not an independent benchmark. The set is small; treat the "
           "percentages as indicative.", "", "**Hazard keyword rules (no model)**", ""]
    md += table(["Metric", "Value"], [["Precision", f"{100 * prec:.1f}%"], ["Recall", f"{100 * rec:.1f}%"],
                                      ["TP / FP / FN / TN", f"{tp} / {fp} / {fn} / {tn}"]])
    md.append("")

    panels = [(np.array([[tn, fp], [fn, tp]]), ["No hazard", "Hazard"], ["No hazard", "Hazard"], "Hazard rules")]
    sentiment = None
    if use_model:
        from safety import _get_pipe
        print("  loading the sentiment model ...")
        preds = _get_pipe()([r["text"] for r in rows], truncation=True, max_length=512)
        labelled = [(r, str(p["label"]).upper()) for r, p in zip(rows, preds) if r["sentiment"]]
        cm = np.zeros((2, 2), dtype=int)
        for r, got in labelled:
            cm[["POSITIVE", "NEGATIVE"].index(r["sentiment"]), ["POSITIVE", "NEGATIVE"].index(got)] += 1
        acc = 100 * (cm[0, 0] + cm[1, 1]) / max(cm.sum(), 1)
        flags = Counter()
        for r, p in zip(rows, preds):
            hard, soft = find_hazards(r["text"])
            flags[classify_safety(str(p["label"]).upper(), float(p["score"]), hard, soft)] += 1
        panels.append((cm, ["POSITIVE", "NEGATIVE"], ["POSITIVE", "NEGATIVE"], f"Sentiment (DistilBERT), accuracy {acc:.0f}%"))
        sentiment = (acc, len(labelled), len(rows) - len(labelled), dict(flags))
        md += ["**Sentiment model (DistilBERT SST-2)**", ""]
        md += table(["Metric", "Value"], [["Accuracy on clearly positive/negative reviews", f"{acc:.1f}% of {len(labelled)}"],
                                          ["Mixed reviews skipped", len(rows) - len(labelled)],
                                          ["Final safety flags over all reviews", dict(flags)]])
        md.append("")
    else:
        md += ["_Sentiment model part skipped (run without --no-model to include it)._", ""]

    fig, axes = plt.subplots(1, len(panels), figsize=(5.2 * len(panels), 4.4))
    axes = np.atleast_1d(axes)
    for ax, (m, rl, cl, title) in zip(axes, panels):
        confusion(ax, m, rl, cl, title)
    save(fig, "review_safety.png")
    facts["safety"] = (prec, rec, sentiment)
    md_charts.append("review_safety.png")


# ====================================================================== 5. speed
def section_speed(md, facts):
    import joblib
    import app as svc
    from routing import router as R
    from safety import find_hazards

    svc.model = joblib.load(svc.MODEL_PATH)
    G = R.build_graph()
    stops = ["Everest Base Camp", "Gokyo Lakes", "Langtang Valley", "Manaslu Circuit",
             "Annapurna Circuit", "Mardi Himal", "Upper Mustang"]

    def request(skip):
        return svc.BudgetRequest(
            starting_city="Pokhara", destination="Everest Base Camp", duration_days=12, group_size=4,
            travel_month="October", nationality_category="Foreign", traveler_type="Group",
            accommodation_tier="Standard", transport_mode="Flight", use_guide=True, use_porter=True, skip_ml=skip)

    def clock(fn, runs):
        fn()
        ms = []
        for _ in range(runs):
            a = time.perf_counter()
            fn()
            ms.append((time.perf_counter() - a) * 1000)
        return float(np.median(ms)), float(np.percentile(ms, 95)), runs

    jobs = [
        ("Budget, itemized formula only", lambda: svc.predict(request(True)), 300),
        ("Budget with the Random Forest cross-check", lambda: svc.predict(request(False)), 40),
        ("Route search (A*, one query)", lambda: R.astar(G, "Lumbini", "Upper Mustang"), 300),
        ("Route order for 7 stops (NN + 2-opt)", lambda: R.optimize_multi_stop(stops, stops[0]), 30),
        ("Hazard scan of one review", lambda: find_hazards("A landslide blocked the trail but the guide was great"), 1000),
    ]
    rows = []
    for label, fn, runs in jobs:
        med, p95, n = clock(fn, runs)
        rows.append([label, f"{med:.2f}", f"{p95:.2f}", n])
    md += ["## 5. Speed", "", "Measured by calling the functions directly on this computer, so no network or server time is "
           "included. Itinerary requests also wait for Gemini, which the app limits to 35 seconds before using a template plan.", ""]
    md += table(["Operation", "Median (ms)", "95th percentile (ms)", "Runs"], rows)
    md.append("")
    facts["speed"] = rows


# ====================================================================== main
md_charts = []


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--no-model", action="store_true", help="skip the DistilBERT sentiment part")
    args = ap.parse_args()

    import pandas
    import sklearn
    md = ["# TrailMind: evaluation evidence", "",
          f"Generated {date.today().isoformat()} by `ml-service/evaluation/make_evidence.py`. "
          f"Python {platform.python_version()}, numpy {np.__version__}, pandas {pandas.__version__}, "
          f"scikit-learn {sklearn.__version__}, matplotlib {matplotlib.__version__}. Processor: {platform.processor() or 'unknown'}.", ""]
    facts = {}
    print("1/5 budget model")
    section_budget(md, facts)
    print("2/5 A* vs Dijkstra")
    section_astar(md, facts)
    print("3/5 route order benchmark")
    section_tsp(md, facts)
    print("4/5 review safety")
    section_safety(md, facts, not args.no_model)
    print("5/5 speed")
    section_speed(md, facts)

    md += ["## Charts", ""] + [f"- ![{c}]({c})" for c in md_charts]
    md += ["", "## How to regenerate", "", "From the ml-service folder: `venv\\Scripts\\python.exe -m evaluation.make_evidence`", ""]
    (OUT / "RESULTS.md").write_text("\n".join(md) + "\n", encoding="utf-8")

    rmse, mae, r2, reproduced = facts["rf"]
    saving, mism, pairs = facts["astar"]
    prec, rec, sentiment = facts["safety"]
    print("\n==== SUMMARY (paste this if anything looks wrong) ====")
    print(f"Random Forest test: RMSE {rmse:,.0f}  MAE {mae:,.0f}  R2 {r2:.3f}  reproduces metrics.json: {reproduced}")
    print(f"A* vs Dijkstra: same cost in {pairs - mism}/{pairs} queries, {saving:.1f}% fewer nodes expanded")
    print("2-opt found the optimum (% of trips): " + ", ".join(f"{n} stops {facts['tsp'][n][2]:.0f}%" for n in sorted(facts["tsp"])))
    print(f"Hazard rules: precision {100 * prec:.1f}%  recall {100 * rec:.1f}%")
    if sentiment:
        print(f"Sentiment accuracy {sentiment[0]:.1f}% on {sentiment[1]} clear reviews")
    print("Speed (median ms): " + "; ".join(f"{r[0]} {r[1]}" for r in facts["speed"]))
    print("Files are in", OUT)


if __name__ == "__main__":
    main()