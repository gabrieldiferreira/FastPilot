#!/usr/bin/env python3
"""
Runs the in-app load benchmark (composeApp/.../ui/perf/LoadBenchmark.kt) on one platform
and samples the app's memory and CPU while it runs (TCC I §4.8, Fase 5 of TCC II).

    scripts/perf_run.py android [--serial SERIAL] [--spec off:5,on:3] [--label NAME]
    scripts/perf_run.py ios     [--device UDID]   [--spec ...]
    scripts/perf_run.py web     [--spec ...]

Builds are not triggered here; build first:
    android: ./gradlew :composeApp:assembleBenchmark        (release code, debug key)
    ios:     xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release \
                 -sdk iphonesimulator -derivedDataPath build/ios-perf build
    web:     ./gradlew :composeApp:wasmJsBrowserDistribution

Output in metrics/performance/<label>/ (label defaults to the platform):
    loads.jsonl     every FASTPILOT_PERF line printed by the app
    resources.csv   t_ms, memory_mb, cpu_pct, rss_mb sampled every --interval seconds
                    (memory_mb is PSS on Android, read every 30 s because dumpsys meminfo
                    forces a GC in the app, and RSS elsewhere; rss_mb is RSS on every
                    platform, for comparisons; CPU from top / ps of the app or renderer
                    process, 100% = one core)
    js_heap.csv     Web only: JS heap and renderer main-thread busy % from Chrome DevTools
    device.json     device / OS / build details
Only one platform should run at a time: all of them share the OpenF1 request budget of this machine.
"""

import argparse
import csv
import json
import os
import re
import shutil
import signal
import subprocess
import sys
import tempfile
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PACKAGE = "com.fastpilot.app"
MARKER = "FASTPILOT_PERF "
ADB = shutil.which("adb") or str(Path.home() / "Library/Android/sdk/platform-tools/adb")
NODE = shutil.which("node") or str(Path.home() / ".gradle/nodejs/node-v22.0.0-darwin-arm64/bin/node")
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"


def now_ms() -> int:
    return int(time.time() * 1000)


def sh(*cmd: str, check: bool = True) -> str:
    return subprocess.run(cmd, check=check, capture_output=True, text=True, errors="replace").stdout.strip()


class Recorder:
    """Collects benchmark lines from a text stream and stops when the 'done' phase arrives."""

    def __init__(self, out: Path):
        self.file = (out / "loads.jsonl").open("w")
        self.done = threading.Event()
        self.lines = 0

    def feed(self, text: str):
        idx = text.find(MARKER)
        if idx < 0:
            return
        payload = text[idx + len(MARKER):].strip()
        try:
            event = json.loads(payload)
        except json.JSONDecodeError:
            return
        self.file.write(payload + "\n")
        self.file.flush()
        self.lines += 1
        if event.get("type") == "load":
            print(f"  {event['screen']:<9} {event['step']:<7} {event['cache']:<4} "
                  f"{'poll' if event['polling'] else '    '} run {event['run']}: "
                  f"{event['ms'] / 1000:6.2f} s {event['status']}"
                  f"{' (429 x' + str(event['rateLimited']) + ')' if event['rateLimited'] else ''}", flush=True)
        elif event.get("type") in ("phase", "start", "failure"):
            print(f"  [{event.get('phase', event['type'])}]", payload if event["type"] == "failure" else "", flush=True)
        if event.get("type") == "phase" and event.get("phase") == "done":
            self.done.set()

    def follow(self, stream):
        for line in iter(stream.readline, ""):
            self.feed(line)
            if self.done.is_set():
                break


def sample_loop(out: Path, sampler, interval: float, stop: threading.Event):
    with (out / "resources.csv").open("w", newline="") as f:
        writer = csv.writer(f)
        writer.writerow(["t_ms", "memory_mb", "cpu_pct", "rss_mb"])
        last = None
        while not stop.is_set():
            started = time.time()
            # A long gap means the machine was suspended: the timings around it are not valid
            if last and started - last > 3 * interval + 5:
                print(f"  WARNING: no sample for {started - last:.0f} s (machine asleep?)", flush=True)
            last = started
            try:
                sample = sampler()
            except Exception as e:  # the app may be restarting; keep going
                print("  sample failed:", e, file=sys.stderr)
                sample = None
            if sample:
                memory, cpu, rss = sample
                writer.writerow([now_ms(), f"{memory:.1f}", f"{cpu:.1f}", f"{rss:.1f}"])
                f.flush()
            stop.wait(max(0.0, interval - (time.time() - started)))


# ── Android ────────────────────────────────────────────────────────────────────

def run_android(args, out: Path, recorder: Recorder):
    adb = [ADB] + (["-s", args.serial] if args.serial else [])
    apk = ROOT / "composeApp/build/outputs/apk/benchmark/composeApp-benchmark.apk"
    sh(*adb, "install", "-r", str(apk))
    sh(*adb, "shell", "pm", "clear", PACKAGE)  # empty local cache and app state
    sh(*adb, "logcat", "-c")
    prop = lambda p: sh(*adb, "shell", "getprop", p)
    (out / "device.json").write_text(json.dumps({
        "platform": "android",
        "model": prop("ro.product.model"),
        "manufacturer": prop("ro.product.manufacturer"),
        "android": prop("ro.build.version.release"),
        "sdk": prop("ro.build.version.sdk"),
        "emulator": prop("ro.kernel.qemu") == "1" or "emulator" in (args.serial or ""),
        "cpu_cores": sh(*adb, "shell", "nproc"),
        "build": "benchmark (release, not debuggable)",
    }, indent=2))

    logcat = subprocess.Popen(adb + ["logcat", "-v", "raw", "-s", "System.out:I"],
                              stdout=subprocess.PIPE, text=True)
    sh(*adb, "shell", "am", "start", "-W", "-n", f"{PACKAGE}/.MainActivity", "-e", "perf", args.spec)
    pid = sh(*adb, "shell", "pidof", PACKAGE)

    # `dumpsys meminfo` makes the app run an explicit GC, so PSS is read only every 30 s;
    # RSS and CPU come from `top`, which does not touch the process
    pss = {"value": 0.0, "at": 0.0}

    def sampler():
        if time.time() - pss["at"] >= 30:
            meminfo = sh(*adb, "shell", "dumpsys", "meminfo", pid)
            pss.update(value=int(re.search(r"TOTAL PSS:\s+(\d+)", meminfo).group(1)) / 1024, at=time.time())
        res, cpu = sh(*adb, "shell", "top", "-b", "-n", "1", "-p", pid, "-o", "RES,%CPU", "-q").split()
        units = {"K": 1 / 1024, "M": 1, "G": 1024}
        rss = float(res[:-1]) * units[res[-1]] if res[-1] in units else int(res) / 1024
        return pss["value"], float(cpu), rss

    return logcat, sampler, lambda: (logcat.terminate(), sh(*adb, "shell", "am", "force-stop", PACKAGE))


# ── iOS (simulator) ────────────────────────────────────────────────────────────

def run_ios(args, out: Path, recorder: Recorder):
    app = ROOT / "build/ios-perf/Build/Products/Release-iphonesimulator/iosApp.app"
    device = args.device or "booted"
    sh("xcrun", "simctl", "uninstall", device, PACKAGE, check=False)  # empty local cache
    sh("xcrun", "simctl", "install", device, str(app))
    devices = json.loads(sh("xcrun", "simctl", "list", "devices", "booted", "-j"))["devices"]
    booted = [(runtime, d) for runtime, ds in devices.items() for d in ds if d["state"] == "Booted"]
    runtime, info = next(((r, d) for r, d in booted if d["udid"] == args.device), booted[0])
    (out / "device.json").write_text(json.dumps({
        "platform": "ios",
        "model": info["name"],
        "ios": runtime.rsplit(".", 1)[-1],
        "simulator": True,
        "host": sh("sysctl", "-n", "machdep.cpu.brand_string"),
        "build": "Release",
    }, indent=2))

    launch = subprocess.Popen(["xcrun", "simctl", "launch", "--console-pty", "--terminate-running-process",
                               device, PACKAGE, "-perf", args.spec],
                              stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    pid = ""
    for _ in range(30):
        pid = sh("pgrep", "-f", "iosApp.app/iosApp", check=False).split("\n")[0]
        if pid:
            break
        time.sleep(1)

    def sampler():
        # The simulator runs the app as a host process: resident memory and CPU straight from ps
        rss, cpu = sh("ps", "-o", "rss=,%cpu=", "-p", pid).split()
        return int(rss) / 1024, float(cpu), int(rss) / 1024

    return launch, sampler, lambda: (launch.terminate(), sh("xcrun", "simctl", "terminate", device, PACKAGE, check=False))


# ── Web (Chrome headless) ──────────────────────────────────────────────────────

def run_web(args, out: Path, recorder: Recorder):
    dist = ROOT / "composeApp/build/dist/wasmJs/productionExecutable"
    server = subprocess.Popen([sys.executable, "-m", "http.server", str(args.port)], cwd=dist,
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    profile = tempfile.mkdtemp(prefix="fastpilot-chrome-")
    chrome = subprocess.Popen([CHROME, "--headless=new", f"--remote-debugging-port={args.cdp_port}",
                               f"--user-data-dir={profile}", "--window-size=430,932", "about:blank"],
                              stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    time.sleep(3)
    version = json.loads(sh("curl", "-s", f"http://127.0.0.1:{args.cdp_port}/json/version"))
    (out / "device.json").write_text(json.dumps({
        "platform": "web",
        "browser": version.get("Browser"),
        "headless": True,
        "host": sh("sysctl", "-n", "machdep.cpu.brand_string"),
        "build": "wasmJs production",
    }, indent=2))

    # perf_web.mjs opens the page, relays console lines and prints "SAMPLE t heap_mb main_thread_cpu" lines
    url = f"http://127.0.0.1:{args.port}/index.html?perf={args.spec}"
    driver = subprocess.Popen([NODE, "--no-warnings", "--experimental-websocket", str(ROOT / "scripts/perf_web.mjs"),
                               str(args.cdp_port), url, str(args.interval)],
                              stdout=subprocess.PIPE, text=True)
    samples = (out / "js_heap.csv").open("w", newline="")
    writer = csv.writer(samples)
    writer.writerow(["t_ms", "js_heap_mb", "main_thread_cpu_pct"])

    def sampler():
        # Like the iOS simulator: resident memory and CPU of the tab's renderer process(es),
        # which include the Wasm linear memory (Skia) that the JS heap figure leaves out
        children = sh("pgrep", "-P", str(chrome.pid), check=False).split()
        rss = cpu = 0.0
        for line in sh("ps", "-o", "rss=,%cpu=,command=", "-p", ",".join(children)).splitlines():
            fields = line.split(None, 2)
            if "--type=renderer" in fields[2] and "--extension-process" not in fields[2]:
                rss += int(fields[0]) / 1024
                cpu += float(fields[1])
        return rss, cpu, rss

    class Splitter:
        def __init__(self, stream):
            self.stream = stream

        def readline(self):
            while True:
                line = self.stream.readline()
                if line.startswith("SAMPLE "):
                    _, t, mem, cpu = line.split()
                    writer.writerow([t, mem, cpu])
                    samples.flush()
                    continue
                return line

    def stop():
        driver.terminate()
        chrome.terminate()
        server.terminate()
        samples.close()
        shutil.rmtree(profile, ignore_errors=True)

    return Splitter(driver.stdout), sampler, stop


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("platform", choices=["android", "ios", "web"])
    parser.add_argument("--spec", default="off:5,on:3", help="LoadBenchmark.Config spec")
    parser.add_argument("--label", help="output folder name (default: platform)")
    parser.add_argument("--interval", type=float, default=2.0, help="resource sampling period (s)")
    parser.add_argument("--serial", help="adb serial (android)")
    parser.add_argument("--device", help="simulator UDID (ios; default: booted)")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--cdp-port", type=int, default=9333)
    parser.add_argument("--max-minutes", type=float, default=120)
    args = parser.parse_args()

    # Idle sleep would freeze the emulator/simulator/browser in the middle of a measurement
    if sys.platform == "darwin":
        subprocess.Popen(["caffeinate", "-ims", "-w", str(os.getpid())])

    out = ROOT / "metrics/performance" / (args.label or args.platform)
    out.mkdir(parents=True, exist_ok=True)
    recorder = Recorder(out)
    print(f"{args.platform}: benchmark {args.spec} -> {out.relative_to(ROOT)}", flush=True)

    stream, sampler, stop = {"android": run_android, "ios": run_ios, "web": run_web}[args.platform](args, out, recorder)
    stop_sampling = threading.Event()
    if sampler:
        threading.Thread(target=sample_loop, args=(out, sampler, args.interval, stop_sampling), daemon=True).start()

    reader = threading.Thread(target=recorder.follow, args=(stream.stdout if hasattr(stream, "stdout") else stream,), daemon=True)
    reader.start()
    signal.signal(signal.SIGINT, lambda *_: recorder.done.set())
    finished = recorder.done.wait(timeout=args.max_minutes * 60)
    stop_sampling.set()
    stop()
    print(f"{'done' if finished else 'TIMED OUT'}: {recorder.lines} events in {out.relative_to(ROOT)}")
    sys.exit(0 if finished else 1)


if __name__ == "__main__":
    main()
