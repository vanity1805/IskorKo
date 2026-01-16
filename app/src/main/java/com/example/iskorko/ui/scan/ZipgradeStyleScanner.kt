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
        
        // Bubble detection (adaptive) - more lenient for various bubble shapes
        private const val BUBBLE_MIN_AREA = 60.0          // Smaller minimum for smaller bubbles
        private const val BUBBLE_MAX_AREA = 4000.0        // Larger maximum
        private const val CIRCULARITY_THRESHOLD = 0.30f   // Very lenient - accept any roundish shape
        
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
        Log.d(TAG, "")
        Log.d(TAG, "╔═══════════════════════════════════════════════════════════")
        Log.d(TAG, "║ SCAN START: $totalQuestions questions, Image: ${bitmap.width}x${bitmap.height}")
        Log.d(TAG, "╠═══════════════════════════════════════════════════════════")
        
        if (totalQuestions !in listOf(20, 50, 100)) {
            return ScanResult.Error("Only 20, 50, or 100 questions supported")
        }
        
        return try {
            // Step 1: Robust corner detection with fallback
            Log.d(TAG, "║ STEP 1: Corner Detection...")
            val (correctedBitmap, corners) = findAndCorrectPerspective(bitmap)
            Log.d(TAG, "║   → Corners found: ${corners?.size ?: "NONE"}")
            Log.d(TAG, "║   → Corrected image: ${correctedBitmap.width}x${correctedBitmap.height}")
            
            // Step 2: Adaptive preprocessing based on lighting
            Log.d(TAG, "║ STEP 2: Preprocessing...")
            val processedMat = preprocessAdaptive(correctedBitmap)
            Log.d(TAG, "║   → Preprocessed mat size: ${processedMat.cols()}x${processedMat.rows()}")
            
            // Step 3: Detect bubbles with quality filtering
            Log.d(TAG, "║ STEP 3: Bubble Detection...")
            val bubbles = detectBubblesRobust(processedMat, correctedBitmap)
            Log.d(TAG, "║   → Detected ${bubbles.size} bubbles")
            
            if (bubbles.isEmpty()) {
                Log.e(TAG, "║ ERROR: No bubbles detected!")
                Log.d(TAG, "╚═══════════════════════════════════════════════════════════")
                return ScanResult.Error("No bubbles detected. Check lighting and focus.")
            }
            
            // Log bubble distribution for debugging
            Log.d(TAG, "║   → Bubble distribution:")
            val minX = bubbles.minOfOrNull { it.x }?.toInt() ?: 0
            val maxX = bubbles.maxOfOrNull { it.x }?.toInt() ?: 0
            val minY = bubbles.minOfOrNull { it.y }?.toInt() ?: 0
            val maxY = bubbles.maxOfOrNull { it.y }?.toInt() ?: 0
            Log.d(TAG, "║       X range: $minX - $maxX (span: ${maxX - minX})")
            Log.d(TAG, "║       Y range: $minY - $maxY (span: ${maxY - minY})")
            Log.d(TAG, "║   → First 10 bubble positions:")
            bubbles.take(10).forEachIndexed { i, b ->
                Log.d(TAG, "║       [$i] x=${b.x.toInt()}, y=${b.y.toInt()}, darkness=${String.format("%.2f", b.darkness)}")
            }
            
            // Step 4: Column-first grouping with validation (now uses row timing marks!)
            Log.d(TAG, "║ STEP 4: Grouping into Questions...")
            val questionGroups = groupByColumnsRobust(bubbles, totalQuestions, optionsPerQuestion, correctedBitmap)
            Log.d(TAG, "║   → Grouped into ${questionGroups.size} questions")
            
            // Step 5: Extract answers with double-mark detection
            Log.d(TAG, "║ STEP 5: Extracting Answers...")
            val (answers, issues) = extractAnswersWithValidation(questionGroups, correctedBitmap)
            Log.d(TAG, "║   → Extracted ${answers.size} answers")
            Log.d(TAG, "║   → Issues: ${issues.size}")
            
            // Log first 20 answers
            Log.d(TAG, "║   → Answers (first 20):")
            answers.take(20).forEachIndexed { i, ans ->
                Log.d(TAG, "║       Q${i+1}: $ans")
            }
            
            // Step 6: Calculate confidence with validation
            val confidence = calculateConfidenceScore(answers, issues, totalQuestions)
            
            Log.d(TAG, "╠═══════════════════════════════════════════════════════════")
            Log.d(TAG, "║ SCAN COMPLETE: ${answers.size}/$totalQuestions, Confidence: ${String.format("%.1f%%", confidence * 100)}")
            Log.d(TAG, "╚═══════════════════════════════════════════════════════════")
            
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
     * Template-based bubble detection for known grid layouts
     */
    private fun detectBubblesRobust(processedMat: Mat, originalBitmap: Bitmap): List<BubbleCandidate> {
        Log.d(TAG, "║     Bubble Detection Details:")
        
        val imageWidth = originalBitmap.width
        val imageHeight = originalBitmap.height
        
        Log.d(TAG, "║     - Image size: ${imageWidth}x${imageHeight}")
        
        // Use template grid matching for the known IskorKo layout
        return detectBubblesWithTemplateGrid(originalBitmap, processedMat)
    }
    
    /**
     * Template grid-based bubble detection - uses known layout to locate bubbles
     */
    private fun detectBubblesWithTemplateGrid(bitmap: Bitmap, processedMat: Mat): List<BubbleCandidate> {
        val width = bitmap.width
        val height = bitmap.height
        
        // IskorKo template layout based on the provided images:
        // For 20 questions: 2 columns, 10 rows, 5 options per row
        // The bubble area starts after header (~15% from top) and ends before footer (~85% from top)
        // Columns take about 80% of width, centered
        
        // Tighter margins to exclude timing marks and edge artifacts
        val headerRatio = 0.20f   // Skip header + top timing marks
        val footerRatio = 0.92f   // Skip footer + bottom timing marks  
        val leftMarginRatio = 0.12f  // Skip left timing marks
        val rightMarginRatio = 0.88f // Skip right timing marks
        
        val bubbleAreaTop = (height * headerRatio).toInt()
        val bubbleAreaBottom = (height * footerRatio).toInt()
        val bubbleAreaLeft = (width * leftMarginRatio).toInt()
        val bubbleAreaRight = (width * rightMarginRatio).toInt()
        
        val bubbleAreaHeight = bubbleAreaBottom - bubbleAreaTop
        val bubbleAreaWidth = bubbleAreaRight - bubbleAreaLeft
        
        Log.d(TAG, "║     - Bubble area: ($bubbleAreaLeft,$bubbleAreaTop) to ($bubbleAreaRight,$bubbleAreaBottom)")
        Log.d(TAG, "║     - Bubble area size: ${bubbleAreaWidth}x${bubbleAreaHeight}")
        
        // Convert to grayscale for analysis
        val grayMat = Mat()
        Utils.bitmapToMat(bitmap, grayMat)
        Imgproc.cvtColor(grayMat, grayMat, Imgproc.COLOR_RGB2GRAY)
        
        // For a standard answer sheet, we'll use HoughCircles but filter by expected positions
        Imgproc.GaussianBlur(grayMat, grayMat, org.opencv.core.Size(9.0, 9.0), 2.0)
        
        val expectedBubbleRadius = (bubbleAreaWidth / 30.0).toInt().coerceIn(12, 35)
        val minRadius = (expectedBubbleRadius * 0.6).toInt()
        val maxRadius = (expectedBubbleRadius * 1.5).toInt()
        
        Log.d(TAG, "║     - Expected bubble radius: $expectedBubbleRadius (range: $minRadius-$maxRadius)")
        
        val circles = Mat()
        Imgproc.HoughCircles(
            grayMat,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            (minRadius * 2.0),  // Minimum distance between circles
            80.0,               // Canny threshold
            25.0,               // Accumulator threshold (balanced)
            minRadius,
            maxRadius
        )
        
        Log.d(TAG, "║     - HoughCircles found: ${circles.cols()} raw circles")
        
        // Filter circles to only those in the bubble area
        val bubbles = mutableListOf<BubbleCandidate>()
        
        for (i in 0 until circles.cols()) {
            val circleData = circles.get(0, i)
            val centerX = circleData[0].toFloat()
            val centerY = circleData[1].toFloat()
            val radius = circleData[2].toFloat()
            
            // Only keep circles within the bubble area
            if (centerX >= bubbleAreaLeft && centerX <= bubbleAreaRight &&
                centerY >= bubbleAreaTop && centerY <= bubbleAreaBottom) {
                
                val darkness = calculateBubbleDarkness(bitmap, centerX.toInt(), centerY.toInt(), radius.toInt())
                
                bubbles.add(BubbleCandidate(
                    x = centerX,
                    y = centerY,
                    radius = radius,
                    darkness = darkness,
                    area = (Math.PI * radius * radius),
                    circularity = 1.0f
                ))
            }
        }
        
        Log.d(TAG, "║     - Bubbles in valid area: ${bubbles.size}")
        
        // If not enough bubbles found, try contour fallback
        if (bubbles.size < 50) {
            Log.d(TAG, "║     - Trying contour fallback...")
            val contourBubbles = detectBubblesWithContours(processedMat, bitmap)
                .filter { 
                    it.x >= bubbleAreaLeft && it.x <= bubbleAreaRight &&
                    it.y >= bubbleAreaTop && it.y <= bubbleAreaBottom 
                }
            
            if (contourBubbles.size > bubbles.size) {
                Log.d(TAG, "║     - Contour found: ${contourBubbles.size} bubbles (before NMS)")
                val nmsResult = applyNonMaxSuppression(contourBubbles, expectedBubbleRadius.toFloat())
                Log.d(TAG, "║     - After NMS: ${nmsResult.size} bubbles")
                return nmsResult
            }
        }
        
        // Apply NMS to HoughCircles result as well
        val nmsResult = applyNonMaxSuppression(bubbles, expectedBubbleRadius.toFloat())
        Log.d(TAG, "║     - Final bubble count (after NMS): ${nmsResult.size}")
        return nmsResult
    }
    
    /**
     * Non-Maximum Suppression to merge overlapping bubble detections
     */
    private fun applyNonMaxSuppression(bubbles: List<BubbleCandidate>, minDistance: Float): List<BubbleCandidate> {
        if (bubbles.isEmpty()) return bubbles
        
        // Sort by area (larger bubbles first - they're likely the main detection)
        val sorted = bubbles.sortedByDescending { it.area }
        val kept = mutableListOf<BubbleCandidate>()
        val suppressed = BooleanArray(sorted.size)
        
        for (i in sorted.indices) {
            if (suppressed[i]) continue
            
            kept.add(sorted[i])
            
            // Suppress all nearby bubbles
            for (j in i + 1 until sorted.size) {
                if (suppressed[j]) continue
                
                val dx = sorted[i].x - sorted[j].x
                val dy = sorted[i].y - sorted[j].y
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                
                if (distance < minDistance) {
                    suppressed[j] = true
                }
            }
        }
        
        return kept
    }
    
    /**
     * Fallback contour-based bubble detection
     */
    private fun detectBubblesWithContours(processedMat: Mat, originalBitmap: Bitmap): List<BubbleCandidate> {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(processedMat, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        Log.d(TAG, "║     Contour Fallback:")
        Log.d(TAG, "║     - Total contours found: ${contours.size}")
        
        var rejectedByArea = 0
        var rejectedByCircularity = 0
        
        val bubbles = contours.mapNotNull { contour ->
            val area = Imgproc.contourArea(contour)
            if (area < BUBBLE_MIN_AREA || area > BUBBLE_MAX_AREA) {
                rejectedByArea++
                return@mapNotNull null
            }
            
            val perimeter = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val circularity = 4 * Math.PI * area / (perimeter * perimeter)
            if (circularity < CIRCULARITY_THRESHOLD) {
                rejectedByCircularity++
                return@mapNotNull null
            }
            
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
        
        Log.d(TAG, "║     - Rejected by area: $rejectedByArea")
        Log.d(TAG, "║     - Rejected by circularity: $rejectedByCircularity")
        Log.d(TAG, "║     - Contour bubbles found: ${bubbles.size}")
        
        return bubbles
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
     * NEW: Detect row timing marks on the left edge of each column
     * These are small black squares that mark each question row
     * 
     * Strategy: Find all small solid squares, then filter by position
     */
    private fun detectRowTimingMarks(bitmap: Bitmap, layout: TemplateLayout): List<Point> {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGB2GRAY)
        
        // Apply threshold to get black marks
        val binary = Mat()
        Imgproc.threshold(gray, binary, 100.0, 255.0, Imgproc.THRESH_BINARY_INV)
        
        // Find contours
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        
        // Row timing marks: larger than bubbles but smaller than corner marks
        val imageArea = bitmap.width * bitmap.height
        val minMarkArea = imageArea * 0.0005  // ~500 pixels for 1000x1000 image
        val maxMarkArea = imageArea * 0.008   // ~8000 pixels (smaller than corner marks)
        
        Log.d(TAG, "║     Timing mark detection:")
        Log.d(TAG, "║     - Area range: ${minMarkArea.toInt()} - ${maxMarkArea.toInt()}")
        
        val allSquareMarks = mutableListOf<Point>()
        
        for (contour in contours) {
            val area = Imgproc.contourArea(contour)
            if (area < minMarkArea || area > maxMarkArea) continue
            
            val rect = Imgproc.boundingRect(contour)
            val aspectRatio = rect.width.toFloat() / rect.height.toFloat()
            
            // Must be roughly square
            if (aspectRatio < 0.5 || aspectRatio > 2.0) continue
            
            // Must be filled (solid black)
            val fillRatio = area / (rect.width * rect.height)
            if (fillRatio < 0.65) continue
            
            val centerX = rect.x + rect.width / 2.0
            val centerY = rect.y + rect.height / 2.0
            
            // Not too close to corners (avoid corner timing marks)
            val cornerMargin = bitmap.width * 0.08
            val isNearCorner = (centerX < cornerMargin || centerX > bitmap.width - cornerMargin) &&
                              (centerY < cornerMargin || centerY > bitmap.height - cornerMargin)
            if (isNearCorner) continue
            
            // Only keep marks in the vertical middle (avoid header/footer)
            if (centerY < bitmap.height * 0.18 || centerY > bitmap.height * 0.85) continue
            
            allSquareMarks.add(Point(centerX, centerY))
        }
        
        Log.d(TAG, "║     - All square marks found: ${allSquareMarks.size}")
        
        if (allSquareMarks.isEmpty()) {
            gray.release()
            binary.release()
            mat.release()
            return emptyList()
        }
        
        // Now filter to keep only marks that are to the LEFT of bubbles (timing marks)
        // Sort by X to find the leftmost marks in each row
        val sortedByX = allSquareMarks.sortedBy { it.x }
        
        // Find the bubble area (rightmost marks are bubbles, leftmost are timing marks)
        val medianX = sortedByX[sortedByX.size / 2].x
        
        // Timing marks should be to the left of the median X
        // But we need to handle 2 columns - so we look for marks in the left 40% of each column half
        val halfWidth = bitmap.width / 2.0
        
        val rowTimingMarks = allSquareMarks.filter { mark ->
            // Column 1 timing marks: left 20% of left half
            val isCol1Mark = mark.x < halfWidth * 0.35
            // Column 2 timing marks: left 20% of right half  
            val isCol2Mark = mark.x > halfWidth * 0.5 && mark.x < halfWidth + halfWidth * 0.35
            
            isCol1Mark || isCol2Mark
        }
        
        Log.d(TAG, "║     - Row timing marks filtered: ${rowTimingMarks.size}")
        if (rowTimingMarks.isNotEmpty()) {
            Log.d(TAG, "║       X range: ${rowTimingMarks.minOf { it.x }.toInt()} - ${rowTimingMarks.maxOf { it.x }.toInt()}")
            Log.d(TAG, "║       Y positions: ${rowTimingMarks.sortedBy { it.y }.take(10).map { it.y.toInt() }.joinToString(", ")}...")
        }
        
        gray.release()
        binary.release()
        mat.release()
        
        return rowTimingMarks.sortedBy { it.y }
    }
    
    /**
     * NEW: Use row timing marks to locate rows, then find bubbles to the right of each mark
     */
    private fun groupByRowTimingMarks(
        rowMarks: List<Point>,
        bubbles: List<BubbleCandidate>,
        layout: TemplateLayout,
        imageWidth: Int,
        optionsPerQuestion: Int
    ): List<List<BubbleCandidate>> {
        if (rowMarks.isEmpty()) return emptyList()
        
        Log.d(TAG, "║     Using ${rowMarks.size} row timing marks for grouping")
        
        val avgBubbleWidth = bubbles.map { it.radius * 2 }.average().toFloat()
        val rowTolerance = avgBubbleWidth * 0.8f  // How close bubble Y must be to mark Y
        
        val questions = mutableListOf<List<BubbleCandidate>>()
        
        for (mark in rowMarks) {
            // Find bubbles to the RIGHT of this mark and at the same Y level
            val rowBubbles = bubbles.filter { bubble ->
                bubble.x > mark.x && // To the right of mark
                bubble.x < mark.x + (imageWidth / layout.columns) * 0.9 && // Within the column
                abs(bubble.y - mark.y) < rowTolerance // Same row
            }.sortedBy { it.x }
            
            if (rowBubbles.size >= 3) { // Accept if at least 3 bubbles found
                // Select exactly optionsPerQuestion bubbles if too many
                val finalBubbles = if (rowBubbles.size > optionsPerQuestion) {
                    selectBestBubbles(rowBubbles, optionsPerQuestion)
                } else {
                    rowBubbles.toMutableList()
                }
                questions.add(finalBubbles)
            }
        }
        
        Log.d(TAG, "║     - Questions from timing marks: ${questions.size}")
        return questions
    }
    
    /**
     * Column-first grouping with robust gap detection
     * NOW: First tries row timing marks, falls back to Y-clustering
     */
    private fun groupByColumnsRobust(
        bubbles: List<BubbleCandidate>,
        totalQuestions: Int,
        optionsPerQuestion: Int,
        originalBitmap: Bitmap? = null
    ): List<List<BubbleCandidate>> {
        val layout = when (totalQuestions) {
            20 -> TemplateLayout(2, 10)
            50 -> TemplateLayout(3, 17)
            100 -> TemplateLayout(4, 25)
            else -> return emptyList()
        }
        
        Log.d(TAG, "║     Grouping Details:")
        Log.d(TAG, "║     - Expected layout: ${layout.columns} columns x ${layout.questionsPerColumn} questions/column")
        Log.d(TAG, "║     - Total bubbles to group: ${bubbles.size}")
        
        if (bubbles.isEmpty()) {
            Log.e(TAG, "║     - ERROR: No bubbles to group!")
            return emptyList()
        }
        
        // NEW: Try to use row timing marks first (more accurate)
        if (originalBitmap != null) {
            val rowMarks = detectRowTimingMarks(originalBitmap, layout)
            if (rowMarks.size >= totalQuestions * 0.7) { // If we found at least 70% of expected marks
                Log.d(TAG, "║     - Using ROW TIMING MARKS for grouping (found ${rowMarks.size}/${totalQuestions})")
                val questions = groupByRowTimingMarks(rowMarks, bubbles, layout, originalBitmap.width, optionsPerQuestion)
                if (questions.size >= totalQuestions * 0.7) {
                    return questions.take(totalQuestions)
                }
                Log.d(TAG, "║     - Timing mark grouping insufficient (${questions.size}), falling back to Y-clustering")
            }
        }
        
        Log.d(TAG, "║     - Using Y-CLUSTERING fallback for grouping")
        
        // Sort by X first
        val sortedByX = bubbles.sortedBy { it.x }
        val avgBubbleWidth = bubbles.map { it.radius * 2 }.average().toFloat()
        
        Log.d(TAG, "║     - Avg bubble width: ${String.format("%.1f", avgBubbleWidth)}")
        
        // Use K-means style clustering to find columns based on expected count
        val columns = clusterByXPosition(bubbles, layout.columns, avgBubbleWidth)
        
        Log.d(TAG, "║     - Detected ${columns.size} columns (expected ${layout.columns})")
        columns.forEachIndexed { i, col ->
            if (col.isNotEmpty()) {
                Log.d(TAG, "║       Column $i: ${col.size} bubbles, X range: ${col.minOf { it.x }.toInt()}-${col.maxOf { it.x }.toInt()}")
            }
        }
        
        // Process each column into rows/questions
        val allQuestions = mutableListOf<List<BubbleCandidate>>()
        for ((colIndex, column) in columns.withIndex()) {
            if (column.isEmpty()) continue
            
            // Sort column by Y, then group by rows
            val sortedByY = column.sortedBy { it.y }
            val questions = groupColumnIntoRows(sortedByY, optionsPerQuestion, avgBubbleWidth)
            Log.d(TAG, "║       Column $colIndex → ${questions.size} questions")
            allQuestions.addAll(questions)
        }
        
        Log.d(TAG, "║     - Total questions grouped: ${allQuestions.size}")
        return allQuestions.take(totalQuestions)
    }
    
    /**
     * Cluster bubbles into columns using their X positions
     */
    private fun clusterByXPosition(bubbles: List<BubbleCandidate>, expectedColumns: Int, avgBubbleWidth: Float): List<List<BubbleCandidate>> {
        if (bubbles.isEmpty()) return emptyList()
        
        val sortedByX = bubbles.sortedBy { it.x }
        val minX = sortedByX.first().x
        val maxX = sortedByX.last().x
        val totalWidth = maxX - minX
        
        // Estimate column width
        val columnWidth = totalWidth / expectedColumns
        
        Log.d(TAG, "║     - X range: ${minX.toInt()}-${maxX.toInt()}, columnWidth: ${columnWidth.toInt()}")
        
        // Assign each bubble to a column based on its X position
        val columns = List(expectedColumns) { mutableListOf<BubbleCandidate>() }
        
        for (bubble in bubbles) {
            val columnIndex = ((bubble.x - minX) / columnWidth).toInt().coerceIn(0, expectedColumns - 1)
            columns[columnIndex].add(bubble)
        }
        
        return columns
    }
    
    /**
     * Group a column of bubbles into rows (questions)
     * For each row, select exactly 5 bubbles that are most aligned horizontally
     */
    private fun groupColumnIntoRows(sortedByY: List<BubbleCandidate>, optionsPerQuestion: Int, avgBubbleWidth: Float): List<List<BubbleCandidate>> {
        if (sortedByY.isEmpty()) return emptyList()
        
        // Row gap: Use the vertical spacing between bubbles
        // For a standard answer sheet, row height is typically 1.5-2x bubble diameter
        // But within a row, bubbles are tightly packed (gap < 0.5x diameter)
        val rowGapThreshold = avgBubbleWidth * 0.6f  // Reduced to catch smaller gaps
        
        Log.d(TAG, "║         Row grouping: ${sortedByY.size} bubbles, gap threshold: ${String.format("%.1f", rowGapThreshold)}")
        
        val rows = mutableListOf<MutableList<BubbleCandidate>>()
        var currentRow = mutableListOf<BubbleCandidate>()
        var lastY = sortedByY[0].y
        
        for (bubble in sortedByY) {
            val yGap = bubble.y - lastY
            
            if (yGap > rowGapThreshold && currentRow.isNotEmpty()) {
                // Save current row
                rows.add(currentRow.sortedBy { it.x }.toMutableList())
                currentRow = mutableListOf()
            }
            
            currentRow.add(bubble)
            lastY = bubble.y
        }
        
        // Don't forget the last row
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.sortedBy { it.x }.toMutableList())
        }
        
        Log.d(TAG, "║         Found ${rows.size} raw rows with sizes: ${rows.map { it.size }.joinToString(", ")}")
        
        // For each row, if it has more than 5 bubbles, select the 5 most evenly spaced
        // If it has fewer than 5, keep it if it has at least 4
        val normalizedRows = rows.mapNotNull { row ->
            when {
                row.size == optionsPerQuestion -> row // Perfect!
                row.size > optionsPerQuestion -> {
                    // Too many bubbles - select the 5 with best horizontal spacing
                    selectBestBubbles(row, optionsPerQuestion)
                }
                row.size >= optionsPerQuestion - 1 -> row // Accept 4 bubbles (one might be missing)
                else -> null // Too few bubbles
            }
        }
        
        Log.d(TAG, "║         Normalized rows: ${normalizedRows.size} (sizes: ${normalizedRows.map { it.size }.joinToString(", ")})")
        
        return normalizedRows
    }
    
    /**
     * Select the best N bubbles from a row based on even horizontal spacing
     */
    private fun selectBestBubbles(row: List<BubbleCandidate>, count: Int): MutableList<BubbleCandidate> {
        if (row.size <= count) return row.toMutableList()
        
        val sorted = row.sortedBy { it.x }
        
        // Calculate expected spacing based on first and last bubble
        val totalWidth = sorted.last().x - sorted.first().x
        val expectedSpacing = totalWidth / (count - 1)
        
        // Greedily select bubbles closest to expected positions
        val selected = mutableListOf<BubbleCandidate>()
        selected.add(sorted.first()) // Always include first
        
        for (i in 1 until count - 1) {
            val expectedX = sorted.first().x + i * expectedSpacing
            val closest = sorted.filter { it !in selected }
                .minByOrNull { kotlin.math.abs(it.x - expectedX) }
            if (closest != null) {
                selected.add(closest)
            }
        }
        
        selected.add(sorted.last()) // Always include last
        
        return selected.sortedBy { it.x }.toMutableList()
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