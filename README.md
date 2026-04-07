<div align="center">

  # 🏎️ FastPilot

  **A modern Android, iOS, and Desktop app for Real-Time Formula 1 Telemetry & Historical Data.**

  [![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/) 
  [![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-000000?style=for-the-badge&logo=android&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
  [![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop-lightgrey?style=for-the-badge)](#)
  [![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](#)
</div>

<br/>

## 📖 Overview

**FastPilot** is a cutting-edge cross-platform application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. Designed for Formula 1 enthusiasts, the app provides real-time access to live session data, complex telemetry, team radio communications, live weather maps, and historical standings.

By leveraging a robust **Offline-First MVI Architecture**, FastPilot guarantees a fluid user experience even in poor network conditions, caching data natively on your device.

---

## ✨ Features

* **⏱️ Live Dashboards:** Track race control messages, driver positions, and intervals in real-time.
* **📻 Live Team Radio:** Listen to raw, unfiltered radio communications between the driver and the pit-wall.
* **📊 Deep Telemetry:** Dive into microsecond intervals comparing RPM, Speed, and Gear shifts constraint between two selected drivers (Duels).
* **🌦️ Live Weather & Strategy:** Track track temperatures, wind speeds, and predict tire degradation cycles using historical stint data.
* **🏆 Historical & Standings Data:** Catch up with previous 2024 and 2025 championships, including driver and constructor standings.

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

## 🛠️ Tech Stack & Architecture

This project strictly follows **Clean Architecture** patterns, separating concerns among Data, Domain, and Presentation layers purely in the `shared` module.

### Core Technologies
* **[Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)**: For sharing business logic across Android, iOS, and JVM.
* **[Compose Multiplatform](https://www.jetbrains.com/lp/compose/)**: Unifying the UI implementation.
* **[Ktor Client](https://ktor.io/)**: Asynchronous HTTP engine used to request external F1 APIs robustly.
* **[SQLDelight](https://cashapp.github.io/sqldelight/)**: Local relational SQLite database acting as the single source of truth (Offline-first approach).
* **[Koin](https://insert-koin.io/)**: Pragmatic Dependency Injection for Kotlin.
* **[Voyager](https://voyager.adriel.cafe/)**: Intuitive UI navigation framework optimized for Compose.

### Data Aggregators
* **[OpenF1 API](https://api.openf1.org/v1)**: High-resolution telemetry, timing, weather, and real-time F1 endpoints.
* **[Jolpica Ergast API](https://api.jolpi.ca/ergast/f1/)**: Standardized open dataset for retrospective F1 results & championships.

---

## 🚀 Getting Started

To run FastPilot on your local machine, ensure you have **Android Studio** or **IntelliJ IDEA** installed, along with the **Kotlin Multiplatform** plugin.

### Prerequisites
* JDK 17
* Android SDK (API 35 max, min API 26)
* Xcode (if compiling for iOS Simulator/Device)

### Clone the Repository
```bash
git clone https://github.com/your-username/FastPilot.git
cd FastPilot
```

### Build Requirements
#### Android
Open the project in Android Studio, select the `composeApp` run configuration targeting an Android Emulator or physical device, and hit run!
Alternatively, from the CLI:
```bash
./gradlew :composeApp:installDebug
```

#### Desktop (JVM)
You can test the cross-platform application rapidly on your computer without emulators:
```bash
./gradlew :composeApp:run
```

#### iOS
Open the `iosApp` folder inside XCode, install pods (if applicable), load the environment, and select your iOS device model. 

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! 
Feel free to check [issues page](#) if you want to contribute.

## 📝 License

This project is [MIT](https://choosealicense.com/licenses/mit/) licensed.
