<div align="center">

  # 🏎️ FastPilot

  **A Kotlin Multiplatform app for live and historical Formula 1 data — Android, iOS and Web from one codebase.**

  [![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/) 
  [![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-000000?style=for-the-badge&logo=android&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
  [![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Web%20%7C%20Desktop-lightgrey?style=for-the-badge)](#)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](#)
</div>

<br/>

## 📖 Overview

**FastPilot** is a cross-platform application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, developed as a Final Course Project (TCC). It consumes the public [OpenF1](https://openf1.org) and [Jolpica (Ergast)](https://github.com/jolpica/jolpica-f1) APIs and computes race metrics locally — clean pace, tyre degradation, theoretical best lap, driver duels and lap delta — in plain, unit-tested Kotlin.

Both the business logic **and the UI** are shared: every screen is written once in Compose Multiplatform and runs on Android, iOS (simulator/device) and the browser (Kotlin/Wasm). The only platform-specific code is the entry point of each app, the HTTP engine, the SQLite driver, the audio player, where the chosen language is saved and how the time zone is read. Measured with `cloc`, **96.1% of the code of the Android + iOS + Web apps is shared** (see [`metrics/shared_code.md`](metrics/shared_code.md)).

The app speaks **Brazilian Portuguese, American English and Spanish** (picked with flags on the login screen) and shows every time in the user's time zone.

---

## ✨ Features

Seven tabs, all shared across platforms:

| Tab | What it shows |
|---|---|
| **Dashboard** | Positions with interval and gap (races and sprints) or best lap and gap to the fastest (practice and qualifying, split into **Q1/Q2/Q3**, laps deleted by race control left out); pit stops and overtakes with their lap; race control messages; opens the live session (or the latest race) and polls OpenF1 while it is live. |
| **Telemetria** | Speed, RPM, gear and throttle traces for a driver, plus a **theoretical best lap** card (best S1 + S2 + S3, compared with the best real lap). |
| **Estratégia** | Stint timeline per driver with compound colours, **tyre degradation** (s/lap per stint) and a **clean pace** ranking (median of clean laps, gap to the leader, σ). |
| **Duelo** | Head-to-head between two drivers and the **lap delta** on their fastest laps, aligned by position on track. |
| **Rádio** | Team radio recordings for the session; new messages every 15 s while it is live. |
| **Clima** | Air/track temperature, wind, humidity and rain; refreshed every 60 s while live. |
| **Tabela** | Driver and constructor championship standings (Jolpica), cached for 7 days (1 hour for the current season). |

Every tab shares the **session picker**: year, Grand Prix (with its round number) and one chip per event of the weekend (FP1–FP3, Sprint Qualifying, Sprint, Qualifying, Race).

Clean pace and tyre degradation discard laps that are not representative (lap 1, in/out laps, laps under SC/VSC/red flag, the lap a driver retires on) using `LapFilters`, matched against `/race_control` by timestamp.

---

## 📸 Gallery

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Splash Screen</b></td>
      <td align="center"><b>Login</b></td>
      <td align="center"><b>Dashboard</b></td>
      <td align="center"><b>Live Telemetry</b></td>
    </tr>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/b5691e0c-40c7-418e-afec-c03395807ff9" alt="Dashboard" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/2d51373a-af64-4718-94ef-4eb9df1199bc" alt="Dashboard" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/e3e9f1ba-11f5-4e2b-be1a-128b03d8e8a6" alt="Dashboard" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/3ca2053c-bdc3-4589-bcde-e7c64e60d736" alt="Telemetry" width="250"/></td>
    </tr>
    <tr>
      <td align="center"><b>Driver Duel</b></td>
      <td align="center"><b>Strategy</b></td>
      <td align="center"><b>Team Radio</b></td>
      <td align="center"><b>Weather</b></td>
    </tr>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/1e0a3075-8b02-4026-ae96-e6403c986724" alt="Driver Duels" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/7d3c8e46-8319-4bc1-a8c8-5fb720001ff0" alt="Weather and Strategy" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/a50e3587-8fcc-4d5d-87b3-79e408b607df" alt="Team Radio" width="250"/></td>
      <td><img src="https://github.com/user-attachments/assets/1ebe2c89-da0b-4db6-ab51-ea6bf11aad51" alt="Splash/Auth" width="250"/></td>
    </tr>
  </table>
</div>

---

## 🛠️ Architecture

```
FastPilot/
├── shared/          # data + domain (Kotlin Multiplatform library)
│   └── src/
│       ├── commonMain/      # API clients, repositories, SQLDelight schema, use cases (metrics)
│       ├── androidMain/     # OkHttp engine, Android SQLite driver
│       ├── iosMain/         # Darwin engine, native SQLite driver
│       ├── wasmJsMain/      # JS fetch engine (no local database on Web)
│       ├── desktopMain/     # CIO engine, JDBC SQLite driver
│       ├── commonTest/      # use case, repository and network tests
│       └── desktopTest/     # schema migration, race fixtures, metrics export harness
├── composeApp/      # presentation: Compose Multiplatform screens, ViewModels, navigation, DI
│   └── src/
│       ├── commonMain/      # all 7 screens + login/splash, Voyager tabs, Koin modules
│       └── androidMain | iosMain | wasmJsMain | desktopMain   # app entry points only
├── iosApp/          # Xcode project (generated by XcodeGen) hosting ComposeUIViewController
├── fixtures/        # recorded OpenF1/Jolpica responses for 5 races
├── scripts/         # evaluation scripts (shared code, FastF1 comparison, performance, Vercel)
└── metrics/         # generated evaluation results
```

The project follows Clean Architecture: `shared` holds the data layer (Ktor clients for OpenF1 and Jolpica, cache-first repositories backed by SQLDelight) and the domain layer (use cases, pure Kotlin); `composeApp` holds the presentation layer (one ViewModel per screen exposing `StateFlow` UI state).

**Offline cache.** On Android, iOS and Desktop, repositories read from SQLite first and only hit the network on a miss. Data downloaded after a session ended — drivers, laps, stints, final positions and gaps, race control, weather, team radio list, pit stops, overtakes and each lap's raw telemetry and track positions — never expires and is never downloaded again, so a race opened once works offline on every screen; race data is deleted 30 days after it was downloaded. Live sessions are never cached. SQLDelight has no synchronous driver for the browser, so on **Web the app runs without the local cache** and every load goes to the network.

**Networking.** OpenF1's free tier allows 3 requests per second **and** 30 in any 60 s (measured). The shared `HttpClient` spaces requests to it (one every 400 ms, at most 29 per 61 s) so the app waits for its turn instead of getting 429s; live sessions go through the Sponsor proxy and are not limited. It retries 429/5xx responses with exponential backoff (2, 4, 8, 16, 16 s) and counts requests and rate-limited responses for the performance benchmark. The calendar of every season comes in a single request (`year>=2023`).

**Languages.** Typed string packages (`composeApp/.../ui/i18n`: `PtBrStrings`, `EnUsStrings`, `EsStrings`) implement one `Strings` interface, so a text missing in any language does not compile; switching language recomposes the app at once and the choice is saved per platform. API texts are translated too: session, country and circuit names, and the FIA **race control messages**, by rules over their closed set of formats (99.9% of 12,372 real messages; a message with any unknown part stays in English).

**Times.** API timestamps are UTC and times written inside race control messages are the circuit's local time; both are shown in the user's time zone (system zone; browser zone on Web), using the session's `gmt_offset` for the latter.

**Web routes.** Each screen has its own path in the app language — `/painel`, `/telemetria`, `/estrategia`, `/duelo`, `/radio`, `/clima`, `/classificacao`, `/entrar` in Portuguese; `/dashboard`, `/telemetry`, `/strategy`, `/duel`, `/radio`, `/weather`, `/standings`, `/login` in English; `/panel`, `/telemetria`, `/estrategia`, `/duelo`, `/radio`, `/clima`, `/clasificacion`, `/acceder` in Spanish — kept with the browser History API: links open a screen directly (a path in any language works), reload keeps it, back/forward switch screens, and changing language rewrites the address without a new history entry. `vercel.json` and `scripts/serve_web.py` serve `index.html` for these paths (a test checks that both list them all).

### Core technologies

| Library | Version | Use |
|---|---|---|
| [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) | 2.1.0 | Targets: `androidTarget`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, `wasmJs`, `jvm("desktop")` |
| [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) | 1.7.3 | Shared UI on all platforms |
| [Ktor Client](https://ktor.io/) | 3.0.3 | HTTP (OkHttp / Darwin / JS / CIO engines) |
| [SQLDelight](https://cashapp.github.io/sqldelight/) | 2.1.0 | Local cache (schema v4 with migrations) |
| [Koin](https://insert-koin.io/) | 4.0.1 | Dependency injection |
| [Voyager](https://voyager.adriel.cafe/) | 1.1.0-beta03 | Tab navigation |

### Data sources
* **[OpenF1 API](https://api.openf1.org/v1)** — sessions, laps, stints, car data, location, positions, intervals, race control, team radio, weather.
* **[Jolpica Ergast API](https://api.jolpi.ca/ergast/f1/)** — championship standings.

---

## 🚀 Getting Started

### Prerequisites
* JDK 17
* Android SDK (compileSdk 35, minSdk 26) — for Android only
* Xcode and [XcodeGen](https://github.com/yonaskolb/XcodeGen) — for iOS only (macOS)
* Google Chrome — for the Web tests

```bash
git clone https://github.com/gabrieldiferreira/FastPilot.git
cd FastPilot
```

### Android
Run the `composeApp` configuration in Android Studio, or:
```bash
./gradlew :composeApp:installDebug
```

### iOS
The Xcode project is generated from `iosApp/project.yml`; a build phase runs `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`, so no CocoaPods are needed.
```bash
cd iosApp && xcodegen generate   # only after editing project.yml
open iosApp.xcodeproj            # pick an iPhone simulator and Run
```

### Web (Kotlin/Wasm)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun        # dev server with reload
./gradlew :composeApp:wasmJsBrowserDistribution          # production build
python3 scripts/serve_web.py 8080                       # serves it; screen paths (/telemetry...) answer index.html
```
Requires a browser with WasmGC (current Chrome, Firefox, Safari 18.2+).

**Deploy on Vercel:** `vercel.json` runs `scripts/vercel_build.sh`, which downloads a JDK 17 (the Vercel image has none), builds the Wasm distribution and serves `composeApp/build/dist/wasmJs/productionExecutable`. Build options live in `.env` (no secrets).

**Live data:** OpenF1 serves real-time data only to paid *Sponsor* accounts. `api/openf1.mjs` is a Vercel function that holds the account (`OPENF1_USERNAME` / `OPENF1_PASSWORD`, set as sensitive environment variables in the Vercel dashboard), adds the access token and forwards requests at `/openf1/v1/<endpoint>`, caching answers for 4 s at the CDN. Set `OPENF1_PROXY_URL` in `shared/.../network/OpenF1Proxy.kt` to `https://<your-deployment>/openf1/v1`: the app then sends live-session requests through the proxy, while historical data still goes straight to OpenF1.

### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

---

## 🧪 Tests

```bash
./gradlew :shared:desktopTest :composeApp:desktopTest     # JVM
./gradlew :shared:iosSimulatorArm64Test                    # iOS simulator
CHROME_BIN="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" \
  ./gradlew :shared:wasmJsBrowserTest :composeApp:wasmJsBrowserTest   # Web
```

Tests cover the metric use cases, `LapFilters`, repositories, the HTTP retry policy, the SQLDelight migration, the recorded race fixtures and the ViewModels (Ktor `MockEngine` running on the test dispatcher).

---

## 📊 Evaluation (TCC II)

All results are generated by scripts in `scripts/` and written to `metrics/`.

### Shared code
```bash
python3 scripts/measure_shared_code.py      # needs cloc → metrics/shared_code.md
```
| Scope | % shared |
|---|---:|
| Android + iOS + Web apps | **96.1%** |
| Android app | 98.4% |
| iOS app | 98.6% |
| Web app | 99.0% |

Tests, generated code and build scripts are excluded; the methodology is in the script header.

### Accuracy vs. FastF1
FastPilot's metrics are compared with the same metrics computed by the [FastF1](https://docs.fastf1.dev/) Python library on five races (Bahrain 2023, Belgium 2023, Singapore 2023, Monaco 2024, São Paulo 2024), using the recorded responses in `fixtures/`.
```bash
python3 scripts/fetch_fixtures.py                     # record API responses (already in the repo)
./gradlew :shared:exportMetrics                       # FastPilot results → metrics/comparison/fastpilot/
python3 scripts/fastf1_reference.py                   # FastF1 results    → metrics/comparison/fastf1/
python3 scripts/compare_metrics.py                    # → metrics/comparison/README.md
```
| Metric | Error (target MAPE ≤ 5%) |
|---|---|
| Clean pace (mean / median) | MAPE 0.00% (n = 91) |
| Theoretical best lap | MAPE 0.00% (n = 91) |
| Best lap | MAPE 0.00% (n = 91) |
| Tyre degradation | MAE 0.006 s/lap, same sign in 221/221 stints |
| Lap delta | curve MAE ≈ 0.1 s, identical final gap (15 pairs) |

Details, the bugs this comparison uncovered and the remaining limitations: [`metrics/comparison/README.md`](metrics/comparison/README.md) and [`metrics/comparison/NOTAS.md`](metrics/comparison/NOTAS.md).

### Performance
A benchmark built into the app (`composeApp/.../ui/perf/`) opens each screen several times and measures the time from the ViewModel call until content is on screen — with and without the local cache, and with live polling on and off — while a script samples CPU and memory of the app process.
```bash
./gradlew :composeApp:assembleBenchmark                        # Android release build for measuring
python3 scripts/perf_run.py android --serial <adb serial>
python3 scripts/perf_run.py ios --device <simulator UDID>      # Release build in build/ios-perf
python3 scripts/perf_run.py web                                # headless Chrome
python3 scripts/perf_report.py                                 # → metrics/performance/README.md
```
The benchmark can also be started by hand: Android `adb shell am start -n com.fastpilot.app/.MainActivity -e perf "off:5,on:3"`, iOS launch argument `-perf off:5,on:3`, Web `index.html?perf=off:5,on:3`.

Latest round (Oct 14 2026, 2026 Singapore GP, 5 runs per screen, 150 loads, all successful, no 429 responses), median time without cache → with cache:

| Screen | Android emulator | iOS simulator | Web (Chrome) |
|---|---|---|---|
| Dashboard | 1.70 s → 0.04 s | 1.07 s → 0.02 s | 0.65 s |
| Strategy (stints) | 1.19 s → 1.42 s | 1.27 s → 0.46 s | 1.04 s |
| Duel (comparison) | 3.39 s → 0.04 s | 1.60 s → 0.01 s | 1.80 s |
| Standings | 0.71 s → 0.01 s | 0.15 s → 0.00 s | 0.18 s |

With the local cache the median screen loads in 2–3% of the time without it (Web has no local cache). This round measured without live polling: on the free OpenF1 tier (30 requests per minute) polling alone (~46/min) exhausts the quota, so the polling round will run through the Sponsor proxy. Full tables, memory and CPU: [`metrics/performance/README.md`](metrics/performance/README.md).

---

## 📚 Documentation

* [`FastPilot_Documentation.md`](FastPilot_Documentation.md) — functional documentation.
* `TCC_I_FastPilot_GabrielFerreira_v.3.0.pdf` — TCC I (proposal). The UI there was planned as Jetpack Compose + SwiftUI; it ended up fully in Compose Multiplatform, which also brought the Web target with almost no extra code.

## 📝 License

This project is [MIT](https://choosealicense.com/licenses/mit/) licensed.
