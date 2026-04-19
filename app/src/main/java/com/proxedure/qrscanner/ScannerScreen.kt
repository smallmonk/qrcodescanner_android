package com.proxedure.qrscanner

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                ImageReaderUtils.scanQrFromUri(context, uri) { result ->
                    if (result != null) {
                        viewModel.onQrCodeDetected(result, uri)
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
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

    if (state.showDialog && state.detectedValue != null) {
        ResultDialog(
            content = state.detectedValue!!,
            isUrl = state.isUrl,
            onDismiss = {
                viewModel.clearResult()
            },
            onOpenUrl = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setPackage("com.android.chrome")
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    try {
                        context.startActivity(fallbackIntent)
                    } catch (e2: Exception) {
                        // Handle no browser available
                    }
                }
            }
        )
    }
}

@Composable
fun CameraPreview(
    onQrCodeDetected: (String) -> Unit,
    isDialogShowing: Boolean,
    isScanningEnabled: Boolean
) {
    val context = LocalContext.current
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
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                    }

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
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
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
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
            if (isUrl) {
                Button(onClick = { onOpenUrl(content) }) {
                    Text("Open in Google Chrome")
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
