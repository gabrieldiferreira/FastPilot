#!/usr/bin/env python3
"""
Checks FastPilot's lap data against Jolpica (successor of the Ergast API), an independent
source of official lap times, on the five comparison races and on two hold-out races that
took no part in tuning the lap filters (TCC II).

Since 15 Oct 2026 the F1 live timing archive used by FastF1 requires an F1 TV account, so the
hold-out races could not get a FastF1 reference. Jolpica only publishes lap times (no sectors,
safety car periods or telemetry), so this covers:
  1. every lap time: OpenF1 lap_duration (what the app reads) vs the Jolpica lap time;
  2. best lap: FastPilot's best_lap (metrics/comparison/fastpilot/<race>.json, exported by
     ./gradlew :shared:exportMetrics) vs the driver's fastest Jolpica lap.
Clean pace, tyre degradation and lap delta depend on data Jolpica does not have and are not checked.

Lap 1 is reported apart: Jolpica times it from the race start, OpenF1 from the line crossing
(and often leaves it empty).

Inputs: fixtures/<race>/openf1/laps.json, fixtures/<race>/jolpica/{laps,results}.json.
Usage: python3 scripts/jolpica_check.py
Outputs: metrics/comparison/jolpica_check.md and errors_jolpica.csv.
"""
import csv
import json
from pathlib import Path
from statistics import mean

ROOT = Path(__file__).resolve().parent.parent
FIXTURES = ROOT / "fixtures"
BASE = ROOT / "metrics" / "comparison"
TUNING = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
HOLDOUT = ["2023_japan", "2024_las_vegas"]  # see HOLDOUT_RACES in scripts/fetch_fixtures.py
TOLERANCE = 0.0005  # s: Jolpica and OpenF1 both give lap times in milliseconds


def seconds(text):
    """'1:34.183' or '58.901' -> seconds."""
    minutes, _, rest = text.rpartition(":")
    return (int(minutes) * 60 if minutes else 0) + float(rest)


def jolpica_laps(race):
    """{(driver number, lap): seconds}, driver numbers taken from results.json."""
    results = json.loads((FIXTURES / race / "jolpica" / "results.json").read_text())
    numbers = {r["Driver"]["driverId"]: int(r["number"])
               for r in results["MRData"]["RaceTable"]["Races"][0]["Results"]}
    laps = json.loads((FIXTURES / race / "jolpica" / "laps.json").read_text())
    return {(numbers[t["driverId"]], int(lap["number"])): seconds(t["time"])
            for lap in laps["MRData"]["RaceTable"]["Races"][0]["Laps"] for t in lap["Timings"]}


def fmt(value, digits=3):
    return f"{value:.{digits}f}".replace(".", ",")


def check(race):
    official = jolpica_laps(race)
    openf1 = {(l["driver_number"], l["lap_number"]): l["lap_duration"]
              for l in json.loads((FIXTURES / race / "openf1" / "laps.json").read_text())}
    rows = []
    for key, ref in sorted(official.items()):
        value = openf1.get(key)
        rows.append((race, "lap_time", f"#{key[0]} L{key[1]}", value, ref,
                     None if value is None else abs(value - ref)))
    fastpilot = json.loads((BASE / "fastpilot" / f"{race}.json").read_text())["drivers"]
    for driver, metrics in sorted(fastpilot.items(), key=lambda d: int(d[0])):
        times = [t for (n, _), t in official.items() if n == int(driver)]
        if times and metrics.get("best_lap") is not None:
            ref = min(times)
            rows.append((race, "best_lap", f"#{driver}", metrics["best_lap"], ref, abs(metrics["best_lap"] - ref)))
    return rows


def lap_stats(rows, first_lap):
    laps = [r for r in rows if r[1] == "lap_time" and r[2].endswith(" L1") == first_lap]
    compared = [r for r in laps if r[5] is not None]
    equal = sum(1 for r in compared if r[5] <= TOLERANCE)
    return len(laps), len(compared), equal, (mean(r[5] for r in compared) if compared else None), \
        (max(r[5] for r in compared) if compared else None)


def main():
    rows = [row for race in TUNING + HOLDOUT for row in check(race)]
    with open(BASE / "errors_jolpica.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["race", "metric", "item", "openf1_or_fastpilot", "jolpica", "abs_error"])
        for row in rows:
            w.writerow([round(x, 4) if isinstance(x, float) else x for x in row])

    lines = [
        "# Verificação com a Jolpica (tempos oficiais de volta)",
        "",
        "Gerado por `python3 scripts/jolpica_check.py`. Fonte independente: Jolpica (sucessora da Ergast), "
        "respostas gravadas em `fixtures/<corrida>/jolpica/`. Detalhe por volta e por piloto em `errors_jolpica.csv`.",
        "",
        f"Corridas dos ajustes dos filtros: {', '.join(TUNING)}. Corridas de validação (fora dos ajustes, sem "
        f"referência FastF1 por causa da restrição de acesso de 15/10/2026): {', '.join(HOLDOUT)}.",
        "",
        f"Iguais = diferença ≤ {fmt(TOLERANCE, 4)} s (as duas fontes dão o tempo em milésimos). "
        "Ritmo limpo, degradação e delta não são verificados aqui: dependem de setores, períodos de safety car "
        "e telemetria, que a Jolpica não tem.",
        "",
        "## Tempo de cada volta (OpenF1 × Jolpica), sem a volta 1",
        "",
        "| Corrida | Grupo | Voltas na Jolpica | Com tempo na OpenF1 | Iguais | MAE (s) | Maior diferença (s) |",
        "|---|---|---:|---:|---:|---:|---:|",
    ]
    for race in TUNING + HOLDOUT:
        n, compared, equal, mae, worst = lap_stats([r for r in rows if r[0] == race], first_lap=False)
        group = "ajuste" if race in TUNING else "**validação**"
        lines.append(f"| {race} | {group} | {n} | {compared} | {equal} | {fmt(mae, 4)} | {fmt(worst)} |")
    for name, races in (("Ajuste (5)", TUNING), ("Validação (2)", HOLDOUT)):
        n, compared, equal, mae, worst = lap_stats([r for r in rows if r[0] in races], first_lap=False)
        lines.append(f"| **{name}** | | {n} | {compared} | {equal} | {fmt(mae, 4)} | {fmt(worst)} |")

    lines += ["", "Volta 1 (definições diferentes: a Jolpica mede desde a largada):", "",
              "| Grupo | Voltas 1 na Jolpica | Com tempo na OpenF1 | Iguais |", "|---|---:|---:|---:|"]
    for name, races in (("Ajuste (5)", TUNING), ("Validação (2)", HOLDOUT)):
        n, compared, equal, _, _ = lap_stats([r for r in rows if r[0] in races], first_lap=True)
        lines.append(f"| {name} | {n} | {compared} | {equal} |")

    lines += ["", "## Melhor volta (FastPilot × menor tempo na Jolpica)", "",
              "| Corrida | Grupo | Pilotos | Iguais | MAE (s) | Maior diferença (s) |", "|---|---|---:|---:|---:|---:|"]
    for race in TUNING + HOLDOUT:
        best = [r for r in rows if r[0] == race and r[1] == "best_lap"]
        group = "ajuste" if race in TUNING else "**validação**"
        lines.append(f"| {race} | {group} | {len(best)} | {sum(1 for r in best if r[5] <= TOLERANCE)} | "
                     f"{fmt(mean(r[5] for r in best), 4)} | {fmt(max(r[5] for r in best))} |")
    lines.append("")
    different = [r for r in rows if r[5] is not None and r[5] > TOLERANCE and not r[2].endswith(" L1")]
    if different:
        lines += ["## Diferenças acima da tolerância (sem a volta 1)", "",
                  "| Corrida | Medida | Item | OpenF1/FastPilot (s) | Jolpica (s) | Diferença (s) |",
                  "|---|---|---|---:|---:|---:|"]
        for r in different:
            lines.append(f"| {r[0]} | {r[1]} | {r[2]} | {fmt(r[3])} | {fmt(r[4])} | {fmt(r[5])} |")
        lines.append("")
    report = "\n".join(lines)
    (BASE / "jolpica_check.md").write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
