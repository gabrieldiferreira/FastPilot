#!/usr/bin/env python3
"""
Reference values for the FastPilot metrics computed with FastF1 (TCC II, Fase 4).

FastF1 uses the official F1 live timing feed (not OpenF1), so the comparison checks
both the data FastPilot relies on and its calculations. The definitions follow
TCC I §4.7, implemented with FastF1's own data and filters:

- clean pace: laps without pit in/out, without safety car / VSC / red flag (TrackStatus
  4, 5, 6, 7) and excluding lap 1 (standing start); 5% trimmed on each side; mean and median.
- tyre degradation: per stint, same lap filter, linear regression LapTime ~ TyreLife (>= 3 laps).
- theoretical best: min(S1) + min(S2) + min(S3) over laps that are not pit-out laps.
- best lap: fastest lap time that is not a pit-out lap.
- lap delta: fastf1.utils.delta_time between the fastest laps of the top-3 finishers.

Usage:
  python3 -m venv ~/.venvs/fastpilot-fastf1 && ~/.venvs/fastpilot-fastf1/bin/pip install fastf1
  ~/.venvs/fastpilot-fastf1/bin/python scripts/fastf1_reference.py [--holdout]
Since 15 Oct 2026 the F1 live timing archive requires an F1 TV account: FastF1 >= 3.9 with
F1_USERNAME/F1_PASSWORD (the five comparison races were fetched on 6 Oct 2026 with FastF1 3.8.3).
Writes metrics/comparison/fastf1/<race>.json (FastF1 cache in ~/.cache/fastf1).
"""
import json
import sys
import warnings
from pathlib import Path

import fastf1
import fastf1.utils
import numpy as np
import pandas as pd

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "metrics" / "comparison" / "fastf1"

# Same races and folders as scripts/fetch_fixtures.py
RACES = [
    ("2023_bahrain", 2023, "Bahrain"),
    ("2023_belgium", 2023, "Belgium"),
    ("2023_singapore", 2023, "Singapore"),
    ("2024_monaco", 2024, "Monaco"),
    ("2024_brazil", 2024, "São Paulo"),
]

# Hold-out races (HOLDOUT_RACES in scripts/fetch_fixtures.py), only with --holdout
HOLDOUT_RACES = [
    ("2023_japan", 2023, "Japan"),
    ("2024_las_vegas", 2024, "Las Vegas"),
]

NEUTRALISED = set("4567")  # SC, red flag, VSC, VSC ending
TRIM = 0.05


def seconds(td):
    return None if pd.isna(td) else td.total_seconds()


def clean_laps(laps: pd.DataFrame) -> pd.DataFrame:
    """Lap filter from TCC I §4.7: no pit in/out laps, no neutralised laps, no standing-start lap."""
    status = laps["TrackStatus"].astype(str)
    return laps[
        laps["PitInTime"].isna()
        & laps["PitOutTime"].isna()
        & laps["LapTime"].notna()
        & (laps["LapNumber"] > 1)
        & ~status.apply(lambda s: bool(NEUTRALISED & set(s)))
    ]


def trimmed(values):
    values = sorted(values)
    k = int(len(values) * TRIM)
    return values[k:len(values) - k] if len(values) > 2 * k else values


def driver_metrics(laps: pd.DataFrame) -> dict:
    not_pit_out = laps[laps["PitOutTime"].isna()]
    clean = trimmed([seconds(t) for t in clean_laps(laps)["LapTime"]])
    with_sectors = not_pit_out.dropna(subset=["Sector1Time", "Sector2Time", "Sector3Time"])
    best_lap = not_pit_out["LapTime"].min()

    stints = []
    stint_ranges = laps.groupby("Stint")["LapNumber"].agg(["min", "max"])
    for stint_number, stint in clean_laps(laps).groupby("Stint"):
        stint = stint.dropna(subset=["TyreLife"])
        if len(stint) < 3:
            continue
        x = stint["TyreLife"].to_numpy(dtype=float)
        y = stint["LapTime"].dt.total_seconds().to_numpy()
        slope, intercept = np.polyfit(x, y, 1)
        stints.append({
            "stint": int(stint_number),
            "compound": str(stint["Compound"].iloc[0]),
            "lap_start": int(stint_ranges.loc[stint_number, "min"]),
            "lap_end": int(stint_ranges.loc[stint_number, "max"]),
            "laps": len(stint),
            "slope": float(slope),
            "intercept": float(intercept),
        })

    return {
        "clean_pace_mean": float(np.mean(clean)) if clean else None,
        "clean_pace_median": float(np.median(clean)) if clean else None,
        "clean_lap_count": len(clean),
        "theoretical_best": (
            seconds(with_sectors["Sector1Time"].min()) + seconds(with_sectors["Sector2Time"].min())
            + seconds(with_sectors["Sector3Time"].min())
        ) if len(with_sectors) else None,
        "best_lap": seconds(best_lap),
        "stints": stints,
    }


def _delta_time_383(reference_lap, compare_lap):
    """fastf1.utils.delta_time as in FastF1 3.8.3 (MIT licence), removed in FastF1 3.9."""
    ref = reference_lap.get_car_data(interpolate_edges=True).add_distance()
    comp = compare_lap.get_car_data(interpolate_edges=True).add_distance()

    def mini_pro(stream):
        dstream_start = stream[1] - stream[0]
        dstream_end = stream[-1] - stream[-2]
        return np.concatenate([[stream[0] - dstream_start], stream, [stream[-1] + dstream_end]])

    ltime = mini_pro(comp["Time"].dt.total_seconds().to_numpy())
    multiplier = ref.Distance.iat[-1] / comp.Distance.iat[-1]
    ldistance = mini_pro(comp["Distance"].to_numpy()) * multiplier
    lap_time = np.interp(ref["Distance"], ldistance, ltime)
    return lap_time - ref["Time"].dt.total_seconds(), ref, comp


# FastF1 >= 3.9 needs an F1 TV account for the timing archive and no longer has utils.delta_time
delta_time = getattr(fastf1.utils, "delta_time", _delta_time_383)


def lap_deltas(session, top3: list) -> list:
    """delta_time between the fastest laps of each pair of the top-3 finishers, on a normalised distance grid."""
    fastest = {n: session.laps.pick_drivers(n).pick_fastest() for n in top3}
    out = []
    for ref, cmp_ in [(top3[0], top3[1]), (top3[0], top3[2]), (top3[1], top3[2])]:
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            delta, ref_tel, _ = delta_time(fastest[ref], fastest[cmp_])
        distance = ref_tel["Distance"].to_numpy()
        out.append({
            "reference": int(ref),
            "compared": int(cmp_),
            "reference_lap": int(fastest[ref]["LapNumber"]),
            "compared_lap": int(fastest[cmp_]["LapNumber"]),
            "lap_length": float(distance[-1]),
            "fraction": (distance / distance[-1]).round(5).tolist(),
            "delta": np.asarray(delta, dtype=float).round(4).tolist(),
            "official_gap": seconds(fastest[cmp_]["LapTime"] - fastest[ref]["LapTime"]),
        })
    return out


def main():
    cache = Path.home() / ".cache" / "fastf1"
    cache.mkdir(parents=True, exist_ok=True)
    fastf1.Cache.enable_cache(str(cache))
    fastf1.set_log_level("WARNING")
    OUT.mkdir(parents=True, exist_ok=True)

    for folder, year, event in (HOLDOUT_RACES if "--holdout" in sys.argv else RACES):
        print(f"{folder}: loading {year} {event}")
        session = fastf1.get_session(year, event, "R")
        session.load(laps=True, telemetry=True, weather=False, messages=False)
        results = session.results.sort_values("Position")
        top3 = [str(n) for n in results["DriverNumber"].head(3)]

        drivers = {}
        for number in session.laps["DriverNumber"].unique():
            drivers[str(int(number))] = driver_metrics(session.laps.pick_drivers(number))

        report = {
            "race": folder,
            "fastf1_version": fastf1.__version__,
            "drivers": drivers,
            "lap_deltas": lap_deltas(session, top3),
        }
        (OUT / f"{folder}.json").write_text(json.dumps(report, indent=1) + "\n", encoding="utf-8")
        print(f"  {len(drivers)} drivers, top3 {top3}")


if __name__ == "__main__":
    main()
