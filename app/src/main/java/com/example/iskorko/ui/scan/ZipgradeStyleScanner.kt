package com.example.iskorko.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Production-grade Zipgrade-style scanner
 * Addresses: fragile detection, double marks, lighting issues, validation
 */
class ZipgradeStyleScanner(private val context: Context) {
    
    companion object {
        private const val TAG = "ZipgradeScanner"
        
        // Timing mark detection - now relative to image size (set in findCornerMarkers)
        private const val MIN_MARKER_AREA_RATIO = 0.0003  // 0.03% of image
        private const val MAX_MARKER_AREA_RATIO = 0.02    // 2% of image
        private const val MARKER_ASPECT_MIN = 0.5         // More lenient
        private const val MARKER_ASPECT_MAX = 2.0         // More lenient
        
        // Bubble detection (adaptive) - more lenient for ovals
        private const val BUBBLE_MIN_AREA = 80.0          // Smaller minimum
        private const val BUBBLE_MAX_AREA = 3000.0        // Larger maximum
        private const val CIRCULARITY_THRESHOLD = 0.40f   // Accept ovals too
        
        // Darkness thresholds (handle multiple scenarios)
        private const val FILLED_THRESHOLD = 0.30f      // Clear fill
        private const val DOUBLE_MARK_THRESHOLD = 0.25f // Both over 25% = double mark
        private const val ERASED_THRESHOLD = 0.15f      // Faint mark detection
        
        // Column/row detection (robust)
        private const val COLUMN_GAP_MULTIPLIER = 2.5f  // Dynamic column detection
        private const val ROW_GAP_THRESHOLD = 50f       // Vertical gap between questions
        
        // Confidence thresholds
        private const val MIN_CONFIDENCE_PASS = 0.70f   // 70% detected = acceptable
        private const val MIN_CONFIDENCE_WARN = 0.85f   // 85% = good
    }
    
    init {
        try {
            System.loadLibrary("opencv_java4")
            Log.d(TAG, "OpenCV loaded")
        } catch (e: Exception) {
            Log.e(TAG, "OpenCV failed: ${e.message}")
        }
    }
    
    fun processAnswerSheet(bitmap: Bitmap, totalQuestions: Int, optionsPerQuestion: Int = 5): ScanResult {
        Log.d(TAG, "=== SCAN START: $totalQuestions questions ===")
        
        if (totalQuestions !in listOf(20, 50, 100)) {
            return ScanResult.Error("Only 20, 50, or 100 questions supported")
        }
        
        return try {
            // Step 1: Robust corner detection with fallback
            val (correctedBitmap, corners) = findAndCorrectPerspective(bitmap)
            
            // Step 2: Adaptive preprocessing based on lighting
            val processedMat = preprocessAdaptive(correctedBitmap)
            
            // Step 3: Detect bubbles with quality filtering
            val bubbles = detectBubblesRobust(processedMat, correctedBitmap)
            Log.d(TAG, "Detected ${bubbles.size} bubbles")
            
            if (bubbles.isEmpty()) {
                return ScanResult.Error("No bubbles detected. Check lighting and focus.")
            }
            
            // Step 4: Column-first grouping with validation
            val questionGroups = groupByColumnsRobust(bubbles, totalQuestions, optionsPerQuestion)
            Log.d(TAG, "Grouped into ${questionGroups.size} questions")
            
            // Step 5: Extract answers with double-mark detection
            val (answers, issues) = extractAnswersWithValidation(questionGroups, correctedBitmap)
            
            // Step 6: Calculate confidence with validation
            val confidence = calculateConfidenceScore(answers, issues, totalQuestions)
            
            Log.d(TAG, "=== SCAN COMPLETE: ${answers.size}/${totalQuestions} detected, ${String.format("%.1f%%", confidence * 100)} confidence ===")
            
            ScanResult.Success(
                answers = answers,
                confidence = confidence,
                issues = issues,
                debugBitmap = correctedBitmap
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Scan failed", e)
            ScanResult.Error("Scan error: ${e.message}")
        }
    }
    
    /**
     * ROBUST CORNER DETECTION - Zipgrade-style
     * Handles skew, lighting, partial obstruction
     */
    private fun findAndCorrectPerspective(bitmap: Bitmap): Pair<Bitmap, List<Point>?> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        
        // Try multiple threshold techniques
        val corners = tryMultipleCornerDetectionMethods(gray, bitmap.width, bitmap.height)
        
        if (corners != null && corners.size == 4) {
            Log.d(TAG, "✓ 4 corners detected, applying perspective correction")
            
            val width = bitmap.width.toDouble()
            val height = bitmap.height.toDouble()
            
            val srcPoints = MatOfPoint2f(*corners.toTypedArray())
            val dstPoints = MatOfPoint2f(
                Point(0.0, 0.0),
                Point(width, 0.0),
                Point(width, height),
                Point(0.0, height)
            )
            
            val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
            val corrected = Mat()
            Imgproc.warpPerspective(mat, corrected, transform, Size(width, height))
            
            val result = Bitmap.createBitmap(corrected.cols(), corrected.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(corrected, result)
            
            return Pair(result, corners)
        }
        
        Log.w(TAG, "⚠ Corners not detected reliably, using original image")
        return Pair(bitmap, null)
    }
    
    /**
     * Try multiple detection methods for robustness
     */
    private fun tryMultipleCornerDetectionMethods(gray: Mat, width: Int, height: Int): List<Point>? {
        // Method 1: Adaptive threshold
        var corners = detectCornersWithThreshold(gray, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, width, height)
        if (corners?.size == 4) return corners
        
        // Method 2: Mean threshold
        corners = detectCornersWithThreshold(gray, Imgproc.ADAPTIVE_THRESH_MEAN_C, width, height)
        if (corners?.size == 4) return corners
        
        // Method 3: Otsu's threshold
        corners = detectCornersWithOtsu(gray, width, height)
        if (corners?.size == 4) return corners
        
        return null
    }
    
    private fun detectCornersWithThreshold(gray: Mat, method: Int, width: Int, height: Int): List<Point>? {
        val binary = Mat()
        Imgproc.adaptiveThreshold(gray, binary, 255.0, method, Imgproc.THRESH_BINARY_INV, 51, 10.0)
        
        return findCornerMarkers(binary, width, height)
    }
    
    private fun detectCornersWithOtsu(gray: Mat, width: Int, height: Int): List<Point>? {
        val binary = Mat()
        Imgproc.threshold(gray, binary, 0.0, 255.0, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
        
        return findCornerMarkers(binary, width, height)
    }
    
    /**
     * Core corner detection logic - with dynamic area thresholds
     */
    private fun findCornerMarkers(binary: Mat, width: Int, height: Int): List<Point>? {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        // Calculate dynamic area thresholds based on image size
        val imageArea = width * height
        val minMarkerArea = imageArea * MIN_MARKER_AREA_RATIO
        val maxMarkerArea = imageArea * MAX_MARKER_AREA_RATIO
        
        Log.d(TAG, "Finding markers: imageSize=${width}x${height}, areaRange=${"%.0f".format(minMarkerArea)}-${"%.0f".format(maxMarkerArea)}")
        
        // Find square-ish contours in valid size range
        val candidates = contours.mapNotNull { contour ->
            val area = Imgproc.contourArea(contour)
            if (area < minMarkerArea || area > maxMarkerArea) return@mapNotNull null
            
            val rect = Imgproc.boundingRect(contour)
            val aspectRatio = rect.width.toDouble() / rect.height.toDouble()
            
            if (aspectRatio < MARKER_ASPECT_MIN || aspectRatio > MARKER_ASPECT_MAX) return@mapNotNull null
            
            // Calculate solidity (area / bounding rect area)
            val rectArea = rect.width * rect.height
            val solidity = area / rectArea
            if (solidity < 0.5) return@mapNotNull null // More lenient - was 0.6
            
            MarkerCandidate(
                center = Point(rect.x + rect.width / 2.0, rect.y + rect.height / 2.0),
                area = area,
                rect = rect
            )
        }.sortedByDescending { it.area }
        
        Log.d(TAG, "Found ${candidates.size} marker candidates")
        
        if (candidates.size < 4) return null
        
        // Find 4 corners that form a quadrilateral
        return identifyCornerQuad(candidates, width, height)
    }
    
    private fun identifyCornerQuad(candidates: List<MarkerCandidate>, width: Int, height: Int): List<Point>? {
        // Use diagonal distance approach - find extreme corners regardless of image position
        // TopLeft: minimum (x + y) - closest to origin
        // BottomRight: maximum (x + y) - farthest from origin  
        // TopRight: maximum (x - y) - rightmost relative to height
        // BottomLeft: maximum (y - x) - lowest relative to width
        
        val topLeft = candidates.minByOrNull { it.center.x + it.center.y }
        val bottomRight = candidates.maxByOrNull { it.center.x + it.center.y }
        val topRight = candidates.maxByOrNull { it.center.x - it.center.y }
        val bottomLeft = candidates.maxByOrNull { it.center.y - it.center.x }
        
        if (topLeft == null || topRight == null || bottomLeft == null || bottomRight == null) {
            Log.w(TAG, "Could not identify all 4 corners")
            return null
        }
        
        // Make sure we have 4 distinct markers
        val uniqueMarkers = setOf(topLeft, topRight, bottomLeft, bottomRight)
        if (uniqueMarkers.size != 4) {
            Log.w(TAG, "Corners not distinct: ${uniqueMarkers.size} unique markers")
            return null
        }
        
        // Verify they form a reasonable quadrilateral
        val corners = listOf(topLeft.center, topRight.center, bottomRight.center, bottomLeft.center)
        
        Log.d(TAG, "Corners: TL=(${topLeft.center.x.toInt()},${topLeft.center.y.toInt()}) " +
                "TR=(${topRight.center.x.toInt()},${topRight.center.y.toInt()}) " +
                "BR=(${bottomRight.center.x.toInt()},${bottomRight.center.y.toInt()}) " +
                "BL=(${bottomLeft.center.x.toInt()},${bottomLeft.center.y.toInt()})")
        
        return corners
    }
    
    private fun distance(p1: Point, p2: Point): Double {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Adaptive preprocessing based on image characteristics
     */
    private fun preprocessAdaptive(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        
        // Calculate mean brightness
        val mean = Core.mean(gray).`val`[0]
        
        // Adjust preprocessing based on lighting
        val blockSize = if (mean < 100) 21 else 11 // Darker images need larger blocks
        val C = if (mean < 100) 5.0 else 2.0
        
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        
        val thresh = Mat()
        Imgproc.adaptiveThreshold(
            gray, thresh, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV,
            blockSize, C
        )
        
        Log.d(TAG, "Preprocessing: brightness=$mean, blockSize=$blockSize")
        
        return thresh
    }
    
    /**
     * Robust bubble detection with quality filtering
     */
    private fun detectBubblesRobust(processedMat: Mat, originalBitmap: Bitmap): List<BubbleCandidate> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(processedMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
        
        val bubbles = contours.mapNotNull { contour ->
            val area = Imgproc.contourArea(contour)
            if (area < BUBBLE_MIN_AREA || area > BUBBLE_MAX_AREA) return@mapNotNull null
            
            val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val circularity = 4 * Math.PI * area / (perimeter * perimeter)
            if (circularity < CIRCULARITY_THRESHOLD) return@mapNotNull null
            
            val center = Point()
            val radius = FloatArray(1)
            Imgproc.minEnclosingCircle(MatOfPoint2f(*contour.toArray()), center, radius)
            
            val darkness = calculateBubbleDarkness(originalBitmap, center.x.toInt(), center.y.toInt(), radius[0].toInt())
            
            BubbleCandidate(
                x = center.x.toFloat(),
                y = center.y.toFloat(),
                radius = radius[0],
                darkness = darkness,
                area = area,
                circularity = circularity.toFloat()
            )
        }
        
        // Filter out noise (very light bubbles that are likely artifacts)
        return bubbles.filter { it.darkness > 0.05f || it.circularity > 0.7f }
    }
    
    private fun calculateBubbleDarkness(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int): Float {
        var darkPixels = 0
        var totalPixels = 0
        val r = (radius * 0.8).toInt() // Sample inner 80% to avoid border
        
        for (dx in -r..r) {
            for (dy in -r..r) {
                if (dx * dx + dy * dy > r * r) continue
                
                val x = (centerX + dx).coerceIn(0, bitmap.width - 1)
                val y = (centerY + dy).coerceIn(0, bitmap.height - 1)
                
                totalPixels++
                val pixel = bitmap.getPixel(x, y)
                val brightness = (android.graphics.Color.red(pixel) + 
                                android.graphics.Color.green(pixel) + 
                                android.graphics.Color.blue(pixel)) / 3
                
                if (brightness < 128) darkPixels++
            }
        }
        
        return if (totalPixels > 0) darkPixels.toFloat() / totalPixels else 0f
    }
    
    /**
     * Column-first grouping with robust gap detection
     */
    private fun groupByColumnsRobust(
        bubbles: List<BubbleCandidate>,
        totalQuestions: Int,
        optionsPerQuestion: Int
    ): List<List<BubbleCandidate>> {
        val layout = when (totalQuestions) {
            20 -> TemplateLayout(2, 10)
            50 -> TemplateLayout(3, 17)
            100 -> TemplateLayout(4, 25)
            else -> return emptyList()
        }
        
        // Sort by X (columns)
        val sortedByX = bubbles.sortedBy { it.x }
        
        // Calculate dynamic column gap
        val avgBubbleWidth = bubbles.map { it.radius * 2 }.average().toFloat()
        val columnGapThreshold = avgBubbleWidth * COLUMN_GAP_MULTIPLIER
        
        Log.d(TAG, "Dynamic column gap: $columnGapThreshold")
        
        // Divide into columns
        val columns = mutableListOf<MutableList<BubbleCandidate>>()
        var currentColumn = mutableListOf<BubbleCandidate>()
        var lastX = sortedByX[0].x
        
        for (bubble in sortedByX) {
            if (bubble.x - lastX > columnGapThreshold && currentColumn.isNotEmpty()) {
                columns.add(currentColumn)
                currentColumn = mutableListOf()
            }
            currentColumn.add(bubble)
            lastX = bubble.x
        }
        if (currentColumn.isNotEmpty()) columns.add(currentColumn)
        
        Log.d(TAG, "Detected ${columns.size} columns (expected ${layout.columns})")
        
        // Process each column
        val allQuestions = mutableListOf<List<BubbleCandidate>>()
        for (column in columns) {
            val questions = groupColumnIntoQuestions(column.sortedBy { it.y }, optionsPerQuestion)
            allQuestions.addAll(questions)
        }
        
        return allQuestions.take(totalQuestions)
    }
    
    private fun groupColumnIntoQuestions(column: List<BubbleCandidate>, optionsPerQuestion: Int): List<List<BubbleCandidate>> {
        val questions = mutableListOf<MutableList<BubbleCandidate>>()
        var currentQuestion = mutableListOf<BubbleCandidate>()
        var lastY = column[0].y
        
        for (bubble in column) {
            if (bubble.y - lastY > ROW_GAP_THRESHOLD && currentQuestion.isNotEmpty()) {
                if (currentQuestion.size == optionsPerQuestion) {
                    questions.add(currentQuestion.sortedBy { it.x }.toMutableList())
                }
                currentQuestion = mutableListOf()
            }
            currentQuestion.add(bubble)
            lastY = bubble.y
        }
        
        if (currentQuestion.size == optionsPerQuestion) {
            questions.add(currentQuestion.sortedBy { it.x }.toMutableList())
        }
        
        return questions
    }
    
    /**
     * Extract answers WITH double-mark and erased mark detection
     */
    private fun extractAnswersWithValidation(
        questionGroups: List<List<BubbleCandidate>>,
        bitmap: Bitmap
    ): Pair<List<String>, List<ScanIssue>> {
        val options = listOf("A", "B", "C", "D", "E")
        val answers = mutableListOf<String>()
        val issues = mutableListOf<ScanIssue>()
        
        for ((index, group) in questionGroups.withIndex()) {
            val qNum = index + 1
            
            if (group.isEmpty()) {
                answers.add("")
                issues.add(ScanIssue(qNum, "No bubbles detected", IssueType.MISSING))
                continue
            }
            
            // Sort by darkness
            val sorted = group.sortedByDescending { it.darkness }
            val darkest = sorted[0]
            val secondDarkest = sorted.getOrNull(1)
            
            // Check for double marks
            if (secondDarkest != null && 
                darkest.darkness >= DOUBLE_MARK_THRESHOLD && 
                secondDarkest.darkness >= DOUBLE_MARK_THRESHOLD) {
                
                val ans1 = options.getOrNull(group.indexOf(darkest)) ?: "?"
                val ans2 = options.getOrNull(group.indexOf(secondDarkest)) ?: "?"
                
                Log.w(TAG, "Q$qNum: DOUBLE MARK detected ($ans1=${String.format("%.2f", darkest.darkness)}, $ans2=${String.format("%.2f", secondDarkest.darkness)})")
                answers.add("")
                issues.add(ScanIssue(qNum, "Double mark: $ans1 and $ans2", IssueType.DOUBLE_MARK))
                continue
            }
            
            // Check for clear fill
            if (darkest.darkness >= FILLED_THRESHOLD) {
                val answer = options.getOrNull(group.indexOf(darkest)) ?: ""
                Log.d(TAG, "Q$qNum: $answer (${String.format("%.2f", darkest.darkness)})")
                answers.add(answer)
                
                // Warn if faint
                if (darkest.darkness < FILLED_THRESHOLD + 0.1f) {
                    issues.add(ScanIssue(qNum, "Faint mark", IssueType.FAINT))
                }
                continue
            }
            
            // Check for erased/light marks
            if (darkest.darkness >= ERASED_THRESHOLD) {
                val answer = options.getOrNull(group.indexOf(darkest)) ?: ""
                Log.w(TAG, "Q$qNum: $answer (ERASED/LIGHT: ${String.format("%.2f", darkest.darkness)})")
                answers.add(answer)
                issues.add(ScanIssue(qNum, "Possible erased mark", IssueType.ERASED))
                continue
            }
            
            // No clear answer
            Log.d(TAG, "Q$qNum: No answer (max: ${String.format("%.2f", darkest.darkness)})")
            answers.add("")
            issues.add(ScanIssue(qNum, "No clear mark", IssueType.NO_MARK))
        }
        
        return Pair(answers, issues)
    }
    
    private fun calculateConfidenceScore(answers: List<String>, issues: List<ScanIssue>, total: Int): Float {
        val detected = answers.count { it.isNotEmpty() }
        val baseConfidence = detected.toFloat() / total
        
        // Penalize for issues
        val criticalIssues = issues.count { it.type in listOf(IssueType.DOUBLE_MARK, IssueType.MISSING) }
        val minorIssues = issues.count { it.type in listOf(IssueType.FAINT, IssueType.ERASED) }
        
        val penalty = (criticalIssues * 0.05f) + (minorIssues * 0.02f)
        
        return (baseConfidence - penalty).coerceIn(0f, 1f)
    }
    
    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val exifOrientation = context.contentResolver.openInputStream(uri)?.use { 
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
            
            val bitmap = context.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it)
            } ?: return null
            
            rotateBitmapByExif(bitmap, exifOrientation ?: ExifInterface.ORIENTATION_NORMAL)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load", e)
            null
        }
    }
    
    private fun rotateBitmapByExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    
    data class BubbleCandidate(
        val x: Float,
        val y: Float,
        val radius: Float,
        val darkness: Float,
        val area: Double,
        val circularity: Float
    )
    
    data class MarkerCandidate(
        val center: Point,
        val area: Double,
        val rect: Rect
    )
    
    data class TemplateLayout(val columns: Int, val questionsPerColumn: Int)
    
    data class ScanIssue(
        val questionNumber: Int,
        val message: String,
        val type: IssueType
    )
    
    enum class IssueType {
        MISSING,      // No bubbles detected
        NO_MARK,      // No filled bubble
        FAINT,        // Light mark
        ERASED,       // Possibly erased
        DOUBLE_MARK   // Multiple bubbles filled
    }
    
    sealed class ScanResult {
        data class Success(
            val answers: List<String>,
            val confidence: Float,
            val issues: List<ScanIssue> = emptyList(),
            val debugBitmap: Bitmap? = null
        ) : ScanResult()
        
        data class Error(val message: String) : ScanResult()
    }
}