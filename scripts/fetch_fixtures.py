#!/usr/bin/env python3
"""
Downloads raw OpenF1 and Jolpica responses for the five races used in the
FastF1 comparison (TCC II, Fase 4) and stores them under fixtures/.

Recorded responses make the evaluation reproducible and independent of API
availability (TCC I, risk #2). Re-run with --force to refresh existing files.

Usage: python3 scripts/fetch_fixtures.py [--force]
"""
import json
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "fixtures"
OPENF1 = "https://api.openf1.org/v1"
JOLPICA = "https://api.jolpi.ca/ergast/f1"

# (folder, OpenF1 race session_key, season, Jolpica round)
RACES = [
    ("2023_bahrain", 7953, 2023, 1),      # dry baseline
    ("2023_belgium", 9141, 2023, 12),     # rain
    ("2023_singapore", 9165, 2023, 15),   # safety cars
    ("2024_monaco", 9523, 2024, 8),       # few overtakes
    ("2024_brazil", 9636, 2024, 21),      # rain + varied strategy
]

# Everything the four metrics need; car_data/location are left out (tens of MB per race)
OPENF1_ENDPOINTS = ["sessions", "drivers", "laps", "stints", "pit", "race_control", "weather", "intervals"]

REQUEST_INTERVAL_S = 0.6  # stay below OpenF1's rate limit
MAX_RETRIES = 8


def fetch(url: str):
    for attempt in range(MAX_RETRIES):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": "FastPilot-TCC-fixtures"})
            with urllib.request.urlopen(req, timeout=120) as resp:
                return json.load(resp)
        except urllib.error.HTTPError as e:
            if e.code == 404 and url.startswith(OPENF1):
                # OpenF1 answers an empty result set with 404 {"detail": "No results found."}
                print("    no data (404)")
                return []
            if (e.code == 429 or e.code >= 500) and attempt < MAX_RETRIES - 1:
                wait = 2 ** attempt
                print(f"    HTTP {e.code}, retrying in {wait}s")
                time.sleep(wait)
                continue
            raise
        except (urllib.error.URLError, OSError):  # includes socket timeouts
            if attempt < MAX_RETRIES - 1:
                time.sleep(2 ** attempt)
                continue
            raise


def fetch_jolpica_laps(url: str):
    """Jolpica caps pages at 100 lap timings; fetch every page and merge them per lap."""
    merged, laps_by_number, offset = None, {}, 0
    while True:
        page = fetch(f"{url}?limit=100&offset={offset}")
        races = page["MRData"]["RaceTable"]["Races"]
        if merged is None:
            merged = page
        for lap in (races[0]["Laps"] if races else []):
            if lap["number"] in laps_by_number:
                laps_by_number[lap["number"]]["Timings"].extend(lap["Timings"])
            else:
                laps_by_number[lap["number"]] = lap
        offset += 100
        if offset >= int(page["MRData"]["total"]):
            break
        time.sleep(REQUEST_INTERVAL_S)
    races = merged["MRData"]["RaceTable"]["Races"]
    if races:
        races[0]["Laps"] = sorted(laps_by_number.values(), key=lambda l: int(l["number"]))
    merged["MRData"].update(limit=merged["MRData"]["total"], offset="0")
    return merged


def save(path: Path, url: str, force: bool, fetcher=fetch):
    if path.exists() and not force:
        print(f"  skip {path.relative_to(ROOT)} (exists)")
        return
    data = fetcher(url)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    count = len(data) if isinstance(data, list) else 1
    print(f"  saved {path.relative_to(ROOT)} ({count} records)")
    time.sleep(REQUEST_INTERVAL_S)


def main():
    force = "--force" in sys.argv
    for folder, session_key, season, rnd in RACES:
        print(f"{folder} (session_key={session_key}, round={rnd})")
        base = ROOT / folder
        for endpoint in OPENF1_ENDPOINTS:
            save(base / "openf1" / f"{endpoint}.json", f"{OPENF1}/{endpoint}?session_key={session_key}", force)
        save(base / "jolpica" / "results.json", f"{JOLPICA}/{season}/{rnd}/results.json", force)
        save(base / "jolpica" / "laps.json", f"{JOLPICA}/{season}/{rnd}/laps.json", force, fetch_jolpica_laps)


if __name__ == "__main__":
    main()
