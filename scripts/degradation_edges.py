#!/usr/bin/env python3
"""TCC II: does the tyre degradation regression fit the middle of a stint better than its edges?

TCC I §4.7 states that the linear regression of lap time on tyre age "works better in the
middle of the stint than in the first and last laps". This script measures it on the five
races of the FastF1 comparison, using the laps the FastPilot regression actually used
(exported by `./gradlew :shared:exportMetrics` in metrics/comparison/fastpilot/<race>.json).

For every stint with at least MIN_POINTS laps, each lap gets the residual
    lap_time - (slope * tyre_age + intercept)
and is classified by its place in the stint (lap_start..lap_end):
    start  = first EDGE_LAPS laps of the stint,
    end    = last EDGE_LAPS laps,
    middle = the others.
The error of a group is the mean absolute residual (MAE, seconds); the bias is the mean
signed residual (positive = slower than the line).

Usage: python3 scripts/degradation_edges.py [--out metrics/comparison/degradation_edges.md]
"""

import argparse
import json
from pathlib import Path
from statistics import mean, median

RACES = ["2023_bahrain", "2023_belgium", "2023_singapore", "2024_monaco", "2024_brazil"]
EDGE_LAPS = 2
MIN_POINTS = 8  # 2 + 2 edge laps and at least 4 in the middle
GROUPS = ["start", "middle", "end"]
GROUP_NAMES = {"start": "Início (2 primeiras)", "middle": "Meio", "end": "Fim (2 últimas)"}


def residuals(stint):
    first, last = stint["lap_start"], stint["lap_end"]
    for point in stint["points"]:
        fitted = stint["slope"] * point["tyre_age"] + stint["intercept"]
        if point["lap"] - first < EDGE_LAPS:
            group = "start"
        elif last - point["lap"] < EDGE_LAPS:
            group = "end"
        else:
            group = "middle"
        yield group, point["lap_time"] - fitted


def fmt(value, decimals=3):
    return f"{value:.{decimals}f}".replace(".", ",")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default="metrics/comparison/fastpilot")
    parser.add_argument("--out", default="metrics/comparison/degradation_edges.md")
    args = parser.parse_args()

    per_race = {}
    all_res = {g: [] for g in GROUPS}
    stint_count = 0
    worse_edges = 0  # stints whose edge MAE is above their middle MAE
    for race in RACES:
        data = json.loads((Path(args.data) / f"{race}.json").read_text())
        race_res = {g: [] for g in GROUPS}
        for driver in data["drivers"].values():
            for stint in driver.get("stints", []):
                if "points" not in stint or len(stint["points"]) < MIN_POINTS:
                    continue
                stint_res = {g: [] for g in GROUPS}
                for group, r in residuals(stint):
                    stint_res[group].append(r)
                if not stint_res["middle"] or not (stint_res["start"] or stint_res["end"]):
                    continue
                stint_count += 1
                edge = [abs(r) for r in stint_res["start"] + stint_res["end"]]
                if mean(edge) > mean(abs(r) for r in stint_res["middle"]):
                    worse_edges += 1
                for g in GROUPS:
                    race_res[g] += stint_res[g]
                    all_res[g] += stint_res[g]
        per_race[race] = race_res

    lines = [
        "# Degradação de pneu: meio do stint × bordas",
        "",
        "Gerado por `python3 scripts/degradation_edges.py` a partir de `metrics/comparison/fastpilot/` "
        "(`./gradlew :shared:exportMetrics`). Verifica a afirmação do TCC I (§4.7) de que a regressão linear "
        "do tempo de volta pela idade do pneu funciona melhor no meio do stint que nas primeiras e últimas voltas.",
        "",
        f"Método: stints com pelo menos {MIN_POINTS} voltas válidas (as mesmas que a regressão do FastPilot usa: "
        "sem volta de saída/entrada do box, safety car, VSC ou bandeira vermelha). Resíduo de cada volta = "
        "tempo real − reta ajustada. Grupos pela posição da volta no stint: início = "
        f"{EDGE_LAPS} primeiras, fim = {EDGE_LAPS} últimas, meio = as demais. "
        "Erro = média do resíduo absoluto (s); viés = média do resíduo com sinal (positivo = mais lento que a reta).",
        "",
        f"Stints analisados: **{stint_count}**; em **{worse_edges}** ({100 * worse_edges / stint_count:.0f}%) "
        "o erro médio das bordas é maior que o do meio." if stint_count else "Nenhum stint.",
        "",
        "## Todas as corridas",
        "",
        "| Grupo | Voltas | Erro médio (s) | Erro mediano (s) | Viés (s) |",
        "|---|---:|---:|---:|---:|",
    ]
    for g in GROUPS:
        res = all_res[g]
        lines.append(f"| {GROUP_NAMES[g]} | {len(res)} | {fmt(mean(abs(r) for r in res))} | "
                     f"{fmt(median(abs(r) for r in res))} | {fmt(mean(res))} |")
    lines += ["", "## Por corrida (erro médio, s)", "",
              "| Corrida | Início | Meio | Fim | Bordas ÷ meio |", "|---|---:|---:|---:|---:|"]
    for race, res in per_race.items():
        if not res["middle"]:
            continue
        mae = {g: mean(abs(r) for r in res[g]) for g in GROUPS if res[g]}
        edges = mean(abs(r) for r in res["start"] + res["end"])
        cell = {g: fmt(mae[g]) if g in mae else "—" for g in GROUPS}  # — = no lap in that group
        lines.append(f"| {race} | {cell['start']} | {cell['middle']} | {cell['end']} | "
                     f"{fmt(edges / mae['middle'], 2)}× |")
    lines.append("")

    Path(args.out).write_text("\n".join(lines), encoding="utf-8")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
