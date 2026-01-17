package com.example.iskorko.ui.scan

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerView(
    examName: String,
    totalQuestions: Int,
    onScanComplete: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    var isCapturing by remember { mutableStateOf(false) }
    var detectionMessage by remember { mutableStateOf("Position answer sheet in frame") }
    var cornersDetected by remember { mutableStateOf(false) }
    var stableFrameCount by remember { mutableStateOf(0) }
    var lastCapturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Real-time detected marker positions (normalized 0-1 coordinates)
    var detectedMarkers by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var detectedCorners by remember { mutableStateOf<DetectedCorners?>(null) }
    
    // Number of consecutive frames needed before auto-capture
    val requiredStableFrames = 8  // Balance between speed and accuracy
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Camera Preview with auto-capture
            CameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                cameraExecutor = cameraExecutor,
                onFrameAnalyzed = { bitmap, result ->
                    if (isCapturing) return@CameraPreview // Don't process if already capturing
                    
                    cornersDetected = result.isValid
                    detectedCorners = result.corners
                    detectedMarkers = result.corners?.allMarkers ?: emptyList()
                    
                    if (result.isValid) {
                        stableFrameCount++
                        lastCapturedBitmap = bitmap
                        
                        val progress = (stableFrameCount * 100) / requiredStableFrames
                        detectionMessage = when {
                            progress >= 100 -> "✓ Scanning..."
                            progress >= 75 -> "✓ Almost there... hold steady"
                            progress >= 50 -> "✓ Paper detected - keep holding"
                            else -> "✓ Aligning... stay still"
                        }
                        
                        // Auto-capture when stable for enough frames
                        if (stableFrameCount >= requiredStableFrames && !isCapturing) {
                            isCapturing = true
                            lastCapturedBitmap?.let { onScanComplete(it) }
                        }
                    } else {
                        // Reset counter if corners lost
                        stableFrameCount = 0
                        val markerCount = result.corners?.allMarkers?.size ?: 0
                        detectionMessage = if (markerCount > 0) {
                            "Found $markerCount markers - need 4 corners"
                        } else {
                            "Align 4 corner marks with squares"
                        }
                    }
                },
                onCaptureImage = { bitmap ->
                    onScanComplete(bitmap)
                }
            )
            
            // Overlay with detection frame (Zipgrade-style timing mark squares)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val frameWidth = size.width * 0.85f
                val frameHeight = size.height * 0.55f
                val left = (size.width - frameWidth) / 2
                val top = (size.height - frameHeight) / 2
                
                // Timing mark square size - matches Zipgrade's large corner squares
                val markerSize = size.width * 0.12f  // Square size for timing marks
                val strokeWidth = 6f
                val cornerColor = if (cornersDetected) Color.Green else Color.White.copy(alpha = 0.9f)
                
                // Top-left timing mark square
                drawRect(
                    color = cornerColor,
                    topLeft = Offset(left, top),
                    size = Size(markerSize, markerSize),
                    style = Stroke(width = strokeWidth)
                )
                
                // Top-right timing mark square
                drawRect(
                    color = cornerColor,
                    topLeft = Offset(left + frameWidth - markerSize, top),
                    size = Size(markerSize, markerSize),
                    style = Stroke(width = strokeWidth)
                )
                
                // Bottom-left timing mark square
                drawRect(
                    color = cornerColor,
                    topLeft = Offset(left, top + frameHeight - markerSize),
                    size = Size(markerSize, markerSize),
                    style = Stroke(width = strokeWidth)
                )
                
                // Bottom-right timing mark square
                drawRect(
                    color = cornerColor,
                    topLeft = Offset(left + frameWidth - markerSize, top + frameHeight - markerSize),
                    size = Size(markerSize, markerSize),
                    style = Stroke(width = strokeWidth)
                )
                
                // Optional: Draw connecting lines between squares (frame outline)
                val lineColor = cornerColor.copy(alpha = 0.4f)
                val lineStroke = 2f
                
                // Top line (between top squares)
                drawLine(
                    color = lineColor,
                    start = Offset(left + markerSize, top + markerSize / 2),
                    end = Offset(left + frameWidth - markerSize, top + markerSize / 2),
                    strokeWidth = lineStroke
                )
                
                // Bottom line (between bottom squares)
                drawLine(
                    color = lineColor,
                    start = Offset(left + markerSize, top + frameHeight - markerSize / 2),
                    end = Offset(left + frameWidth - markerSize, top + frameHeight - markerSize / 2),
                    strokeWidth = lineStroke
                )
                
                // Left line (between left squares)
                drawLine(
                    color = lineColor,
                    start = Offset(left + markerSize / 2, top + markerSize),
                    end = Offset(left + markerSize / 2, top + frameHeight - markerSize),
                    strokeWidth = lineStroke
                )
                
                // Right line (between right squares)
                drawLine(
                    color = lineColor,
                    start = Offset(left + frameWidth - markerSize / 2, top + markerSize),
                    end = Offset(left + frameWidth - markerSize / 2, top + frameHeight - markerSize),
                    strokeWidth = lineStroke
                )
            }
            
            // Real-time detected markers overlay (Zipgrade-style)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val markerRadius = 20f
                
                // Draw all detected potential markers as yellow circles
                detectedMarkers.forEach { (normalizedX, normalizedY) ->
                    val x = normalizedX * size.width
                    val y = normalizedY * size.height
                    
                    // Yellow circle for potential markers
                    drawCircle(
                        color = Color.Yellow.copy(alpha = 0.7f),
                        radius = markerRadius,
                        center = Offset(x, y),
                        style = Stroke(width = 3f)
                    )
                }
                
                // Draw identified corners with colored squares
                detectedCorners?.let { corners ->
                    val cornerSize = 40f
                    
                    // Top-left - Red
                    corners.topLeft?.let { (nx, ny) ->
                        val x = nx * size.width
                        val y = ny * size.height
                        drawRect(
                            color = Color.Red,
                            topLeft = Offset(x - cornerSize/2, y - cornerSize/2),
                            size = Size(cornerSize, cornerSize),
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.Red,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                    
                    // Top-right - Blue
                    corners.topRight?.let { (nx, ny) ->
                        val x = nx * size.width
                        val y = ny * size.height
                        drawRect(
                            color = Color.Blue,
                            topLeft = Offset(x - cornerSize/2, y - cornerSize/2),
                            size = Size(cornerSize, cornerSize),
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.Blue,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                    
                    // Bottom-left - Magenta
                    corners.bottomLeft?.let { (nx, ny) ->
                        val x = nx * size.width
                        val y = ny * size.height
                        drawRect(
                            color = Color.Magenta,
                            topLeft = Offset(x - cornerSize/2, y - cornerSize/2),
                            size = Size(cornerSize, cornerSize),
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.Magenta,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                    
                    // Bottom-right - Cyan
                    corners.bottomRight?.let { (nx, ny) ->
                        val x = nx * size.width
                        val y = ny * size.height
                        drawRect(
                            color = Color.Cyan,
                            topLeft = Offset(x - cornerSize/2, y - cornerSize/2),
                            size = Size(cornerSize, cornerSize),
                            style = Stroke(width = 4f)
                        )
                        drawCircle(
                            color = Color.Cyan,
                            radius = 8f,
                            center = Offset(x, y)
                        )
                    }
                    
                    // Draw lines connecting the corners if all 4 are detected
                    if (corners.topLeft != null && corners.topRight != null && 
                        corners.bottomLeft != null && corners.bottomRight != null && cornersDetected) {
                        val lineColor = Color.Green
                        val strokeWidth = 4f
                        
                        val tl = Offset(corners.topLeft.first * size.width, corners.topLeft.second * size.height)
                        val tr = Offset(corners.topRight.first * size.width, corners.topRight.second * size.height)
                        val bl = Offset(corners.bottomLeft.first * size.width, corners.bottomLeft.second * size.height)
                        val br = Offset(corners.bottomRight.first * size.width, corners.bottomRight.second * size.height)
                        
                        // Draw rectangle outline
                        drawLine(lineColor, tl, tr, strokeWidth)  // Top
                        drawLine(lineColor, tr, br, strokeWidth)  // Right
                        drawLine(lineColor, br, bl, strokeWidth)  // Bottom
                        drawLine(lineColor, bl, tl, strokeWidth)  // Left
                    }
                }
            }
            
            // Top bar with info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = examName,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalQuestions Questions",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp
                        )
                    }
                    
                    // Placeholder for balance
                    Box(modifier = Modifier.width(48.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Detection status with progress
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            when {
                                isCapturing -> Color.Green.copy(alpha = 0.5f)
                                cornersDetected -> Color.Green.copy(alpha = 0.3f)
                                else -> Color.White.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            if (cornersDetected) Icons.Filled.CheckCircle else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (cornersDetected) Color.Green else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCapturing) "Processing scan..." else detectionMessage,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Show progress bar when detecting
                    if (cornersDetected && !isCapturing && stableFrameCount > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        LinearProgressIndicator(
                            progress = { stableFrameCount.toFloat() / requiredStableFrames },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp),
                            color = Color.Green,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }
            }
            
            // Bottom instruction panel (auto-scan - no button needed)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isCapturing) {
                    // Show capturing state
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.Green,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scanning answer sheet...",
                        color = Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "IskorKo Scanner",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Place all 4 timing marks inside corner squares\n• Keep paper flat and well-lit\n• Hold steady until scan completes",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
            
        } else {
            // Permission denied
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please grant camera permission to scan answer sheets",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800202)
                    )
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    onFrameAnalyzed: (Bitmap, DetectionResult) -> Unit,
    onCaptureImage: (Bitmap) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
            
            // Image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Image analyzer for real-time detection
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))  // HD for faster processing
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        try {
                            // Convert ImageProxy to Bitmap
                            val bitmap = imageProxy.toBitmap()
                            
                            // Detect corner markers and get positions
                            val result = detectCornerMarkersWithPositions(bitmap)
                            
                            // Update UI on main thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onFrameAnalyzed(bitmap, result)
                            }
                        } catch (e: Exception) {
                            Log.e("ImageAnalyzer", "Frame analysis failed: ${e.message}", e)
                            // Still update UI to show we're processing
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onFrameAnalyzed(
                                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
                                    DetectionResult(false, null)
                                )
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }
                }
            
            // Select back camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Camera binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Data class to hold marker info
 */
private data class MarkerInfo(
    val centerX: Double,
    val centerY: Double,
    val area: Double,
    val rect: org.opencv.core.Rect
)

/**
 * Data class for detected corners with normalized coordinates (0-1)
 */
data class DetectedCorners(
    val topLeft: Pair<Float, Float>?,
    val topRight: Pair<Float, Float>?,
    val bottomLeft: Pair<Float, Float>?,
    val bottomRight: Pair<Float, Float>?,
    val allMarkers: List<Pair<Float, Float>>  // All detected potential markers
)

/**
 * Detection result containing both validity and corner positions
 */
data class DetectionResult(
    val isValid: Boolean,
    val corners: DetectedCorners?
)

/**
 * Zipgrade-style corner detection with position tracking
 * Returns detection result with all marker positions for real-time visualization
 */
private fun detectCornerMarkersWithPositions(bitmap: Bitmap): DetectionResult {
    return try {
        val width = bitmap.width
        val height = bitmap.height
        
        // Convert to OpenCV Mat
        val mat = org.opencv.core.Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)
        
        // Convert to grayscale
        val gray = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.cvtColor(mat, gray, org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY)
        
        // Apply Gaussian blur to reduce noise
        val blurred = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.GaussianBlur(gray, blurred, org.opencv.core.Size(5.0, 5.0), 0.0)
        
        // Apply adaptive threshold
        val binary = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.adaptiveThreshold(
            blurred, binary, 255.0,
            org.opencv.imgproc.Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            org.opencv.imgproc.Imgproc.THRESH_BINARY_INV,
            25, 10.0
        )
        
        // Find contours
        val contours = ArrayList<org.opencv.core.MatOfPoint>()
        val hierarchy = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.findContours(
            binary, contours, hierarchy,
            org.opencv.imgproc.Imgproc.RETR_LIST,
            org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
        )
        
        // Calculate area range for timing marks
        // 1cm marks on 1088px image ≈ 40px side = ~1600px area
        // Answer bubbles are ~150px area - filter them OUT
        val imageArea = width * height
        val minMarkerArea = 800.0   // Larger than answer bubbles (~150px)
        val maxMarkerArea = imageArea * 0.05  // 5% of image
        
        Log.d("CornerDetection", "Image: ${width}x${height}, areaRange=${minMarkerArea.toInt()}-${maxMarkerArea.toInt()}")
        
        // Find all potential square markers
        val allMarkers = mutableListOf<MarkerInfo>()
        
        // Debug: count contours at each filter stage
        var totalContours = contours.size
        var passedArea = 0
        var passedAspect = 0
        var passedFill = 0
        
        for (contour in contours) {
            val area = org.opencv.imgproc.Imgproc.contourArea(contour)
            if (area < minMarkerArea || area > maxMarkerArea) continue
            passedArea++
            
            val rect = org.opencv.imgproc.Imgproc.boundingRect(contour)
            val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
            
            // Must be roughly square (0.5 - 2.0 is very lenient)
            if (aspectRatio < 0.5 || aspectRatio > 2.0) continue
            passedAspect++
            
            // Must be somewhat solid (filled > 40%)
            val filledRatio = area / (rect.width * rect.height).toDouble()
            if (filledRatio < 0.4) continue
            passedFill++
            
            // Calculate center
            val centerX = rect.x + rect.width / 2.0
            val centerY = rect.y + rect.height / 2.0
            
            allMarkers.add(MarkerInfo(centerX, centerY, area, rect))
        }
        
        Log.d("CornerDetection", "Contours: total=$totalContours, passedArea=$passedArea, passedAspect=$passedAspect, passedFill=$passedFill => ${allMarkers.size} markers")
        
        // For now, use all markers as potential corners (no position filtering)
        val potentialMarkers = allMarkers
        
        // Log all markers for debugging
        allMarkers.forEachIndexed { idx, m ->
            val nearCorner = potentialMarkers.contains(m)
            Log.d("CornerDetection", "  Marker $idx: pos=(${m.centerX.toInt()}, ${m.centerY.toInt()}) area=${m.area.toInt()} ${if(nearCorner) "[CORNER]" else "[CENTER-ignored]"}")
        }
        
        // Convert all markers to normalized coordinates for display (show ALL for debugging)
        val allMarkerPositions = allMarkers.map { marker ->
            Pair(
                (marker.centerX / width).toFloat(),
                (marker.centerY / height).toFloat()
            )
        }
        
        // Find 4 markers that form a rectangle and get their positions
        val cornerResult = findCornerMarkersWithPositions(potentialMarkers, width, height)
        
        // Clean up
        mat.release()
        gray.release()
        blurred.release()
        binary.release()
        hierarchy.release()
        contours.forEach { it.release() }
        
        val corners = DetectedCorners(
            topLeft = cornerResult.topLeft,
            topRight = cornerResult.topRight,
            bottomLeft = cornerResult.bottomLeft,
            bottomRight = cornerResult.bottomRight,
            allMarkers = allMarkerPositions
        )
        
        DetectionResult(cornerResult.isValid, corners)
    } catch (e: Exception) {
        Log.e("CornerDetection", "Corner detection failed: ${e.message}", e)
        DetectionResult(false, null)
    }
}

/**
 * Result from corner finding with positions
 */
private data class CornerFindResult(
    val isValid: Boolean,
    val topLeft: Pair<Float, Float>?,
    val topRight: Pair<Float, Float>?,
    val bottomLeft: Pair<Float, Float>?,
    val bottomRight: Pair<Float, Float>?
)

/**
 * Find 4 markers that form valid corners of a rectangle
 * Position-independent: finds the best 4 markers that form a rectangle
 * Returns positions for real-time visualization
 */
private fun findCornerMarkersWithPositions(markers: List<MarkerInfo>, imageWidth: Int, imageHeight: Int): CornerFindResult {
    if (markers.size < 4) {
        Log.d("CornerDetection", "❌ FAIL: Not enough markers: ${markers.size} (need 4)")
        return CornerFindResult(false, null, null, null, null)
    }
    
    // Log all marker positions for debugging
    Log.d("CornerDetection", "=== Analyzing ${markers.size} markers (image: ${imageWidth}x${imageHeight}) ===")
    
    // Sort markers to find the 4 extreme corners of a potential rectangle
    // Use convex hull-like approach: find markers that are most extreme in each diagonal direction
    
    // Find the 4 markers that form the largest valid rectangle
    // TopLeft: minimize (x + y)
    // TopRight: minimize (y - x) or maximize (x - y)  
    // BottomLeft: maximize (y - x) or minimize (x - y)
    // BottomRight: maximize (x + y)
    
    val topLeft = markers.minByOrNull { it.centerX + it.centerY }
    val bottomRight = markers.maxByOrNull { it.centerX + it.centerY }
    val topRight = markers.maxByOrNull { it.centerX - it.centerY }
    val bottomLeft = markers.maxByOrNull { it.centerY - it.centerX }
    
    Log.d("CornerDetection", "Extreme corners found:")
    topLeft?.let { Log.d("CornerDetection", "  TL: (${it.centerX.toInt()}, ${it.centerY.toInt()})") }
    topRight?.let { Log.d("CornerDetection", "  TR: (${it.centerX.toInt()}, ${it.centerY.toInt()})") }
    bottomLeft?.let { Log.d("CornerDetection", "  BL: (${it.centerX.toInt()}, ${it.centerY.toInt()})") }
    bottomRight?.let { Log.d("CornerDetection", "  BR: (${it.centerX.toInt()}, ${it.centerY.toInt()})") }
    
    // Convert to normalized positions for display
    fun toNormalized(marker: MarkerInfo?): Pair<Float, Float>? {
        return marker?.let {
            Pair(
                (it.centerX / imageWidth).toFloat(),
                (it.centerY / imageHeight).toFloat()
            )
        }
    }
    
    val tlPos = toNormalized(topLeft)
    val trPos = toNormalized(topRight)
    val blPos = toNormalized(bottomLeft)
    val brPos = toNormalized(bottomRight)
    
    // Log which corners were identified
    Log.d("CornerDetection", "Corners identified: TL=${topLeft != null} TR=${topRight != null} BL=${bottomLeft != null} BR=${bottomRight != null}")
    
    if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null) {
        Log.d("CornerDetection", "❌ FAIL: Could not identify all 4 corners")
        return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    }
    
    // Make sure we have 4 distinct markers
    val corners = setOf(topLeft, topRight, bottomLeft, bottomRight)
    if (corners.size != 4) {
        Log.d("CornerDetection", "❌ FAIL: Corners not distinct (same marker used for multiple corners): ${corners.size} unique")
        return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    }
    
    // Log corner positions
    Log.d("CornerDetection", "Corner positions:")
    Log.d("CornerDetection", "  TL: (${topLeft.centerX.toInt()}, ${topLeft.centerY.toInt()})")
    Log.d("CornerDetection", "  TR: (${topRight.centerX.toInt()}, ${topRight.centerY.toInt()})")
    Log.d("CornerDetection", "  BL: (${bottomLeft.centerX.toInt()}, ${bottomLeft.centerY.toInt()})")
    Log.d("CornerDetection", "  BR: (${bottomRight.centerX.toInt()}, ${bottomRight.centerY.toInt()})")
    
    // SIMPLIFIED VALIDATION - just check we have 4 distinct markers forming some rectangle
    val areas = listOf(topLeft.area, topRight.area, bottomLeft.area, bottomRight.area)
    val minArea = areas.minOrNull() ?: return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    val maxArea = areas.maxOrNull() ?: return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    
    Log.d("CornerDetection", "Marker sizes: min=${minArea.toInt()} max=${maxArea.toInt()} ratio=${"%.1f".format(maxArea/minArea)}x")
    
    // Very lenient size check - within 10x
    if (maxArea > minArea * 10) {
        Log.d("CornerDetection", "❌ FAIL: Markers too different in size")
        return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    }
    
    // Calculate rectangle dimensions - use absolute values for robustness
    val topWidth = kotlin.math.abs(topRight.centerX - topLeft.centerX)
    val bottomWidth = kotlin.math.abs(bottomRight.centerX - bottomLeft.centerX)
    val leftHeight = kotlin.math.abs(bottomLeft.centerY - topLeft.centerY)
    val rightHeight = kotlin.math.abs(bottomRight.centerY - topRight.centerY)
    
    Log.d("CornerDetection", "Rectangle dims: topW=${topWidth.toInt()} botW=${bottomWidth.toInt()} leftH=${leftHeight.toInt()} rightH=${rightHeight.toInt()}")
    
    // Just check minimum dimensions
    val avgWidth = (topWidth + bottomWidth) / 2
    val avgHeight = (leftHeight + rightHeight) / 2
    
    // Paper should have some minimum size (at least 10% of image)
    val minDimension = kotlin.math.min(avgWidth, avgHeight)
    val minRequired = kotlin.math.min(imageWidth, imageHeight) * 0.10
    
    Log.d("CornerDetection", "Size check: minDim=${minDimension.toInt()} required=${minRequired.toInt()}")
    
    if (minDimension < minRequired) {
        Log.d("CornerDetection", "❌ FAIL: Paper too small")
        return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    }
    
    // VALIDATE: Check that opposite sides are similar (forms a rectangle, not random points)
    val widthRatio = kotlin.math.min(topWidth, bottomWidth) / kotlin.math.max(topWidth, bottomWidth)
    val heightRatio = kotlin.math.min(leftHeight, rightHeight) / kotlin.math.max(leftHeight, rightHeight)
    
    Log.d("CornerDetection", "Parallelism: widthRatio=${"%.2f".format(widthRatio)} heightRatio=${"%.2f".format(heightRatio)} (need >0.6)")
    
    if (widthRatio < 0.6 || heightRatio < 0.6) {
        Log.d("CornerDetection", "❌ FAIL: Not a rectangle (sides not parallel)")
        return CornerFindResult(false, tlPos, trPos, blPos, brPos)
    }
    
    val aspectRatio = avgWidth / avgHeight
    
    Log.d("CornerDetection", "✅ SUCCESS! Valid paper: ${avgWidth.toInt()}x${avgHeight.toInt()}, Ratio: ${"%.2f".format(aspectRatio)}")
    return CornerFindResult(true, tlPos, trPos, blPos, brPos)
}

/**
 * Extension function to convert ImageProxy to Bitmap
 * Properly handles YUV_420_888 format from CameraX with rotation
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun androidx.camera.core.ImageProxy.toBitmap(): Bitmap {
    val image = this.image ?: throw IllegalStateException("Image is null")
    
    val planes = image.planes
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer
    
    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()
    
    val nv21 = ByteArray(ySize + uSize + vSize)
    
    // U and V are swapped for NV21
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)
    
    val yuvImage = android.graphics.YuvImage(
        nv21,
        android.graphics.ImageFormat.NV21,
        this.width,
        this.height,
        null
    )
    
    val out = java.io.ByteArrayOutputStream()
    yuvImage.compressToJpeg(
        android.graphics.Rect(0, 0, this.width, this.height),
        85,
        out
    )
    
    val jpegBytes = out.toByteArray()
    val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        ?: throw IllegalStateException("Failed to decode bitmap")
    
    // Apply rotation if needed
    val rotationDegrees = this.imageInfo.rotationDegrees
    return if (rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap
    }
}

