package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.solver.MathSolver
import com.example.solver.SolveResult
import com.example.ui.HomeworkViewModel
import com.example.ui.theme.InfinityBlue
import com.example.ui.theme.InfinityOrange
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.OnSlateDark
import com.example.ui.theme.OnSlateSurface
import com.example.ui.theme.OnSlateCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerScreen(
    viewModel: HomeworkViewModel,
    modifier: Modifier = Modifier
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val hasPermission = cameraPermissionState.status.isGranted

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasPermission) {
            CameraActiveLayout(viewModel)
        } else {
            CameraPermissionFallbackLayout(
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                viewModel = viewModel
            )
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun CameraActiveLayout(viewModel: HomeworkViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var detectedText by remember { mutableStateOf("") }
    var isAnalyzing by remember { mutableStateOf(false) }
    var showDemoPicker by remember { mutableStateOf(false) }
    var simulationActive by remember { mutableStateOf(false) }
    var simulatedProgress by remember { mutableStateOf(0f) }

    // Active result card state
    val activeResult by viewModel.activeResult.collectAsState()

    // Camera provider variables
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            } catch (e: Exception) {
                // Ignore
            }
            cameraExecutor.shutdown()
        }
    }

    // List of gorgeous homework demo presets for simulation
    val demoEquations = listOf(
        "x^2 - 5x + 6 = 0" to "Quadratic Equation (Roots x=2, 3)",
        "2x + 5 = 15" to "Linear Equation (x = 5)",
        "3/4 - 1/2" to "Fractions (3/4 - 1/2 = 1/4)",
        "d/dx(3x^2 + 5x - 2)" to "Calculus (Derivative = 6x + 5)",
        "2x + y = 5; x - y = 1" to "System of Equations (x=2, y=1)",
        "15 + 3 * 4 - 2" to "Arithmetic (PEMDAS = 25)"
    )

    fun runSimulatedScan(formula: String) {
        coroutineScope.launch {
            simulationActive = true
            detectedText = ""
            simulatedProgress = 0f
            // Glow scan animation for 1.5s
            for (i in 1..10) {
                delay(120)
                simulatedProgress = i / 10f
            }
            detectedText = formula
            simulationActive = false
            // Trigger automatic local math solve!
            viewModel.solveEquation(formula, saveToHistory = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera viewfinder overlay
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProviderRef = cameraProvider
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // On-device ML Kit Latin Text Recognition Analyzer
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (!simulationActive) {
                                    processImageWithMLKit(
                                        imageProxy = imageProxy,
                                        onTextDetected = { text ->
                                            if (text.isNotBlank() && text.length > 2) {
                                                // Clean up common OCR artifacts to match mathematical formatting
                                                val clean = text.replace("\n", " ")
                                                    .replace("X", "x")
                                                    .replace("–", "-")
                                                    .replace("—", "-")
                                                detectedText = clean
                                            }
                                        },
                                        onFailure = {
                                            // Handle fail silently
                                        }
                                    )
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        // Bind failed silently
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Draw HUD / Transparent Crop viewport with clean focus box
        ScannerHUD(
            simulationActive = simulationActive,
            simulatedProgress = simulatedProgress
        )

        // Top Header Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* Toggle camera flash if needed */ },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x7F000000))
            ) {
                Icon(
                    imageVector = Icons.Outlined.FlashOn,
                    contentDescription = "Flash",
                    tint = Color.White
                )
            }

            // Simulated Scan Button
            Button(
                onClick = { showDemoPicker = !showDemoPicker },
                colors = ButtonDefaults.buttonColors(containerColor = InfinityOrange),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("simulation_demo_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Demo Solver",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = { viewModel.setTab(3) }, // Switch to history screen
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0x7F000000))
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History",
                    tint = Color.White
                )
            }
        }

        // Floating Demo Picker Dialog
        if (showDemoPicker) {
            AlertDialog(
                onDismissRequest = { showDemoPicker = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = InfinityOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Demo Equation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(demoEquations) { (equation, desc) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showDemoPicker = false
                                        runSimulatedScan(equation)
                                    },
                                colors = CardDefaults.cardColors(containerColor = SlateCard)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = equation,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = InfinityOrange,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDemoPicker = false }) {
                        Text("Cancel", color = InfinityOrange)
                    }
                },
                containerColor = SlateSurface
            )
        }

        // Scanning viewport instructions label
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-110).dp)
                .background(Color(0x7F000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (simulationActive) "Scanning simulated equation..." else "Align math equation in viewfinder",
                color = if (simulationActive) InfinityOrange else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Bottom Result Overlay Card (Photomath Style Sliding Drawer Card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp) // Leave space for main tab navigation bar
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AnimatedVisibility(
                    visible = detectedText.isNotBlank() || simulationActive,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x3FFFFFFF), RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(containerColor = SlateSurface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Camera,
                                        contentDescription = null,
                                        tint = InfinityOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DETECTED FORMULA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                }

                                IconButton(
                                    onClick = { detectedText = "" },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = if (simulationActive) "Scanning..." else detectedText,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = OnSlateSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.onKeyboardInputChanged(detectedText)
                                        viewModel.setTab(1) // switch to edit keyboard tab
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = InfinityOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit")
                                }

                                Button(
                                    onClick = {
                                        viewModel.solveEquation(detectedText)
                                        // Wait, once solved, let's open details or switch to the math detail presentation!
                                    },
                                    modifier = Modifier.weight(1.5f).testTag("solve_scanned_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = InfinityOrange),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Solve Steps", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // If solved, show an instant drawer detailing the solution
                AnimatedVisibility(
                    visible = activeResult != null && detectedText.isNotBlank() && activeResult?.originalEquation?.replace(" ", "") == detectedText.replace(" ", ""),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    activeResult?.let { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .border(1.dp, InfinityOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                            colors = CardDefaults.cardColors(containerColor = SlateCard),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(InfinityOrange, RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = result.type.uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = InfinityOrange
                                        )
                                    }

                                    Text(
                                        text = "On-Device Rule Solver",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Solution:",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )

                                Text(
                                    text = result.finalAnswer,
                                    fontSize = 22.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = InfinityOrange
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Show the first 2 steps inside preview card
                                Divider(color = Color(0x1F000000), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Step-by-step logic preview:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = InfinityBlue
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val firstStep = result.steps.firstOrNull()
                                if (firstStep != null) {
                                    Text(
                                        text = "${firstStep.title}: ${firstStep.explanation}",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (result.supportsGraph) {
                                        OutlinedButton(
                                            onClick = {
                                                val func = if (result.type == "Quadratic Equation") {
                                                    val a = result.graphParams["a"] ?: 1.0
                                                    val b = result.graphParams["b"] ?: 0.0
                                                    val c = result.graphParams["c"] ?: 0.0
                                                    // Convert to plot-friendly format: e.g. x^2 - 5x + 6
                                                    val signB = if (b >= 0) "+$b" else "$b"
                                                    val signC = if (c >= 0) "+$c" else "$c"
                                                    "${a}x^2${signB}x${signC}"
                                                } else {
                                                    // Linear
                                                    val m = result.graphParams["m"] ?: 1.0
                                                    val c = result.graphParams["c"] ?: 0.0
                                                    "${m}x${if (c >= 0) "+$c" else "$c"}"
                                                }
                                                viewModel.setGraphFunction(func)
                                                viewModel.setTab(2) // open graphing calculator tab
                                            },
                                            modifier = Modifier.weight(1.2f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InfinityBlue),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Plot Graph")
                                        }
                                    }

                                    // Full Steps Dialog popup or detail sheet
                                    Button(
                                        onClick = {
                                            // Switch tab or reveal full sheet overlay
                                            viewModel.onKeyboardInputChanged(result.originalEquation)
                                            viewModel.setTab(1) // switch to edit keyboard which will show full steps card list
                                        },
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = InfinityOrange),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("See All Steps")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom HUD that draws standard Photomath crop viewfinder overlay on live camera
@Composable
fun ScannerHUD(
    simulationActive: Boolean,
    simulatedProgress: Float
) {
    val infiniteTransition = rememberInfiniteTransition()
    val laserYOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Central scanning box dimension
        val boxWidth = 280.dp.toPx()
        val boxHeight = 120.dp.toPx()

        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // 1. Draw outer dark background overlay using punchout blending (clear transparent center)
        drawRect(
            color = Color(0x99000000)
        )

        // Punch out the scanning viewport
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            blendMode = BlendMode.Clear
        )

        // 2. Draw modern high-contrast neon orange crop borders
        val cornerLen = 24.dp.toPx()
        val strokeW = 4.dp.toPx()
        val borderPaint = InfinityOrange

        // Top Left Corner
        drawLine(borderPaint, Offset(left - strokeW/2, top), Offset(left + cornerLen, top), strokeW)
        drawLine(borderPaint, Offset(left, top - strokeW/2), Offset(left, top + cornerLen), strokeW)

        // Top Right Corner
        drawLine(borderPaint, Offset(right + strokeW/2, top), Offset(right - cornerLen, top), strokeW)
        drawLine(borderPaint, Offset(right, top - strokeW/2), Offset(right, top + cornerLen), strokeW)

        // Bottom Left Corner
        drawLine(borderPaint, Offset(left - strokeW/2, bottom), Offset(left + cornerLen, bottom), strokeW)
        drawLine(borderPaint, Offset(left, bottom + strokeW/2), Offset(left, bottom - cornerLen), strokeW)

        // Bottom Right Corner
        drawLine(borderPaint, Offset(right + strokeW/2, bottom), Offset(right - cornerLen, bottom), strokeW)
        drawLine(borderPaint, Offset(right, bottom + strokeW/2), Offset(right, bottom - cornerLen), strokeW)

        // 3. Draw scanning laser animation bar
        val laserY = top + (boxHeight * (if (simulationActive) simulatedProgress else laserYOffset))
        drawLine(
            color = if (simulationActive) InfinityOrange else InfinityBlue.copy(alpha = 0.8f),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}

// Friendly fallback screen when camera permissions are denied
@Composable
fun CameraPermissionFallbackLayout(
    onRequestPermission: () -> Unit,
    viewModel: HomeworkViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(InfinityOrange.copy(alpha = 0.1f), RoundedCornerShape(48.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = InfinityOrange,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Camera Permission Required",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Infinity Homework uses the camera to scan and solve math problems in real-time. Please grant camera permission to continue, or use the interactive scientific keyboard!",
            fontSize = 14.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = InfinityOrange),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Grant Camera Permission", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.setTab(1) }, // switch directly to scientific keyboard calculator
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Keyboard, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Math Keyboard")
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImageWithMLKit(
    imageProxy: ImageProxy,
    onTextDetected: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                var text = ""
                // Find mathematical expression (usually single lines in focus box)
                // Filter lines that resemble mathematical characters
                val mathPattern = "[0-9xXyY+\\-*/()^=√ ÷·]+".toRegex()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val lineText = line.text.trim()
                        if (lineText.matches(mathPattern) || lineText.contains("=") || lineText.contains("d/dx")) {
                            text = lineText
                            break
                        }
                    }
                    if (text.isNotEmpty()) break
                }

                // If nothing matched, default to first non-empty text block line
                if (text.isEmpty() && visionText.textBlocks.isNotEmpty()) {
                    text = visionText.textBlocks.first().lines.firstOrNull()?.text ?: ""
                }

                onTextDetected(text)
                imageProxy.close()
            }
            .addOnFailureListener { exc ->
                onFailure(exc)
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}
