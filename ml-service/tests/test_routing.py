"""B9-1: graph, A*, nearest neighbour, 2-opt and journey distance.
A* is checked against an independent Dijkstra; the route optimiser against brute force."""
import heapq
import itertools
import math

import pytest

from routing import router as R
from routing.destinations import DESTINATIONS, GAMMA

NAMES = sorted(DESTINATIONS)
GRAPH = R.build_graph()

SETS = [
    ("Pashupatinath",
     ["Pashupatinath", "Boudhanath", "Swayambhunath", "Bhaktapur Durbar Square", "Nagarkot"]),
    ("Chitwan National Park",
     ["Chitwan National Park", "Lumbini", "Bardia National Park", "Janakpur (Janaki Mandir)",
      "Bandipur Trails", "Nagarkot"]),
    ("Everest Base Camp",
     ["Everest Base Camp", "Gokyo Lakes", "Langtang Valley", "Manaslu Circuit",
      "Annapurna Circuit", "Mardi Himal", "Upper Mustang"]),
]


def dijkstra(graph, start):
    dist = {start: 0.0}
    queue = [(0.0, start)]
    while queue:
        d, u = heapq.heappop(queue)
        if d > dist.get(u, math.inf):
            continue
        for v, w in graph[u].items():
            nd = d + w
            if nd < dist.get(v, math.inf):
                dist[v] = nd
                heapq.heappush(queue, (nd, v))
    return dist


# ------------------------------------------------------------------ distance basics
def test_haversine_basics():
    kathmandu, pokhara = (27.7172, 85.3240), (28.2096, 83.9856)
    assert R.haversine(kathmandu, kathmandu) == 0
    assert R.haversine(kathmandu, pokhara) == pytest.approx(R.haversine(pokhara, kathmandu))
    assert 139 <= R.haversine(kathmandu, pokhara) <= 146


def test_macro_distance_is_straight_line_times_the_origin_curvature():
    for a, b in [("Pashupatinath", "Lumbini"), ("Everest Base Camp", "Gokyo Lakes"), ("Chitwan National Park", "Nagarkot")]:
        o, t = DESTINATIONS[a], DESTINATIONS[b]
        straight = R.haversine((o[0], o[1]), (t[0], t[1]))
        assert R.macro_distance(a, b) == pytest.approx(straight * GAMMA[o[2]])
        assert R.macro_distance(a, b) >= straight


def test_curvature_factors_are_sensible():
    assert all(1.0 < g <= 2.0 for g in GAMMA.values())


# ------------------------------------------------------------------ graph
def test_graph_has_every_destination_and_positive_edges():
    assert set(GRAPH) == set(NAMES)
    for a, nbrs in GRAPH.items():
        assert nbrs, a
        assert a not in nbrs
        assert all(w > 0 for w in nbrs.values())


def test_graph_edges_exist_in_both_directions():
    for a, nbrs in GRAPH.items():
        for b in nbrs:
            assert a in GRAPH[b], (a, b)


def test_graph_is_connected():
    seen, stack = {NAMES[0]}, [NAMES[0]]
    while stack:
        for v in GRAPH[stack.pop()]:
            if v not in seen:
                seen.add(v)
                stack.append(v)
    assert seen == set(NAMES)


# ------------------------------------------------------------------ A*
def test_astar_to_itself_is_free():
    path, cost = R.astar(GRAPH, "Lumbini", "Lumbini")
    assert path == ["Lumbini"]
    assert cost == 0


def test_astar_matches_dijkstra_for_every_pair():
    for start in NAMES:
        best = dijkstra(GRAPH, start)
        for goal in NAMES:
            path, cost = R.astar(GRAPH, start, goal)
            assert path is not None, (start, goal)
            assert path[0] == start and path[-1] == goal, (start, goal)
            assert cost == pytest.approx(best[goal], abs=1e-6), (start, goal)
            walked = sum(GRAPH[a][b] for a, b in zip(path, path[1:]))
            assert walked == pytest.approx(cost, abs=1e-6), (start, goal)


# ------------------------------------------------------------------ multi-stop route
@pytest.mark.parametrize("start,stops", SETS)
def test_nearest_neighbour_visits_every_stop_once(start, stops):
    tour = R.nearest_neighbour(stops, start)
    assert tour[0] == start
    assert sorted(tour) == sorted(stops)


@pytest.mark.parametrize("start,stops", SETS)
def test_two_opt_never_makes_the_tour_longer_and_is_stable(start, stops):
    nn = R.nearest_neighbour(stops, start)
    better = R.two_opt(nn)
    assert better[0] == start
    assert sorted(better) == sorted(stops)
    assert R._tour_length(better) <= R._tour_length(nn) + 1e-9
    assert R.two_opt(better) == better


@pytest.mark.parametrize("start,stops", SETS)
def test_route_optimiser_against_brute_force(start, stops):
    order, opt_len, nn_len = R.optimize_multi_stop(stops, start)
    assert sorted(order) == sorted(stops)
    assert order[0] == start
    assert opt_len == pytest.approx(R._tour_length(order), abs=1e-6)
    assert opt_len <= nn_len + 1e-9
    rest = [s for s in stops if s != start]
    best = min(R._tour_length([start] + list(p)) for p in itertools.permutations(rest))
    assert best <= opt_len + 1e-9                  # nothing beats the true optimum
    assert opt_len <= best * 1.35                  # and the heuristic stays close to it


def test_one_or_two_stops_are_left_alone():
    assert R.optimize_multi_stop(["Lumbini"])[0] == ["Lumbini"]
    assert R.optimize_multi_stop(["Lumbini", "Pashupatinath"])[0] == ["Lumbini", "Pashupatinath"]


# ------------------------------------------------------------------ journey distance
def test_every_stop_has_a_sensible_micro_distance():
    for name in NAMES:
        km = R.micro_distance(name)
        assert 0.05 <= km <= 20, (name, km)
        assert R.micro_distance(name) == km        # same answer every time


def test_journey_distance_adds_up():
    order = SETS[0][1]
    d = R.journey_distance(order)
    assert d["macro_km"] == pytest.approx(R._tour_length(order), abs=0.06)
    assert d["total_km"] == pytest.approx(d["macro_km"] + d["micro_km"], abs=0.11)
    assert d["micro_km"] > 0