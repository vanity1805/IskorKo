package com.example.iskorko.ui.dashboard

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class StudentClassItem(
    val id: String = "",
    val className: String = "",
    val section: String = "",
    val classCode: String = "",
    val professorName: String = "",
    val examCount: Int = 0
)

// Student Grade Item
data class StudentGradeItem(
    val id: String = "",
    val examId: String = "",
    val examName: String = "",
    val className: String = "",
    val classId: String = "",
    val score: Int = 0,
    val totalPoints: Int = 0,
    val percentage: Float = 0f,
    val gradedDate: Long = 0L,
    val answers: List<String> = emptyList(),
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val unanswered: Int = 0,
    val rank: Int = 0,  // Student's rank in class for this exam
    val classAverage: Float = 0f  // Class average for comparison
)

// Student Statistics
data class StudentStatistics(
    val totalExamsTaken: Int = 0,
    val averageScore: Float = 0f,
    val highestScore: Float = 0f,
    val lowestScore: Float = 0f,
    val totalClasses: Int = 0,
    val perfectScores: Int = 0,  // Number of 100% scores
    val improvementTrend: Float = 0f  // Positive = improving, negative = declining
)

// Grade grouped by class
data class StudentClassGrades(
    val className: String = "",
    val classId: String = "",
    val averageScore: Float = 0f,
    val examCount: Int = 0,
    val grades: List<StudentGradeItem> = emptyList()
)

// Sort options for student grades
enum class StudentGradeSortOption {
    DATE_DESC,
    DATE_ASC,
    SCORE_DESC,
    SCORE_ASC,
    EXAM_NAME,
    CLASS_NAME
}

class StudentDashboardViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var studentName = mutableStateOf("Student")
        private set
    
    var studentEmail = mutableStateOf("")
        private set
    
    var profilePictureUrl = mutableStateOf<String?>(null)
        private set
    
    var isUploadingPhoto = mutableStateOf(false)
        private set
    
    var classes = mutableStateOf<List<StudentClassItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    // Grade-related states
    var allGrades = mutableStateOf<List<StudentGradeItem>>(emptyList())
        private set
    
    var studentStatistics = mutableStateOf(StudentStatistics())
        private set
    
    var classGrades = mutableStateOf<List<StudentClassGrades>>(emptyList())
        private set
    
    var isLoadingGrades = mutableStateOf(false)
        private set
    
    var selectedSortOption = mutableStateOf(StudentGradeSortOption.DATE_DESC)
        private set
    
    var searchQuery = mutableStateOf("")
        private set
    
    var selectedClassFilter = mutableStateOf<String?>(null)
        private set
    
    private var classesListener: ListenerRegistration? = null
    private var gradesListener: ListenerRegistration? = null
    private var gradeResultsListeners = mutableListOf<ListenerRegistration>()
    private var notificationsListener: ListenerRegistration? = null
    
    var notifications = mutableStateOf<List<NotificationItem>>(emptyList())
        private set
    
    var unreadNotificationCount = mutableStateOf(0)
        private set
    
    init {
        loadStudentData()
        loadClasses()
        loadStudentGrades()
        loadNotifications()
    }
    
    private fun loadStudentData() {
        val uid = auth.currentUser?.uid ?: return
        
        // Get email from auth
        studentEmail.value = auth.currentUser?.email ?: ""
        
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                studentName.value = document.getString("fullName") ?: "Student"
                profilePictureUrl.value = document.getString("profilePictureUrl")
            }
            .addOnFailureListener {
                studentName.value = "Student"
            }
    }
    
    private fun loadClasses() {
        val uid = auth.currentUser?.uid ?: return
        
        // Find all classes where this student is enrolled
        classesListener = db.collection("classes")
            .whereArrayContains("studentIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading.value = false
                    return@addSnapshotListener
                }
                
                val classIds = snapshot?.documents?.map { it.id } ?: emptyList()
                
                if (classIds.isEmpty()) {
                    classes.value = emptyList()
                    isLoading.value = false
                    return@addSnapshotListener
                }
                
                // Load full class details with professor names
                loadClassDetails(classIds)
            }
    }
    
    private fun loadClassDetails(classIds: List<String>) {
        val loadedClasses = mutableListOf<StudentClassItem>()
        var processedCount = 0
        
        classIds.forEach { classId ->
            db.collection("classes").document(classId).get()
                .addOnSuccessListener { doc ->
                    val professorId = doc.getString("professorId") ?: ""
                    
                    // Load exam count for this class
                    db.collection("exams")
                        .whereEqualTo("classId", classId)
                        .get()
                        .addOnSuccessListener { examSnapshot ->
                            // Load professor name
                            db.collection("users").document(professorId).get()
                                .addOnSuccessListener { profDoc ->
                                    loadedClasses.add(
                                        StudentClassItem(
                                            id = doc.id,
                                            className = doc.getString("className") ?: "",
                                            section = doc.getString("section") ?: "",
                                            classCode = doc.getString("classCode") ?: "",
                                            professorName = profDoc.getString("fullName") ?: "Unknown",
                                            examCount = examSnapshot.size()
                                        )
                                    )
                                    
                                    processedCount++
                                    if (processedCount == classIds.size) {
                                        classes.value = loadedClasses.sortedBy { it.className }
                                        isLoading.value = false
                                    }
                                }
                                .addOnFailureListener {
                                    loadedClasses.add(
                                        StudentClassItem(
                                            id = doc.id,
                                            className = doc.getString("className") ?: "",
                                            section = doc.getString("section") ?: "",
                                            classCode = doc.getString("classCode") ?: "",
                                            professorName = "Unknown",
                                            examCount = examSnapshot.size()
                                        )
                                    )
                                    
                                    processedCount++
                                    if (processedCount == classIds.size) {
                                        classes.value = loadedClasses.sortedBy { it.className }
                                        isLoading.value = false
                                    }
                                }
                        }
                        .addOnFailureListener {
                            // If exam count fails, load with 0 exams
                            db.collection("users").document(professorId).get()
                                .addOnSuccessListener { profDoc ->
                                    loadedClasses.add(
                                        StudentClassItem(
                                            id = doc.id,
                                            className = doc.getString("className") ?: "",
                                            section = doc.getString("section") ?: "",
                                            classCode = doc.getString("classCode") ?: "",
                                            professorName = profDoc.getString("fullName") ?: "Unknown",
                                            examCount = 0
                                        )
                                    )
                                    
                                    processedCount++
                                    if (processedCount == classIds.size) {
                                        classes.value = loadedClasses.sortedBy { it.className }
                                        isLoading.value = false
                                    }
                                }
                                .addOnFailureListener {
                                    processedCount++
                                    if (processedCount == classIds.size) {
                                        classes.value = loadedClasses.sortedBy { it.className }
                                        isLoading.value = false
                                    }
                                }
                        }
                }
                .addOnFailureListener {
                    processedCount++
                    if (processedCount == classIds.size) {
                        classes.value = loadedClasses.sortedBy { it.className }
                        isLoading.value = false
                    }
                }
        }
    }
    
    fun joinClass(
        classCode: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (classCode.isBlank()) {
            onError("Please enter a class code")
            return
        }
        
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not authenticated")
            return
        }
        
        // Find class by code
        db.collection("classes")
            .whereEqualTo("classCode", classCode.trim().uppercase())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onError("Invalid class code")
                    return@addOnSuccessListener
                }
                
                val classDoc = documents.documents[0]
                val studentIds = classDoc.get("studentIds") as? List<String> ?: emptyList()
                
                // Check if already enrolled
                if (studentIds.contains(uid)) {
                    onError("You are already enrolled in this class")
                    return@addOnSuccessListener
                }
                
                // Add student to class
                val className = classDoc.getString("className") ?: "Class"
                val professorId = classDoc.getString("professorId") ?: ""
                
                db.collection("classes")
                    .document(classDoc.id)
                    .update("studentIds", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener {
                        // Create notification for professor
                        if (professorId.isNotEmpty()) {
                            createNotificationForUser(
                                userId = professorId,
                                type = "NEW_STUDENT",
                                title = "New Student Joined",
                                message = "${studentName.value} joined $className",
                                relatedId = classDoc.id
                            )
                        }
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to join class: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Error: ${e.message}")
            }
    }
    
    fun leaveClass(
        classId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onError("User not authenticated")
            return
        }
        
        db.collection("classes")
            .document(classId)
            .update("studentIds", FieldValue.arrayRemove(uid))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to leave class: ${e.message}")
            }
    }
    
    fun logout(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
    
    private fun createNotificationForUser(
        userId: String,
        type: String,
        title: String,
        message: String,
        relatedId: String? = null
    ) {
        val notificationData = hashMapOf(
            "userId" to userId,
            "type" to type,
            "title" to title,
            "message" to message,
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false,
            "relatedId" to relatedId
        )
        
        db.collection("notifications")
            .add(notificationData)
            .addOnSuccessListener {
                Log.d("Notifications", "Notification created for user $userId")
            }
            .addOnFailureListener { e ->
                Log.e("Notifications", "Failed to create notification: ${e.message}")
            }
    }
    
    // ==================== PROFILE FUNCTIONALITY ====================
    
    fun updateProfile(
        newName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: run {
            onError("User not authenticated")
            return
        }
        
        if (newName.isBlank()) {
            onError("Name cannot be empty")
            return
        }
        
        db.collection("users")
            .document(uid)
            .update("fullName", newName.trim())
            .addOnSuccessListener {
                studentName.value = newName.trim()
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to update profile: ${e.message}")
            }
    }
    
    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("User not authenticated")
            return
        }
        
        val email = user.email ?: run {
            onError("Email not available")
            return
        }
        
        if (newPassword.length < 6) {
            onError("Password must be at least 6 characters")
            return
        }
        
        // Re-authenticate user first
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, currentPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Now update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to change password: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Current password is incorrect")
            }
    }
    
    // Profile picture functions (using Base64 stored in Firestore - no Firebase Storage needed)
    fun uploadProfilePicture(
        imageUri: Uri,
        context: android.content.Context,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onError("User not authenticated")
            return
        }
        
        isUploadingPhoto.value = true
        
        try {
            // Read and compress the image
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                isUploadingPhoto.value = false
                onError("Failed to load image")
                return
            }
            
            // Resize to max 200x200 for profile picture (keeps file size small)
            val maxSize = 200
            val ratio = minOf(maxSize.toFloat() / originalBitmap.width, maxSize.toFloat() / originalBitmap.height)
            val newWidth = (originalBitmap.width * ratio).toInt()
            val newHeight = (originalBitmap.height * ratio).toInt()
            val resizedBitmap = android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            
            // Convert to Base64 (NO_WRAP to avoid line breaks that break data URIs)
            val outputStream = java.io.ByteArrayOutputStream()
            resizedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64String = android.util.Base64.encodeToString(
                outputStream.toByteArray(), 
                android.util.Base64.NO_WRAP
            )
            
            // Store as data URI
            val dataUri = "data:image/jpeg;base64,$base64String"
            
            Log.d("ProfilePicture", "Base64 string length: ${base64String.length}")
            
            // Save to Firestore
            db.collection("users").document(userId)
                .update("profilePictureUrl", dataUri)
                .addOnSuccessListener {
                    profilePictureUrl.value = dataUri
                    isUploadingPhoto.value = false
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    isUploadingPhoto.value = false
                    onError("Failed to save profile picture: ${e.message}")
                }
                
            // Clean up
            if (originalBitmap != resizedBitmap) {
                originalBitmap.recycle()
            }
            resizedBitmap.recycle()
            
        } catch (e: Exception) {
            isUploadingPhoto.value = false
            onError("Failed to process image: ${e.message}")
        }
    }
    
    fun removeProfilePicture(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onError("User not authenticated")
            return
        }
        
        isUploadingPhoto.value = true
        
        // Remove from Firestore
        db.collection("users").document(userId)
            .update("profilePictureUrl", null)
            .addOnSuccessListener {
                profilePictureUrl.value = null
                isUploadingPhoto.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                isUploadingPhoto.value = false
                onError("Failed to update profile: ${e.message}")
            }
    }
    
    // ==================== GRADES FUNCTIONALITY ====================
    
    fun loadStudentGrades() {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("StudentGrades", "No authenticated user found")
            return
        }
        
        isLoadingGrades.value = true
        Log.d("StudentGrades", "Loading grades for student: $uid")
        
        // Clean up previous listeners
        gradesListener?.remove()
        gradeResultsListeners.forEach { it.remove() }
        gradeResultsListeners.clear()
        
        // Listen to grades collection for this student
        gradesListener = db.collection("grades")
            .whereEqualTo("studentId", uid)
            .addSnapshotListener { gradesSnapshot, error ->
                if (error != null) {
                    Log.e("StudentGrades", "Error loading grades: ${error.message}")
                    isLoadingGrades.value = false
                    return@addSnapshotListener
                }
                
                Log.d("StudentGrades", "Found ${gradesSnapshot?.documents?.size ?: 0} grades")
                
                if (gradesSnapshot == null || gradesSnapshot.isEmpty) {
                    allGrades.value = emptyList()
                    calculateStudentStatistics(emptyList())
                    generateClassGrades(emptyList())
                    isLoadingGrades.value = false
                    return@addSnapshotListener
                }
                
                // Get all exam IDs to fetch exam details
                val examIds = gradesSnapshot.documents.mapNotNull { it.getString("examId") }.distinct()
                
                if (examIds.isEmpty()) {
                    allGrades.value = emptyList()
                    calculateStudentStatistics(emptyList())
                    generateClassGrades(emptyList())
                    isLoadingGrades.value = false
                    return@addSnapshotListener
                }
                
                // Fetch exam details and build grade items
                fetchExamDetailsAndBuildGrades(gradesSnapshot.documents, examIds, uid)
            }
    }
    
    private fun fetchExamDetailsAndBuildGrades(
        gradeDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        examIds: List<String>,
        studentId: String
    ) {
        val examMap = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot>()
        val examBatches = examIds.chunked(10)
        var completedBatches = 0
        
        examBatches.forEach { batch ->
            db.collection("exams")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), batch)
                .get()
                .addOnSuccessListener { examsSnapshot ->
                    examsSnapshot.documents.forEach { examDoc ->
                        examMap[examDoc.id] = examDoc
                    }
                    
                    completedBatches++
                    if (completedBatches == examBatches.size) {
                        // Now build the grade items
                        buildGradeItems(gradeDocs, examMap, studentId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StudentGrades", "Error fetching exams: ${e.message}")
                    completedBatches++
                    if (completedBatches == examBatches.size) {
                        buildGradeItems(gradeDocs, examMap, studentId)
                    }
                }
        }
    }
    
    private fun buildGradeItems(
        gradeDocs: List<com.google.firebase.firestore.DocumentSnapshot>,
        examMap: Map<String, com.google.firebase.firestore.DocumentSnapshot>,
        studentId: String
    ) {
        val grades = mutableListOf<StudentGradeItem>()
        
        gradeDocs.forEach { gradeDoc ->
            val examId = gradeDoc.getString("examId") ?: return@forEach
            val examDoc = examMap[examId] ?: return@forEach
            
            val answers = (gradeDoc.get("answers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val answerKey = (examDoc.get("answerKey") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            
            val score = gradeDoc.getLong("score")?.toInt() ?: 0
            val totalQuestions = gradeDoc.getLong("totalQuestions")?.toInt()
                ?: examDoc.getLong("totalQuestions")?.toInt()
                ?: answerKey.size
            
            // Calculate answer statistics
            var correct = 0
            var incorrect = 0
            var unanswered = 0
            
            if (answers.isNotEmpty() && answerKey.isNotEmpty()) {
                answers.forEachIndexed { index, answer ->
                    when {
                        answer.isEmpty() || answer == "-" -> unanswered++
                        answer == answerKey.getOrNull(index) -> correct++
                        else -> incorrect++
                    }
                }
            } else {
                correct = score
                incorrect = totalQuestions - score
            }
            
            grades.add(
                StudentGradeItem(
                    id = gradeDoc.id,
                    examId = examId,
                    examName = examDoc.getString("examName") ?: examDoc.getString("title") ?: "Exam",
                    className = examDoc.getString("className") ?: "",
                    classId = examDoc.getString("classId") ?: "",
                    score = score,
                    totalPoints = totalQuestions,
                    percentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions * 100) else 0f,
                    gradedDate = gradeDoc.getLong("gradedAt")
                        ?: gradeDoc.getLong("timestamp")
                        ?: System.currentTimeMillis(),
                    answers = answers,
                    correctAnswers = correct,
                    incorrectAnswers = incorrect,
                    unanswered = unanswered
                )
            )
        }
        
        Log.d("StudentGrades", "Built ${grades.size} grade items")
        
        // Fetch class names for grades that don't have them
        fetchClassNamesAndContinue(grades, studentId)
    }
    
    private fun fetchClassNamesAndContinue(grades: List<StudentGradeItem>, studentId: String) {
        // Get unique class IDs that need name resolution
        val classIdsNeedingNames = grades
            .filter { it.className.isEmpty() && it.classId.isNotEmpty() }
            .map { it.classId }
            .distinct()
        
        if (classIdsNeedingNames.isEmpty()) {
            // All grades already have class names, proceed to rankings
            fetchClassRankings(grades, studentId)
            return
        }
        
        Log.d("StudentGrades", "Fetching names for ${classIdsNeedingNames.size} classes")
        
        val classNameMap = mutableMapOf<String, String>()
        var fetchedCount = 0
        
        classIdsNeedingNames.forEach { classId ->
            db.collection("classes")
                .document(classId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val className = doc.getString("className") ?: doc.getString("name") ?: ""
                        if (className.isNotEmpty()) {
                            classNameMap[classId] = className
                        }
                    }
                    fetchedCount++
                    if (fetchedCount == classIdsNeedingNames.size) {
                        // Update grades with class names and continue
                        val updatedGrades = grades.map { grade ->
                            if (grade.className.isEmpty() && classNameMap.containsKey(grade.classId)) {
                                grade.copy(className = classNameMap[grade.classId] ?: "")
                            } else {
                                grade
                            }
                        }
                        fetchClassRankings(updatedGrades, studentId)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("StudentGrades", "Error fetching class ${classId}: ${e.message}")
                    fetchedCount++
                    if (fetchedCount == classIdsNeedingNames.size) {
                        val updatedGrades = grades.map { grade ->
                            if (grade.className.isEmpty() && classNameMap.containsKey(grade.classId)) {
                                grade.copy(className = classNameMap[grade.classId] ?: "")
                            } else {
                                grade
                            }
                        }
                        fetchClassRankings(updatedGrades, studentId)
                    }
                }
        }
    }
    
    private fun fetchClassRankings(grades: List<StudentGradeItem>, studentId: String) {
        if (grades.isEmpty()) {
            updateGradesUI(grades)
            return
        }
        
        val examIds = grades.map { it.examId }.distinct()
        val updatedGrades = grades.toMutableList()
        var processedExams = 0
        
        examIds.forEach { examId ->
            db.collection("grades")
                .whereEqualTo("examId", examId)
                .get()
                .addOnSuccessListener { allResultsSnapshot ->
                    // Calculate rank and class average
                    val allScores = allResultsSnapshot.documents.mapNotNull { doc ->
                        val score = doc.getLong("score")?.toInt() ?: 0
                        val total = doc.getLong("totalQuestions")?.toInt() ?: 1
                        val pct = if (total > 0) (score.toFloat() / total * 100) else 0f
                        doc.getString("studentId") to pct
                    }
                    
                    val classAverage = if (allScores.isNotEmpty()) {
                        allScores.map { it.second }.average().toFloat()
                    } else 0f
                    
                    // Sort by percentage descending to get rank
                    val sortedScores = allScores.sortedByDescending { it.second }
                    val studentRank = sortedScores.indexOfFirst { it.first == studentId } + 1
                    
                    // Update grades with this exam
                    updatedGrades.forEachIndexed { index, grade ->
                        if (grade.examId == examId) {
                            updatedGrades[index] = grade.copy(
                                rank = studentRank,
                                classAverage = classAverage
                            )
                        }
                    }
                    
                    processedExams++
                    if (processedExams == examIds.size) {
                        updateGradesUI(updatedGrades)
                    }
                }
                .addOnFailureListener {
                    processedExams++
                    if (processedExams == examIds.size) {
                        updateGradesUI(updatedGrades)
                    }
                }
        }
    }
    
    private fun updateGradesUI(grades: List<StudentGradeItem>) {
        allGrades.value = grades.sortedByDescending { it.gradedDate }
        calculateStudentStatistics(grades)
        generateClassGrades(grades)
        isLoadingGrades.value = false
    }
    
    private fun calculateStudentStatistics(grades: List<StudentGradeItem>) {
        if (grades.isEmpty()) {
            studentStatistics.value = StudentStatistics()
            return
        }
        
        val percentages = grades.map { it.percentage }
        val avgScore = percentages.average().toFloat()
        val highest = percentages.maxOrNull() ?: 0f
        val lowest = percentages.minOrNull() ?: 0f
        val totalClasses = grades.map { it.classId }.distinct().size
        val perfectScores = grades.count { it.percentage >= 100f }
        
        // Calculate improvement trend (compare recent vs older grades)
        val sortedByDate = grades.sortedBy { it.gradedDate }
        val trend = if (sortedByDate.size >= 2) {
            val midPoint = sortedByDate.size / 2
            val olderAvg = sortedByDate.take(midPoint).map { it.percentage }.average().toFloat()
            val newerAvg = sortedByDate.takeLast(midPoint).map { it.percentage }.average().toFloat()
            newerAvg - olderAvg
        } else 0f
        
        studentStatistics.value = StudentStatistics(
            totalExamsTaken = grades.size,
            averageScore = avgScore,
            highestScore = highest,
            lowestScore = lowest,
            totalClasses = totalClasses,
            perfectScores = perfectScores,
            improvementTrend = trend
        )
    }
    
    private fun generateClassGrades(grades: List<StudentGradeItem>) {
        val grouped = grades.groupBy { it.classId }.map { (classId, classGrades) ->
            StudentClassGrades(
                className = classGrades.firstOrNull()?.className ?: "",
                classId = classId,
                averageScore = classGrades.map { it.percentage }.average().toFloat(),
                examCount = classGrades.size,
                grades = classGrades.sortedByDescending { it.gradedDate }
            )
        }.sortedBy { it.className }
        
        classGrades.value = grouped
    }
    
    // Filter and sort functions
    fun getFilteredAndSortedGrades(): List<StudentGradeItem> {
        var filtered = allGrades.value
        
        // Apply class filter
        selectedClassFilter.value?.let { classId ->
            filtered = filtered.filter { it.classId == classId }
        }
        
        // Apply search
        if (searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter {
                it.examName.contains(searchQuery.value, ignoreCase = true) ||
                it.className.contains(searchQuery.value, ignoreCase = true)
            }
        }
        
        // Apply sorting
        return when (selectedSortOption.value) {
            StudentGradeSortOption.DATE_DESC -> filtered.sortedByDescending { it.gradedDate }
            StudentGradeSortOption.DATE_ASC -> filtered.sortedBy { it.gradedDate }
            StudentGradeSortOption.SCORE_DESC -> filtered.sortedByDescending { it.percentage }
            StudentGradeSortOption.SCORE_ASC -> filtered.sortedBy { it.percentage }
            StudentGradeSortOption.EXAM_NAME -> filtered.sortedBy { it.examName }
            StudentGradeSortOption.CLASS_NAME -> filtered.sortedBy { it.className }
        }
    }
    
    fun setSortOption(option: StudentGradeSortOption) {
        selectedSortOption.value = option
    }
    
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    
    fun setClassFilter(classId: String?) {
        selectedClassFilter.value = classId
    }
    
    fun refreshGrades() {
        loadStudentGrades()
    }
    
    // Notifications functions
    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d("Notifications", "Loading notifications for student: $userId")
        
        notificationsListener?.remove()
        // Simple query without orderBy to avoid needing composite index
        notificationsListener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Notifications", "Error loading notifications: ${error.message}")
                    generateNotificationsFromActivity()
                    return@addSnapshotListener
                }
                
                Log.d("Notifications", "Received ${snapshot?.size() ?: 0} notifications")
                
                if (snapshot == null || snapshot.isEmpty) {
                    generateNotificationsFromActivity()
                    return@addSnapshotListener
                }
                
                val notificationsList = snapshot.documents.mapNotNull { doc ->
                    try {
                        val typeStr = doc.getString("type") ?: "SYSTEM"
                        val type = try {
                            NotificationType.valueOf(typeStr)
                        } catch (e: Exception) {
                            NotificationType.SYSTEM
                        }
                        
                        NotificationItem(
                            id = doc.id,
                            type = type,
                            title = doc.getString("title") ?: "",
                            message = doc.getString("message") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            isRead = doc.getBoolean("isRead") ?: false,
                            relatedId = doc.getString("relatedId")
                        )
                    } catch (e: Exception) {
                        Log.e("Notifications", "Error parsing notification: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.timestamp }.take(50) // Sort locally
                
                Log.d("Notifications", "Parsed ${notificationsList.size} notifications")
                
                notifications.value = notificationsList
                unreadNotificationCount.value = notificationsList.count { !it.isRead }
            }
    }
    
    private fun generateNotificationsFromActivity() {
        val notificationsList = mutableListOf<NotificationItem>()
        
        // Add notifications from recent grades
        allGrades.value.take(10).forEach { grade ->
            notificationsList.add(
                NotificationItem(
                    id = "grade_${grade.id}",
                    type = NotificationType.NEW_GRADE,
                    title = "Grade Received",
                    message = "You scored ${grade.percentage.toInt()}% on ${grade.examName} in ${grade.className}",
                    timestamp = grade.gradedDate,
                    isRead = true,
                    relatedId = grade.examId
                )
            )
        }
        
        // Add notifications from classes joined
        classes.value.take(5).forEach { classItem ->
            notificationsList.add(
                NotificationItem(
                    id = "class_${classItem.id}",
                    type = NotificationType.CLASS_UPDATE,
                    title = "Enrolled in Class",
                    message = "You joined ${classItem.className} - ${classItem.section}",
                    timestamp = System.currentTimeMillis() - (86400000 * classes.value.indexOf(classItem)),
                    isRead = true,
                    relatedId = classItem.id
                )
            )
        }
        
        notifications.value = notificationsList.sortedByDescending { it.timestamp }.take(20)
        unreadNotificationCount.value = 0
    }
    
    fun markNotificationAsRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                notifications.value = notifications.value.map { 
                    if (it.id == notificationId) it.copy(isRead = true) else it 
                }
                unreadNotificationCount.value = notifications.value.count { !it.isRead }
            }
    }
    
    fun markAllNotificationsAsRead() {
        val userId = auth.currentUser?.uid ?: return
        
        notifications.value.filter { !it.isRead }.forEach { notification ->
            db.collection("notifications")
                .document(notification.id)
                .update("isRead", true)
        }
        
        notifications.value = notifications.value.map { it.copy(isRead = true) }
        unreadNotificationCount.value = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        classesListener?.remove()
        gradesListener?.remove()
        gradeResultsListeners.forEach { it.remove() }
        gradeResultsListeners.clear()
        notificationsListener?.remove()
    }
}