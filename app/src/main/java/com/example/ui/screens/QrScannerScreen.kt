package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.Treasure
import com.example.ui.components.ArcaneCard
import com.example.ui.components.ArcaneOutlinedButton
import com.example.ui.components.ArcanePrimaryButton
import com.example.ui.theme.AmberForge
import com.example.ui.theme.AmethystGlow
import com.example.ui.theme.ArcaneOutline
import com.example.ui.theme.CrimsonDanger
import com.example.ui.theme.DiscoveryCyan
import com.example.ui.theme.MidnightIndigo
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TreasureGold
import com.example.ui.theme.VoidBlack
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    treasure: Treasure,
    onBack: () -> Unit,
    onQrScanned: (String) -> Boolean,
    onSimulateDemoQr: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var lastScanFeedback by remember { mutableStateOf<String?>(null) }
    var scanHandled by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val infinite = rememberInfiniteTransition(label = "reticle")
    val laserOffset by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        if (hasPermission) {
            val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
            val scanner = remember {
                BarcodeScanning.getClient(
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                )
            }

            DisposableEffect(Unit) {
                onDispose {
                    cameraExecutor.shutdown()
                    scanner.close()
                }
            }

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { imageAnalysis ->
                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && !scanHandled) {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            scanner.process(inputImage)
                                                .addOnSuccessListener { barcodes ->
                                                    val raw = barcodes.firstOrNull()?.rawValue
                                                    if (!raw.isNullOrBlank() && !scanHandled) {
                                                        val matched = onQrScanned(raw)
                                                        if (matched) {
                                                            scanHandled = true
                                                            onBack()
                                                        } else {
                                                            lastScanFeedback = "Invalid seal: ${raw.take(24)}"
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("QrScanner", "Barcode scan error: ${e.message}")
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            boundCamera = camera
                        } catch (e: Exception) {
                            Log.e("QrScanner", "CameraX bind failed", e)
                            lastScanFeedback = "Camera preview unavailable on this device."
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Reticle Overlay
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(2.dp, DiscoveryCyan, RoundedCornerShape(24.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y = size.height * laserOffset
                        drawLine(
                            color = AmethystGlow,
                            start = Offset(16.dp.toPx(), y),
                            end = Offset(size.width - 16.dp.toPx(), y),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                }
            }
        } else {
            // Permission Denied / Rationale State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ArcaneCard(borderColor = NeonViolet.copy(alpha = 0.5f)) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = null,
                            tint = AmethystGlow,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Camera Permission Needed", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(
                            "DeadDrop uses your camera to scan physical QR rune seals placed by treasure creators.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        ArcanePrimaryButton(
                            text = "Grant Camera Access",
                            onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VoidBlack.copy(alpha = 0.72f))
                    .border(1.dp, ArcaneOutline, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = VoidBlack.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, NeonViolet)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = AmethystGlow, modifier = Modifier.size(16.dp))
                    Text("Arcane QR Lens", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }

            IconButton(
                onClick = {
                    val next = !torchEnabled
                    torchEnabled = next
                    boundCamera?.cameraControl?.enableTorch(next)
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VoidBlack.copy(alpha = 0.72f))
                    .border(1.dp, ArcaneOutline, CircleShape)
            ) {
                Icon(
                    imageVector = if (torchEnabled) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = "Toggle flashlight",
                    tint = if (torchEnabled) TreasureGold else TextPrimary
                )
            }
        }

        // Bottom Controls & Explicit Demo Simulation Fallback
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (lastScanFeedback != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonDanger.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CrimsonDanger)
                ) {
                    Text(
                        text = lastScanFeedback.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = CrimsonDanger,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            ArcaneCard(
                borderColor = ArcaneOutline,
                containerColor = MidnightIndigo.copy(alpha = 0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Target: ${treasure.name}",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Align the treasure's QR Seal inside the cyan frame.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    ArcaneOutlinedButton(
                        text = "Demo Mode: Simulate Matching QR Scan",
                        onClick = {
                            onSimulateDemoQr()
                            onBack()
                        },
                        borderColor = AmberForge.copy(alpha = 0.65f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("simulate_qr_button")
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Science, contentDescription = null, tint = AmberForge, modifier = Modifier.size(14.dp))
                        Text(
                            text = "Demo simulation for emulators without a physical QR printout.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberForge
                        )
                    }
                }
            }
        }
    }
}
