# Agent Handover Document - QRScanner

## Project Overview
**QRScanner** is an Android application built with Kotlin and Jetpack Compose. It uses CameraX for camera functionality and Google ML Kit for QR code detection.

## Current State
- **Core Functionality**: Live camera scanning, scanning from gallery images, and persistent Wi-Fi connectivity are implemented.
- **UI**: Built with Jetpack Compose. Includes a camera preview with a bounding box, zoom support, and context-aware result dialogs.
- **Scanning Logic**: 
    - `CameraAnalyzer.kt` handles real-time frames.
    - `ImageReaderUtils.kt` handles static image URI decoding.
    - `ScannerViewModel.kt` manages the state (scanning active, results, dialogs).
- **Wi-Fi Integration**: 
    - Targeted for Android 12+ (API 31).
    - Uses `WifiNetworkSuggestion` for persistent connectivity.
    - Uses `ACTION_WIFI_ADD_NETWORKS` intent for immediate user-confirmed connection.
    - Implements `ConnectivityManager.NetworkCallback` with `FLAG_INCLUDE_LOCATION_INFO` for SSID verification.
- **Recent Updates**: 
    - Raised `minSdk` to 31.
    - Implemented robust Wi-Fi connection and verification flow.
    - Cleaned up legacy code and improved permission handling.
    - Updated `README.md` and `AGENTS.md`.

## Key Files & Modules
- `ScannerScreen.kt`: Main UI, camera preview setup, and permission handling.
- `ScannerViewModel.kt`: State machine for the scanner (Scanning, Paused, Result showing).
- `CameraAnalyzer.kt`: Integration with ML Kit Barcode Scanning.
- `WifiUtils.kt`: Currently present but functionality needs verification/expansion (likely for WiFi QR codes).

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Camera**: CameraX
- **Barcode Detection**: Google ML Kit (Barcode Scanning)
- **Image Loading**: Coil
- **Architecture**: MVVM

## Agent Routine
- **Documentation Maintenance**: After each change, review whether `README.md` and `AGENTS.md` should be modified to reflect the latest updates.

## Pending / Future Tasks
- [ ] Implement Scan History.
- [x] Enhance WiFi QR code parsing and connection logic.
- [ ] Add flashlight toggle.
- [ ] Improve UI/UX (animations, better bounding box).

## Context for Next Agent
The project is in a functional state for basic QR scanning. Camera permissions are handled in `ScannerScreen.kt`. The zoom is implemented via `ScaleGestureDetector` in the `AndroidView` factory.
