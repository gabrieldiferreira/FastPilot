#!/usr/bin/env python3
"""
Compares FastPilot's metrics with the FastF1 reference (TCC II, Fase 4).

Inputs:  metrics/comparison/fastpilot/<race>.json   (./gradlew :shared:exportMetrics)
         metrics/comparison/fastf1/<race>.json      (scripts/fastf1_reference.py)
Outputs: metrics/comparison/README.md (tables) and metrics/comparison/errors.csv (one row per compared value)

Error measures:
- MAPE (mean absolute percentage error, target <= 5%, TCC I §4.8) for values measured in seconds
  of lap time: clean pace (mean and median), theoretical best, best lap.
- Degradation slope and lap delta oscillate around zero, where percentage errors are unstable
  (division by ~0), so they are also reported as absolute errors in seconds (MAE).
- Lap delta curves are compared on a grid of 201 points of normalised lap distance (0..1).

Stints are paired by driver, compound and the largest overlap of their lap ranges (OpenF1 and
FastF1 number stints differently in some races).

Usage: python3 scripts/compare_metrics.py [--label NAME] [--fastpilot DIR] [--out NAME] [--holdout]
  e.g. --fastpilot metrics/comparison/fastpilot_baseline --out baseline
"""
import csv
import json
import sys
from pathlib import Path
from statistics import mean, median

ROOT = Path(__file__).resolve().parent.parent
BASE = ROOT / "metrics" / "comparison"
RACES = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
# Hold-out races, not used to tune the lap filters (see scripts/fetch_fixtures.py)
HOLDOUT_RACES = ["2023_japan", "2024_las_vegas"]
if "--holdout" in sys.argv:
    RACES = HOLDOUT_RACES
SCALAR_METRICS = [
    ("clean_pace_mean", "Ritmo limpo (média)"),
    ("clean_pace_median", "Ritmo limpo (mediana)"),
    ("theoretical_best", "Volta teórica"),
    ("best_lap", "Melhor volta"),
]
GRID = [i / 200 for i in range(201)]


def ape(fp, ref):
    return abs(fp - ref) / abs(ref) * 100


def interp(x, xs, ys):
    """Linear interpolation, xs ascending."""
    if x <= xs[0]:
        return ys[0]
    if x >= xs[-1]:
        return ys[-1]
    lo, hi = 0, len(xs) - 1
    while hi - lo > 1:
        mid = (lo + hi) // 2
        if xs[mid] <= x:
            lo = mid
        else:
            hi = mid
    span = xs[hi] - xs[lo]
    return ys[lo] if span == 0 else ys[lo] + (ys[hi] - ys[lo]) * (x - xs[lo]) / span


def fmt(value, digits=2, suffix=""):
    return "—" if value is None else f"{value:.{digits}f}{suffix}"


def arg(name, default):
    return sys.argv[sys.argv.index(name) + 1] if name in sys.argv else default


def matching_stint(stint, candidates):
    """Reference stint with the same compound and the largest lap overlap (at least half of the stint)."""
    best, best_overlap = None, 0
    for c in candidates:
        if c["compound"] != stint["compound"]:
            continue
        overlap = min(stint["lap_end"], c["lap_end"]) - max(stint["lap_start"], c["lap_start"]) + 1
        if overlap > best_overlap:
            best, best_overlap = c, overlap
    length = stint["lap_end"] - stint["lap_start"] + 1
    return best if best_overlap * 2 >= length else None


def main():
    label = arg("--label", "atual")
    fastpilot_dir = ROOT / arg("--fastpilot", "metrics/comparison/fastpilot")
    out_name = arg("--out", "")
    rows = []  # (race, metric, item, fastpilot, fastf1, abs_error, ape)

    for race in RACES:
        fp = json.loads((fastpilot_dir / f"{race}.json").read_text())
        ff = json.loads((BASE / "fastf1" / f"{race}.json").read_text())

        for driver in sorted(set(fp["drivers"]) & set(ff["drivers"]), key=int):
            a, b = fp["drivers"][driver], ff["drivers"][driver]
            for key, _ in SCALAR_METRICS:
                if a.get(key) is not None and b.get(key) is not None:
                    rows.append((race, key, f"#{driver}", a[key], b[key], abs(a[key] - b[key]), ape(a[key], b[key])))
            for s in a["stints"]:
                r = matching_stint(s, b["stints"])
                if r:
                    rows.append((race, "degradation_slope", f"#{driver} stint {s['stint']} {s['compound']}",
                                 s["slope"], r["slope"], abs(s["slope"] - r["slope"]),
                                 ape(s["slope"], r["slope"]) if r["slope"] != 0 else None))

        ref_deltas = {(d["reference"], d["compared"]): d for d in ff["lap_deltas"]}
        for d in fp["lap_deltas"]:
            r = ref_deltas.get((d["reference"], d["compared"]))
            if not r:
                continue
            item = f"#{d['reference']} L{d['reference_lap']} vs #{d['compared']} L{d['compared_lap']}"
            if (r["reference_lap"], r["compared_lap"]) != (d["reference_lap"], d["compared_lap"]):
                item += f" (FastF1: L{r['reference_lap']} vs L{r['compared_lap']})"
            errors = [abs(interp(g, d["fraction"], d["delta"]) - interp(g, r["fraction"], r["delta"])) for g in GRID]
            rows.append((race, "lap_delta_curve", item, None, None, mean(errors), None))
            rows.append((race, "lap_delta_final", item, d["official_gap"], r["delta"][-1],
                         abs(d["official_gap"] - r["delta"][-1]), None))

    suffix = f"_{out_name}" if out_name else ""
    with open(BASE / f"errors{suffix}.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["race", "metric", "item", "fastpilot", "fastf1", "abs_error", "ape_percent"])
        for row in rows:
            w.writerow([x if not isinstance(x, float) else round(x, 5) for x in row])

    def select(metric, race=None):
        return [r for r in rows if r[1] == metric and (race is None or r[0] == race)]

    lines = [
        "# Comparação FastPilot × FastF1",
        "",
        f"Execução: **{label}**. Gerado por `scripts/compare_metrics.py` a partir de",
        f"`{fastpilot_dir.relative_to(ROOT)}/` (`./gradlew :shared:exportMetrics`) e `metrics/comparison/fastf1/`",
        f"(`scripts/fastf1_reference.py`). Detalhe por item em `errors{suffix}.csv`.",
        "",
        "## Métricas em segundos de volta (MAPE, meta ≤ 5%)",
        "",
        "| Métrica | " + " | ".join(RACES) + " | Geral | MAE geral (s) | n |",
        "|---|" + "---:|" * (len(RACES) + 3),
    ]
    for key, name in SCALAR_METRICS:
        per_race = [select(key, race) for race in RACES]
        all_rows = select(key)
        cells = [fmt(mean(r[6] for r in pr), 2, "%") if pr else "—" for pr in per_race]
        lines.append(f"| {name} | " + " | ".join(cells) +
                     f" | **{fmt(mean(r[6] for r in all_rows), 2, '%')}** | {fmt(mean(r[5] for r in all_rows), 3)} | {len(all_rows)} |")

    deg = select("degradation_slope")
    deg_ape = [r[6] for r in deg if r[6] is not None]
    same_sign = sum(1 for r in deg if (r[3] > 0) == (r[4] > 0))
    lines += [
        "",
        "## Degradação de pneu (inclinação em s/volta por stint)",
        "",
        "| Corrida | Stints comparados | MAE (s/volta) | Mediana do erro (s/volta) | MAPE | Mesmo sinal |",
        "|---|---:|---:|---:|---:|---:|",
    ]
    for race in RACES:
        rr = select("degradation_slope", race)
        if rr:
            lines.append(f"| {race} | {len(rr)} | {fmt(mean(r[5] for r in rr), 3)} | {fmt(median(r[5] for r in rr), 3)} | "
                         f"{fmt(mean(r[6] for r in rr if r[6] is not None), 1, '%')} | "
                         f"{sum(1 for r in rr if (r[3] > 0) == (r[4] > 0))}/{len(rr)} |")
    lines.append(f"| **Geral** | {len(deg)} | **{fmt(mean(r[5] for r in deg), 3)}** | {fmt(median(r[5] for r in deg), 3)} | "
                 f"{fmt(mean(deg_ape), 1, '%')} | {same_sign}/{len(deg)} |")

    lines += [
        "",
        "## Delta na volta (§4.7)",
        "",
        "Erro médio absoluto entre as curvas em 201 pontos da distância normalizada, e diferença no valor final.",
        "",
        "| Corrida | Par | MAE da curva (s) | Gap final FastPilot (s) | Gap final FastF1 (s) |",
        "|---|---|---:|---:|---:|",
    ]
    for race in RACES:
        finals = {r[2]: r for r in select("lap_delta_final", race)}
        for r in select("lap_delta_curve", race):
            f = finals[r[2]]
            lines.append(f"| {race} | {r[2]} | {fmt(r[5], 3)} | {fmt(f[3], 3)} | {fmt(f[4], 3)} |")
    curve = select("lap_delta_curve")
    lines.append(f"| **Geral** | {len(curve)} pares | **{fmt(mean(r[5] for r in curve), 3)}** | | |")
    lines.append("")

    report = "\n".join(lines)
    (BASE / (f"{out_name}.md" if out_name else "README.md")).write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
