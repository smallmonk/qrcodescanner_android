# Agent Handover Document - QRScanner

## Project Overview
**QRScanner** is an Android application built with Kotlin and Jetpack Compose. It uses CameraX for camera functionality and Google ML Kit for QR code detection.

## Current State
- **Core Functionality**: Live camera scanning and scanning from gallery images are implemented.
- **UI**: Built with Jetpack Compose. Includes a camera preview with a bounding box, zoom support, and a result dialog.
- **Scanning Logic**: 
    - `CameraAnalyzer.kt` handles real-time frames.
    - `ImageReaderUtils.kt` handles static image URI decoding.
    - `ScannerViewModel.kt` manages the state (scanning active, results, dialogs).
- **Recent Updates**: Added `README.md` and `AGENTS.md`.

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

## Pending / Future Tasks
- [ ] Implement Scan History.
- [ ] Enhance WiFi QR code parsing and connection logic (using `WifiUtils.kt`).
- [ ] Add flashlight toggle.
- [ ] Improve UI/UX (animations, better bounding box).

## Context for Next Agent
The project is in a functional state for basic QR scanning. Camera permissions are handled in `ScannerScreen.kt`. The zoom is implemented via `ScaleGestureDetector` in the `AndroidView` factory.
