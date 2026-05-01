package com.proxedure.qrscanner

import android.util.Patterns
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScannerState(
    val detectedValue: String? = null,
    val isUrl: Boolean = false,
    val isWifi: Boolean = false,
    val showDialog: Boolean = false,
    val isScanningEnabled: Boolean = true,
    val isCameraActive: Boolean = true,
    val selectedImageUri: android.net.Uri? = null
)

class ScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    fun onQrCodeDetected(value: String, imageUri: android.net.Uri? = null) {
        val isUrl = Patterns.WEB_URL.matcher(value).matches()
        val isWifi = value.startsWith("WIFI:", ignoreCase = true)
        _state.value = _state.value.copy(
            detectedValue = value,
            isUrl = isUrl,
            isWifi = isWifi,
            showDialog = true,
            isScanningEnabled = false,
            isCameraActive = false,
            selectedImageUri = imageUri
        )
    }

    fun onNoQrCodeDetected(imageUri: android.net.Uri? = null) {
        _state.value = _state.value.copy(
            detectedValue = null,
            isUrl = false,
            isWifi = false,
            showDialog = true,
            isScanningEnabled = false,
            isCameraActive = false,
            selectedImageUri = imageUri
        )
    }

    fun startScanning() {
        _state.value = _state.value.copy(
            isScanningEnabled = true,
            isCameraActive = true,
            detectedValue = null,
            selectedImageUri = null
        )
    }

    fun stopCamera() {
        _state.value = _state.value.copy(
            isCameraActive = false,
            isScanningEnabled = false
        )
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showDialog = false)
    }

    fun clearResult() {
        val wasFromImage = _state.value.selectedImageUri != null
        _state.value = _state.value.copy(
            detectedValue = null,
            isUrl = false,
            isWifi = false,
            showDialog = false,
            isScanningEnabled = !wasFromImage,
            isCameraActive = !wasFromImage,
            selectedImageUri = null
        )
    }
}
