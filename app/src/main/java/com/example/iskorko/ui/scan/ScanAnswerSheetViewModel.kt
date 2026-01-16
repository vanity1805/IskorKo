package com.example.iskorko.ui.scan

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ScanAnswerSheetViewModel : ViewModel() {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var examName = mutableStateOf("")
        private set
    
    var totalQuestions = mutableStateOf(0)
        private set
    
    var answerKey = mutableStateOf<List<String>>(emptyList())
        private set
    
    var capturedImage = mutableStateOf<Bitmap?>(null)
        private set
    
    var detectedAnswers = mutableStateOf<List<String>>(emptyList())
        private set
    
    var selectedStudent = mutableStateOf<String?>(null)
        private set
    
    var isProcessing = mutableStateOf(false)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    var scanStep = mutableStateOf(ScanStep.CAMERA)
        private set
    
    // Use ZipgradeStyleScanner instead of AnswerSheetProcessor
    private lateinit var imageProcessor: ZipgradeStyleScanner
    
    var detectionConfidence = mutableStateOf(0f)
        private set
    
    fun loadExamDetails(examId: String) {
        db.collection("exams")
            .document(examId)
            .get()
            .addOnSuccessListener { doc ->
                examName.value = doc.getString("examName") ?: ""
                totalQuestions.value = doc.getLong("totalQuestions")?.toInt() ?: 0
                answerKey.value = doc.get("answerKey") as? List<String> ?: emptyList()
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to load exam: ${e.message}"
            }
    }
    
    fun onImageCaptured(bitmap: Bitmap) {
        capturedImage.value = bitmap
        scanStep.value = ScanStep.PREVIEW
    }
    
    fun retakePhoto() {
        capturedImage.value = null
        detectedAnswers.value = emptyList()
        scanStep.value = ScanStep.CAMERA
    }
    
    fun processImage(context: Context) {
        isProcessing.value = true
        scanStep.value = ScanStep.PROCESSING
        
        val bitmap = capturedImage.value
        if (bitmap == null) {
            errorMessage.value = "No image to process"
            isProcessing.value = false
            return
        }
        
        // Initialize scanner if needed
        if (!::imageProcessor.isInitialized) {
            imageProcessor = ZipgradeStyleScanner(context)
        }
        
        // Process in background thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                android.util.Log.d("ScanViewModel", "=== STARTING ZIPGRADE SCAN ===")
                android.util.Log.d("ScanViewModel", "Image size: ${bitmap.width}x${bitmap.height}")
                android.util.Log.d("ScanViewModel", "Total questions: ${totalQuestions.value}")
                
                // Use Zipgrade scanner
                val result = imageProcessor.processAnswerSheet(
                    bitmap = bitmap,
                    totalQuestions = totalQuestions.value,
                    optionsPerQuestion = 5
                )
                
                when (result) {
                    is ZipgradeStyleScanner.ScanResult.Success -> {
                        detectedAnswers.value = result.answers.map { if (it.isEmpty()) "A" else it }
                        detectionConfidence.value = result.confidence
                        
                        // Log issues for review
                        result.issues.forEach { issue ->
                            android.util.Log.w("ScanViewModel", "Q${issue.questionNumber}: ${issue.message}")
                        }
                        
                        // Show warning if low confidence
                        if (result.confidence < 0.85f) {
                            android.util.Log.w("ScanViewModel", "⚠ Low confidence: ${result.issues.size} issues detected")
                        }
                        
                        isProcessing.value = false
                        scanStep.value = ScanStep.REVIEW
                    }
                    is ZipgradeStyleScanner.ScanResult.Error -> {
                        android.util.Log.e("ScanViewModel", "Scan error: ${result.message}")
                        errorMessage.value = result.message
                        isProcessing.value = false
                        scanStep.value = ScanStep.PREVIEW
                    }
                }
                
            } catch (e: Exception) {
                android.util.Log.e("ScanViewModel", "=== PROCESSING ERROR ===", e)
                errorMessage.value = "Processing failed: ${e.message}"
                isProcessing.value = false
                scanStep.value = ScanStep.PREVIEW
            }
        }
    }
    
    fun onAnswerChange(index: Int, answer: String) {
        if (index >= 0 && index < detectedAnswers.value.size) {
            val newAnswers = detectedAnswers.value.toMutableList()
            newAnswers[index] = answer
            detectedAnswers.value = newAnswers
        }
    }
    
    fun calculateScore(): Int {
        var correct = 0
        detectedAnswers.value.forEachIndexed { index, answer ->
            if (index < answerKey.value.size && answer == answerKey.value[index]) {
                correct++
            }
        }
        return correct
    }
    
    fun saveGrade(
        examId: String,
        classId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val studentId = selectedStudent.value
        if (studentId == null) {
            onError("Please select a student")
            return
        }
        
        val score = calculateScore()
        val percentage = if (totalQuestions.value > 0) (score.toFloat() / totalQuestions.value * 100).toInt() else 0
        
        // Check if grade already exists
        db.collection("grades")
            .whereEqualTo("examId", examId)
            .whereEqualTo("studentId", studentId)
            .get()
            .addOnSuccessListener { snapshot ->
                val gradeData = hashMapOf(
                    "examId" to examId,
                    "studentId" to studentId,
                    "score" to score,
                    "totalQuestions" to totalQuestions.value,
                    "answers" to detectedAnswers.value,
                    "gradedAt" to System.currentTimeMillis(),
                    "gradingMethod" to "scanned"
                )
                
                if (snapshot.isEmpty) {
                    // Create new grade
                    db.collection("grades")
                        .add(gradeData)
                        .addOnSuccessListener {
                            // Create notification for student
                            createNotificationForStudent(
                                studentId = studentId,
                                examName = examName.value,
                                score = score,
                                totalQuestions = totalQuestions.value,
                                percentage = percentage
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError("Failed to save grade: ${e.message}")
                        }
                } else {
                    // Update existing grade
                    val gradeId = snapshot.documents[0].id
                    db.collection("grades")
                        .document(gradeId)
                        .update(gradeData as Map<String, Any>)
                        .addOnSuccessListener {
                            // Create notification for student (grade updated)
                            createNotificationForStudent(
                                studentId = studentId,
                                examName = examName.value,
                                score = score,
                                totalQuestions = totalQuestions.value,
                                percentage = percentage,
                                isUpdate = true
                            )
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError("Failed to update grade: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                onError("Error: ${e.message}")
            }
    }
    
    private fun createNotificationForStudent(
        studentId: String,
        examName: String,
        score: Int,
        totalQuestions: Int,
        percentage: Int,
        isUpdate: Boolean = false
    ) {
        val notificationData = hashMapOf(
            "userId" to studentId,
            "type" to "NEW_GRADE",
            "title" to if (isUpdate) "Grade Updated" else "New Grade Available",
            "message" to "You scored $score/$totalQuestions ($percentage%) on $examName",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "relatedId" to null
        )
        
        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Notifications", "Grade notification created for student $studentId")
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Failed to create grade notification: ${e.message}")
            }
    }
    
    fun selectStudent(studentId: String) {
        selectedStudent.value = studentId
    }
}

enum class ScanStep {
    CAMERA,      // Taking photo
    PREVIEW,     // Preview captured image
    PROCESSING,  // Processing image
    REVIEW       // Review detected answers
}