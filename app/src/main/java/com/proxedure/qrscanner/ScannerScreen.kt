package com.proxedure.qrscanner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.view.ScaleGestureDetector
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import android.net.wifi.WifiInfo
import android.net.wifi.WifiNetworkSuggestion
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var lastAttemptedSsid by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
            hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        }
    )

    val wifiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val attemptedSsid = lastAttemptedSsid
                if (attemptedSsid != null) {
                    val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val networkRequest = NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build()

                    val handler = Handler(Looper.getMainLooper())
                    var isUnregistered = false

                    val callback = object : ConnectivityManager.NetworkCallback(
                        FLAG_INCLUDE_LOCATION_INFO
                    ) {
                        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                            val wifiInfo = capabilities.transportInfo as? WifiInfo
                            val currentSsid = wifiInfo?.ssid?.removeSurrounding("\"")
                            if (currentSsid == attemptedSsid) {
                                handler.post {
                                    if (!isUnregistered) {
                                        isUnregistered = true
                                        Toast.makeText(context, "Wifi connection to $attemptedSsid is established", Toast.LENGTH_SHORT).show()
                                        try {
                                            connectivityManager.unregisterNetworkCallback(this)
                                        } catch (e: Exception) {
                                            // Already unregistered
                                        }
                                        handler.removeCallbacksAndMessages(null)
                                    }
                                }
                            }
                        }
                    }

                    connectivityManager.registerNetworkCallback(networkRequest, callback)

                    // Timeout after 10 seconds if connection is not confirmed
                    handler.postDelayed({
                        if (!isUnregistered) {
                            isUnregistered = true
                            Toast.makeText(context, "$attemptedSsid is added previously.  Manual switch to it is required.", Toast.LENGTH_SHORT).show()
                            connectivityManager.unregisterNetworkCallback(callback)
                        }
                    }, 10000)
                }
            }
            viewModel.clearResult()
        }
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                ImageReaderUtils.scanQrFromUri(context, uri) { result ->
                    if (result != null) {
                        viewModel.onQrCodeDetected(result, uri)
                    } else {
                        viewModel.onNoQrCodeDetected()
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val cameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        val locationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        
        hasCameraPermission = cameraPermission == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasLocationPermission = locationPermission == android.content.pm.PackageManager.PERMISSION_GRANTED

        val permissionsToRequest = mutableListOf<String>()
        if (!hasCameraPermission) permissionsToRequest.add(Manifest.permission.CAMERA)
        if (!hasLocationPermission) permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "QR Scanner") })
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.stopCamera()
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(Icons.Default.Image, contentDescription = "Import Image")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.selectedImageUri != null) {
                AsyncImage(
                    model = state.selectedImageUri,
                    contentDescription = "Selected QR Code",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else if (hasCameraPermission) {
                if (state.isCameraActive) {
                    CameraPreview(
                        onQrCodeDetected = { viewModel.onQrCodeDetected(it) },
                        isDialogShowing = state.showDialog,
                        isScanningEnabled = state.isScanningEnabled
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    )
                }
            } else {
                Text(
                    text = "Camera permission is required to scan QR codes.",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Add standard bounding box overlay
            if (state.selectedImageUri == null && hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .align(Alignment.Center)
                        .border(
                            width = 4.dp,
                            color = if (state.isScanningEnabled) Color.Green else Color.Gray,
                            shape = MaterialTheme.shapes.medium
                        )
                ) {
                    if (state.isCameraActive) {
                        if (state.isScanningEnabled) {
                            Button(
                                onClick = { viewModel.stopCamera() },
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Text("Stop Preview")
                            }
                        }
                    } else if (!state.showDialog) {
                        Button(
                            onClick = { viewModel.startScanning() },
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Text("Tap to Scan")
                        }
                    }
                }
            }
        }
    }

    if (state.showDialog) {
        ResultDialog(
            content = state.detectedValue ?: "No QR code detected in the selected image.",
            isUrl = state.isUrl,
            isWifi = state.isWifi,
            onDismiss = {
                viewModel.clearResult()
            },
            onOpenUrl = { url ->
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Handle no browser available
                }
            },
            onConnectWifi = { wifiString ->
                if (hasLocationPermission) {
                    val config = parseWifiString(wifiString)
                    lastAttemptedSsid = config.ssid
                    connectToWifi(context, config, wifiLauncher)
                } else {
                    Toast.makeText(context, "Location permission is required to connect to Wi-Fi", Toast.LENGTH_LONG).show()
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                }
            }
        )
    }
}

private data class WifiConfig(
    val ssid: String,
    val password: String,
    val type: String,
    val hidden: Boolean
)

private fun parseWifiString(wifiString: String): WifiConfig {
    val ssid = wifiString.substringAfter("S:", "").substringBefore(";")
    val password = wifiString.substringAfter("P:", "").substringBefore(";")
    val type = wifiString.substringAfter("T:", "").substringBefore(";")
    val hidden = wifiString.substringAfter("H:", "").substringBefore(";").equals("true", ignoreCase = true)
    return WifiConfig(ssid, password, type, hidden)
}

private fun connectToWifi(
    context: android.content.Context,
    config: WifiConfig,
    launcher: ActivityResultLauncher<Intent>
) {
    // 1. Add as a high-priority suggestion for persistent connectivity and autojoin.
    // Suggestions are the modern way to ensure the system connects to and remembers a network.
    val suggestionBuilder = WifiNetworkSuggestion.Builder()
        .setSsid(config.ssid)
        .setIsHiddenSsid(config.hidden)
        .setIsInitialAutojoinEnabled(true)
        .setPriority(100)

    if (config.password.isNotEmpty()) {
        when {
            config.type.contains("WPA3", ignoreCase = true) -> suggestionBuilder.setWpa3Passphrase(config.password)
            else -> suggestionBuilder.setWpa2Passphrase(config.password)
        }
    }

    val suggestionsList = arrayListOf(suggestionBuilder.build())

    // 2. Trigger the "Add Networks" system UI.
    // This allows the user to save the network persistently. If already saved, 
    // the system will still prioritize the connection based on the suggestion settings above.
    val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS).apply {
        putParcelableArrayListExtra(Settings.EXTRA_WIFI_NETWORK_LIST, suggestionsList)
    }

    try {
        launcher.launch(intent)
        Toast.makeText(context, "Switching to ${config.ssid}...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Wi-Fi settings", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isDialogShowing: Boolean,
    isScanningEnabled: Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val analyzer = remember {
        CameraAnalyzer { result ->
            onQrCodeDetected(result)
        }
    }
    
    LaunchedEffect(isScanningEnabled) {
        if (isScanningEnabled) {
            analyzer.resumeScanning()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            var camera: Camera? = null

            val scaleGestureDetector = ScaleGestureDetector(ctx, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    camera?.let { cam ->
                        val zoomState = cam.cameraInfo.zoomState.value
                        val currentZoomRatio = zoomState?.zoomRatio ?: 1f
                        cam.cameraControl.setZoomRatio(currentZoomRatio * detector.scaleFactor)
                    }
                    return true
                }
            })

            previewView.setOnTouchListener { view, event ->
                scaleGestureDetector.onTouchEvent(event)
                view.performClick()
                true
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                    }

                try {
                    cameraProvider.unbindAll()
                    camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun ResultDialog(
    content: String,
    isUrl: Boolean,
    isWifi: Boolean,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onConnectWifi: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Scan Result")
        },
        text = {
            Text(text = content)
        },
        confirmButton = {
            Row {
                if (isUrl) {
                    Button(onClick = { onOpenUrl(content) }) {
                        Text("Open in Browser")
                    }
                }
                if (isWifi) {
                    Button(onClick = { onConnectWifi(content) }) {
                        Text("Connect to Wifi")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
