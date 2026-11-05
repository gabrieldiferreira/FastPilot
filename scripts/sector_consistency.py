#!/usr/bin/env python3
"""
Checks the sector times of the recorded OpenF1 laps and explains the theoretical best lap
differences between FastPilot and FastF1 (TCC II).

The theoretical best (min S1 + min S2 + min S3 over the laps that are not pit-out laps and have all
three sectors) is computed the same way by FastPilot and by scripts/fastf1_reference.py, so a
difference can only come from the sector data. Two OpenF1 problems are looked for:
  - a lap with a lap time but a missing sector: the whole lap is left out, with its other sectors;
  - a lap whose S1 + S2 + S3 does not match its lap time (more than TOLERANCE): one sector is wrong.
For every driver whose theoretical best differs from FastF1 (errors.csv and errors_holdout.csv),
the report lists those laps of the driver and whether they hold one of the driver's best sectors.

Inputs: fixtures/<race>/openf1/laps.json, metrics/comparison/errors.csv, errors_holdout.csv.
Usage: python3 scripts/sector_consistency.py
Outputs: metrics/comparison/sector_consistency.md and sector_consistency.csv.
"""
import csv
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FIXTURES = ROOT / "fixtures"
BASE = ROOT / "metrics" / "comparison"
TUNING = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
HOLDOUT = ["2023_japan", "2024_las_vegas"]  # see HOLDOUT_RACES in scripts/fetch_fixtures.py
TOLERANCE = 0.01  # s; sectors and lap times are given in milliseconds
DIFFERENCE = 0.0005  # s; theoretical best differences below this are rounding


def fmt(value, digits=3):
    return f"{value:.{digits}f}".replace(".", ",")


def sectors(lap):
    return [lap.get(f"duration_sector_{k}") for k in (1, 2, 3)]


def problems(race):
    """(race, driver, lap, kind, S1, S2, S3, lap time, sum − lap time) for every suspicious lap."""
    rows = []
    for lap in json.loads((FIXTURES / race / "openf1" / "laps.json").read_text()):
        s, duration = sectors(lap), lap["lap_duration"]
        if duration is None or lap["is_pit_out_lap"]:
            continue
        if None in s:
            if any(x is not None for x in s):
                rows.append((race, lap["driver_number"], lap["lap_number"], "setor ausente", *s, duration, None))
        elif abs(sum(s) - duration) > TOLERANCE:
            rows.append((race, lap["driver_number"], lap["lap_number"], "soma ≠ tempo da volta", *s, duration,
                         sum(s) - duration))
    return rows


def best_sectors(race, driver):
    """[(value, lap)] of the driver's best S1, S2 and S3, with the theoretical best rule."""
    laps = [l for l in json.loads((FIXTURES / race / "openf1" / "laps.json").read_text())
            if l["driver_number"] == driver and not l["is_pit_out_lap"] and None not in sectors(l)]
    return [min((sectors(l)[k], l["lap_number"]) for l in laps) for k in range(3)]


def differing_drivers():
    out = []
    for name in ("errors.csv", "errors_holdout.csv"):
        path = BASE / name
        if not path.exists():
            continue
        for row in csv.DictReader(open(path, encoding="utf-8")):
            if row["metric"] == "theoretical_best" and float(row["abs_error"]) > DIFFERENCE:
                out.append((row["race"], int(row["item"].lstrip("#")), float(row["fastpilot"]), float(row["fastf1"])))
    return out


def main():
    races = TUNING + HOLDOUT
    rows = [r for race in races for r in problems(race)]
    with open(BASE / "sector_consistency.csv", "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["race", "driver", "lap", "problem", "s1", "s2", "s3", "lap_duration", "sum_minus_lap"])
        for r in rows:
            w.writerow([round(x, 3) if isinstance(x, float) else x for x in r])

    lines = [
        "# Consistência dos setores da OpenF1",
        "",
        "Gerado por `python3 scripts/sector_consistency.py` a partir de `fixtures/<corrida>/openf1/laps.json`. "
        "Voltas consideradas: com tempo de volta e que não são de saída do box (as que a volta teórica pode usar). "
        f"Soma ≠ tempo da volta = |S1 + S2 + S3 − tempo da volta| > {fmt(TOLERANCE, 2)} s. Detalhe por volta em "
        "`sector_consistency.csv`.",
        "",
        "## Voltas com problema nos setores",
        "",
        "| Corrida | Grupo | Voltas verificadas | Setor ausente | Soma ≠ tempo da volta |",
        "|---|---|---:|---:|---:|",
    ]
    for race in races:
        checked = sum(1 for l in json.loads((FIXTURES / race / "openf1" / "laps.json").read_text())
                      if l["lap_duration"] is not None and not l["is_pit_out_lap"])
        missing = sum(1 for r in rows if r[0] == race and r[3] == "setor ausente")
        wrong = sum(1 for r in rows if r[0] == race and r[3] != "setor ausente")
        group = "ajuste" if race in TUNING else "validação"
        lines.append(f"| {race} | {group} | {checked} | {missing} | {wrong} |")

    lines += ["", "## Diferenças da volta teórica em relação à FastF1", "",
              f"Pilotos com diferença acima de {fmt(DIFFERENCE, 4)} s em `errors.csv` ou `errors_holdout.csv`, "
              "com as voltas problemáticas do piloto. Marcadas com **melhor** as que fornecem um dos melhores "
              "setores do piloto no FastPilot.", ""]
    for race, driver, fp, ff in differing_drivers():
        best = best_sectors(race, driver)
        best_laps = {lap for _, lap in best}
        lines.append(f"### {race}, #{driver}: FastPilot {fmt(fp)} s × FastF1 {fmt(ff)} s "
                     f"(diferença {fmt(fp - ff)} s)")
        lines.append("")
        lines.append("Melhores setores no FastPilot: " + "; ".join(
            f"S{k + 1} {fmt(v)} s (volta {lap})" for k, (v, lap) in enumerate(best)) + ".")
        lines.append("")
        if 1 in best_laps:
            lines += ["Um dos melhores setores vem da volta 1. A cronometragem oficial não tem o S1 da volta 1 "
                      "(largada do grid), por isso a FastF1 não usa essa volta; a OpenF1 tem os três setores.", ""]
        mine = [r for r in rows if r[0] == race and r[1] == driver]
        if not mine:
            lines += ["Nenhuma volta com setor ausente ou soma inconsistente.", ""]
            continue
        lines += ["| Volta | Problema | S1 | S2 | S3 | Tempo da volta | Soma − tempo |", "|---:|---|---:|---:|---:|---:|---:|"]
        for r in mine:
            cells = ["—" if x is None else fmt(x) for x in r[4:9]]
            tag = " (**melhor**)" if r[2] in best_laps else ""
            lines.append(f"| {r[2]}{tag} | {r[3]} | " + " | ".join(cells) + " |")
        lines.append("")
    report = "\n".join(lines)
    (BASE / "sector_consistency.md").write_text(report, encoding="utf-8")
    print(report)


if __name__ == "__main__":
    main()
