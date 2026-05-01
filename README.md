# QRScanner

A simple and efficient Android application for scanning QR codes using Jetpack Compose and CameraX.

## Features

- **Wi-Fi Connectivity (Android 12+)**: Automatically parses Wi-Fi QR codes and facilitates persistent connections using `WifiNetworkSuggestion` and the system's "Add Networks" interface. Includes real-time connection verification.
- **Live Camera Scanning**: Real-time QR code detection using the device's back camera.
- **Image Import**: Scan QR codes from images stored in your device's gallery.
- **Pinch-to-Zoom**: Easily zoom in and out during live scanning using scale gestures.
- **URL Handling**: Automatically detects URLs in QR codes and provides an option to open them in a web browser.
- **Copy to Clipboard**: For non-URL and non-Wi-Fi QR codes, a "Copy" button is provided to easily copy the text content to the system clipboard.
- **Interactive UI**:
    - Visual bounding box for targeting QR codes.
    - Start/Stop scanning controls to save battery and processing power.
    - Clear results display via dialogs.
- **Permission Management**: Seamlessly handles camera and location permissions (required for Wi-Fi SSID verification).

## Requirements

- **Minimum SDK**: Android 12 (API level 31)
- **Permissions**: `CAMERA`, `ACCESS_FINE_LOCATION` (for Wi-Fi functionality)

## Technologies Used

- **Jetpack Compose**: For building a modern, declarative UI.
- **CameraX**: For robust camera integration and image analysis.
- **ML Kit**: For high-performance QR code detection.
- **Coil**: For efficient image loading when previewing imported images.
- **Kotlin Coroutines & Flow**: For reactive state management.

## Project Structure

- `MainActivity.kt`: The entry point of the application.
- `ScannerScreen.kt`: The main UI component containing the camera preview and scan results.
- `ScannerViewModel.kt`: Manages the UI state and scanning logic.
- `CameraAnalyzer.kt`: Custom CameraX analyzer for processing frames with ML Kit.
- `ImageReaderUtils.kt`: Utilities for scanning QR codes from static images (URIs).
- `ImageReaderUtils.kt`: Helper for decoding QR codes from local storage.
