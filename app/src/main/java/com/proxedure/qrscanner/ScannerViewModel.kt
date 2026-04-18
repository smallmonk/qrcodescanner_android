package com.proxedure.qrscanner

import android.util.Patterns
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScannerState(
    val detectedValue: String? = null,
    val isUrl: Boolean = false,
    val showDialog: Boolean = false,
    val isScanningEnabled: Boolean = false,
    val selectedImageUri: android.net.Uri? = null
)

class ScannerViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScannerState())
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    fun onQrCodeDetected(value: String, imageUri: android.net.Uri? = null) {
        val isUrl = Patterns.WEB_URL.matcher(value).matches()
        _state.value = _state.value.copy(
            detectedValue = value,
            isUrl = isUrl,
            showDialog = true,
            isScanningEnabled = false,
            selectedImageUri = imageUri
        )
    }

    fun startScanning() {
        _state.value = _state.value.copy(
            isScanningEnabled = true,
            detectedValue = null,
            selectedImageUri = null
        )
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(showDialog = false)
    }

    fun clearResult() {
        _state.value = _state.value.copy(
            detectedValue = null,
            isUrl = false,
            showDialog = false,
            selectedImageUri = null
        )
    }
}
