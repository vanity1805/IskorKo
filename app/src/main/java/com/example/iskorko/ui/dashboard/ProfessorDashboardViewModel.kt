package com.example.iskorko.ui.dashboard

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class ClassItem(
    val id: String = "",
    val className: String = "",
    val section: String = "",
    val classCode: String = "",
    val studentCount: Int = 0,
    val examCount: Int = 0,
    val archived: Boolean = false
)

data class GradeItem(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val className: String = "",
    val classId: String = "",
    val examName: String = "",
    val examId: String = "",
    val score: Int = 0,
    val totalPoints: Int = 0,
    val percentage: Float = 0f,
    val gradedDate: Long = 0L,
    val remarks: String = "",
    val answers: List<String> = emptyList(),
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val unanswered: Int = 0
)

data class GradeStatistics(
    val totalExamsGraded: Int = 0,
    val averageScore: Float = 0f,
    val pendingExams: Int = 0,
    val totalStudents: Int = 0,
    val highestScore: Int = 0,
    val lowestScore: Int = 0,
    val passingRate: Float = 0f
)

data class ClassGradesSummary(
    val className: String = "",
    val classId: String = "",
    val examCount: Int = 0,
    val studentCount: Int = 0,
    val averageScore: Float = 0f,
    val grades: List<GradeItem> = emptyList()
)

data class ExamGradesSummary(
    val examName: String = "",
    val examId: String = "",
    val className: String = "",
    val totalQuestions: Int = 0,
    val averageScore: Float = 0f,
    val highestScore: Int = 0,
    val lowestScore: Int = 0,
    val studentsTaken: Int = 0,
    val grades: List<GradeItem> = emptyList()
)

enum class GradeViewMode {
    ALL_GRADES,
    BY_CLASS,
    BY_EXAM,
    BY_STUDENT
}

enum class GradeSortOption {
    DATE_DESC,
    DATE_ASC,
    SCORE_DESC,
    SCORE_ASC,
    STUDENT_NAME,
    CLASS_NAME,
    EXAM_NAME
}

enum class NotificationType {
    NEW_STUDENT,
    NEW_GRADE,
    CLASS_UPDATE,
    EXAM_CREATED,
    SYSTEM
}

data class NotificationItem(
    val id: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val isRead: Boolean = false,
    val relatedId: String? = null, // classId, examId, etc.
    val classLabel: String? = null
) {
    fun getTimeAgo(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }
}

class ProfessorDashboardViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var professorName = mutableStateOf("Professor")
        private set
    
    var professorEmail = mutableStateOf("")
        private set
    
    var professorJoinDate = mutableStateOf(0L)
        private set
    
    var profilePictureUrl = mutableStateOf<String?>(null)
        private set
    
    var isUploadingPhoto = mutableStateOf(false)
        private set
    
    var classes = mutableStateOf<List<ClassItem>>(emptyList())
        private set

    var archivedClasses = mutableStateOf<List<ClassItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    var allGrades = mutableStateOf<List<GradeItem>>(emptyList())
        private set
    
    var gradeStatistics = mutableStateOf(GradeStatistics())
        private set
    
    var classSummaries = mutableStateOf<List<ClassGradesSummary>>(emptyList())
        private set
    
    var examSummaries = mutableStateOf<List<ExamGradesSummary>>(emptyList())
        private set
    
    var isLoadingGrades = mutableStateOf(false)
        private set
    
    var selectedViewMode = mutableStateOf(GradeViewMode.ALL_GRADES)
        private set
    
    var selectedSortOption = mutableStateOf(GradeSortOption.DATE_DESC)
        private set
    
    var searchQuery = mutableStateOf("")
        private set
    
    var selectedClassFilter = mutableStateOf<String?>(null)
        private set
    
    var selectedExamFilter = mutableStateOf<String?>(null)
        private set
    
    private var classesListener: ListenerRegistration? = null
    private var gradesListener: ListenerRegistration? = null
    private var examResultsListeners = mutableListOf<ListenerRegistration>()
    private var notificationsListener: ListenerRegistration? = null
    private var allClasses: List<ClassItem> = emptyList()
    
    var notifications = mutableStateOf<List<NotificationItem>>(emptyList())
        private set
    
    var unreadNotificationCount = mutableStateOf(0)
        private set
    
    init {
        loadProfessorData()
        loadClasses()
        loadAllGrades()
        loadNotifications()
    }
    
    private fun loadProfessorData() {
        val uid = auth.currentUser?.uid ?: return
        
        // Get email from auth
        professorEmail.value = auth.currentUser?.email ?: ""
        
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                professorName.value = document.getString("fullName") ?: "Professor"
                professorJoinDate.value = document.getLong("createdAt") ?: 0L
                profilePictureUrl.value = document.getString("profilePictureUrl")
            }
            .addOnFailureListener {
                professorName.value = "Professor"
            }
    }
    
     private fun loadClasses() {
        val uid = auth.currentUser?.uid ?: return
        
        // Real-time listener for classes
        classesListener = db.collection("classes")
            .whereEqualTo("professorId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading.value = false
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    classes.value = emptyList()
                    archivedClasses.value = emptyList()
                    allClasses = emptyList()
                    isLoading.value = false
                    return@addSnapshotListener
                }
                
                // Load classes with exam counts
                loadClassesWithExamCounts(snapshot.documents)
            }
    }
    
    private fun loadClassesWithExamCounts(classDocs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val loadedClasses = mutableListOf<ClassItem>()
        var processedCount = 0
        
        classDocs.forEach { doc ->
            val classId = doc.id
            
            // Count exams for this class
            db.collection("exams")
                .whereEqualTo("classId", classId)
                .get()
                .addOnSuccessListener { examSnapshot ->
                    loadedClasses.add(
                        ClassItem(
                            id = classId,
                            className = doc.getString("className") ?: "",
                            section = doc.getString("section") ?: "",
                            classCode = doc.getString("classCode") ?: "",
                            studentCount = (doc.get("studentIds") as? List<*>)?.size ?: 0,
                            examCount = examSnapshot.size(),
                            archived = doc.getBoolean("archived") ?: false
                        )
                    )
                    
                    processedCount++
                    if (processedCount == classDocs.size) {
                        val sortedClasses = loadedClasses.sortedBy { it.className }
                        allClasses = sortedClasses
                        classes.value = sortedClasses.filter { !it.archived }
                        archivedClasses.value = sortedClasses.filter { it.archived }
                        isLoading.value = false
                    }
                }
                .addOnFailureListener {
                    loadedClasses.add(
                        ClassItem(
                            id = classId,
                            className = doc.getString("className") ?: "",
                            section = doc.getString("section") ?: "",
                            classCode = doc.getString("classCode") ?: "",
                            studentCount = (doc.get("studentIds") as? List<*>)?.size ?: 0,
                            examCount = 0,
                            archived = doc.getBoolean("archived") ?: false
                        )
                    )
                    
                    processedCount++
                    if (processedCount == classDocs.size) {
                        val sortedClasses = loadedClasses.sortedBy { it.className }
                        allClasses = sortedClasses
                        classes.value = sortedClasses.filter { !it.archived }
                        archivedClasses.value = sortedClasses.filter { it.archived }
                        isLoading.value = false
                    }
                }
        }
    }

    // Load all grades from Firestore with REAL-TIME updates
    fun loadAllGrades() {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("GradesTab", "No authenticated user found")
            return
        }
        
        isLoadingGrades.value = true
        Log.d("GradesTab", "Starting to load grades for user: $uid")
        
        // Remove previous grades listener if exists
        gradesListener?.remove()
        examResultsListeners.forEach { it.remove() }
        examResultsListeners.clear()
        
        // First, get all class IDs for this professor (real-time)
        gradesListener = db.collection("classes")
            .whereEqualTo("professorId", uid)
            .addSnapshotListener { classesSnapshot, error ->
                if (error != null) {
                    Log.e("GradesTab", "Error loading classes: ${error.message}")
                    isLoadingGrades.value = false
                    return@addSnapshotListener
                }
                
                val classIds = classesSnapshot?.documents?.map { it.id } ?: emptyList()
                Log.d("GradesTab", "Found ${classIds.size} classes")
                
                if (classIds.isEmpty()) {
                    allGrades.value = emptyList()
                    calculateStatistics(emptyList())
                    generateClassSummaries(emptyList())
                    generateExamSummaries(emptyList())
                    isLoadingGrades.value = false
                    return@addSnapshotListener
                }
                
                // Load grades for all classes
                loadGradesForClassesRealtime(classIds)
            }
    }
    
    private fun loadGradesForClassesRealtime(classIds: List<String>) {
        // Clean up previous listeners
        examResultsListeners.forEach { it.remove() }
        examResultsListeners.clear()
        
        Log.d("GradesTab", "Loading grades for classes: $classIds")
        
        // Firestore limitation: whereIn supports max 10 values, so we batch if needed
        val batches = classIds.chunked(10)
        val allGradesList = mutableListOf<GradeItem>()
        var completedBatches = 0
        
        batches.forEach { batchClassIds ->
            // First get all exams for these classes
            db.collection("exams")
                .whereIn("classId", batchClassIds)
                .get()
                .addOnSuccessListener { examsSnapshot ->
                    val examMap = examsSnapshot.documents.associateBy { it.id }
                    val examIds = examMap.keys.toList()
                    
                    Log.d("GradesTab", "Found ${examIds.size} exams for batch")
                    
                    if (examIds.isEmpty()) {
                        completedBatches++
                        if (completedBatches == batches.size) {
                            finalizeGradesLoading(allGradesList)
                        }
                        return@addOnSuccessListener
                    }
                    
                    // Listen to GRADES collection in real-time (not examResults!)
                    val examBatches = examIds.chunked(10)
                    examBatches.forEach { examIdBatch ->
                        val listener = db.collection("grades")  // Changed from "examResults" to "grades"
                            .whereIn("examId", examIdBatch)
                            .addSnapshotListener { resultsSnapshot, resultsError ->
                                if (resultsError != null) {
                                    Log.e("GradesTab", "Error loading grades: ${resultsError.message}")
                                    return@addSnapshotListener
                                }
                                
                                Log.d("GradesTab", "Received ${resultsSnapshot?.documents?.size ?: 0} grades from database")
                                
                                // Build grades from results
                                val batchGrades = mutableListOf<GradeItem>()
                                
                                resultsSnapshot?.documents?.forEach { gradeDoc ->
                                    val examId = gradeDoc.getString("examId") ?: return@forEach
                                    val examDoc = examMap[examId] ?: return@forEach
                                    
                                    val studentId = gradeDoc.getString("studentId") ?: return@forEach
                                    
                                    val answers = (gradeDoc.get("answers") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                                    val correctAnswerKey = (examDoc.get("answerKey") as? List<*>)?.mapNotNull { it as? String } 
                                        ?: emptyList()
                                    
                                    val score = gradeDoc.getLong("score")?.toInt() ?: 0
                                    val totalQuestions = gradeDoc.getLong("totalQuestions")?.toInt() 
                                        ?: examDoc.getLong("totalQuestions")?.toInt()
                                        ?: correctAnswerKey.size
                                    
                                    // Calculate statistics from answers if available
                                    var correct = 0
                                    var incorrect = 0
                                    var unanswered = 0
                                    
                                    if (answers.isNotEmpty() && correctAnswerKey.isNotEmpty()) {
                                        answers.forEachIndexed { index, answer ->
                                            when {
                                                answer.isEmpty() || answer == "-" -> unanswered++
                                                answer == correctAnswerKey.getOrNull(index) -> correct++
                                                else -> incorrect++
                                            }
                                        }
                                    } else {
                                        // If no answers array, calculate from score
                                        correct = score
                                        incorrect = totalQuestions - score
                                    }
                                    
                                    val classId = examDoc.getString("classId") ?: ""
                                    
                                    // Get class name from loaded classes
                                    val classItem = allClasses.find { it.id == classId }
                                    val classNameDisplay = if (classItem != null) {
                                        "${classItem.className} - ${classItem.section}"
                                    } else {
                                        examDoc.getString("className") ?: classId
                                    }
                                    
                                    // Get student name - we need to fetch it
                                    batchGrades.add(
                                        GradeItem(
                                            id = gradeDoc.id,
                                            studentId = studentId,
                                            studentName = "", // Will be filled below
                                            className = classNameDisplay,
                                            classId = classId,
                                            examName = examDoc.getString("examName") ?: examDoc.getString("title") ?: "",
                                            examId = examId,
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
                                
                                // Fetch student names and update grades
                                fetchStudentNamesAndUpdate(batchGrades, examIdBatch, allGradesList)
                            }
                        
                        examResultsListeners.add(listener)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GradesTab", "Error loading exams: ${e.message}")
                    completedBatches++
                    if (completedBatches == batches.size) {
                        isLoadingGrades.value = false
                    }
                }
        }
    }
    
    private fun fetchStudentNamesAndUpdate(
        grades: List<GradeItem>,
        examIdBatch: List<String>,
        allGradesList: MutableList<GradeItem>
    ) {
        if (grades.isEmpty()) {
            synchronized(allGradesList) {
                allGradesList.removeAll { it.examId in examIdBatch }
                updateGradesUI(allGradesList)
            }
            return
        }
        
        val studentIds = grades.map { it.studentId }.distinct()
        
        if (studentIds.isEmpty()) {
            synchronized(allGradesList) {
                allGradesList.removeAll { it.examId in examIdBatch }
                allGradesList.addAll(grades)
                updateGradesUI(allGradesList)
            }
            return
        }
        
        // Fetch student names individually (in parallel)
        val studentNameMap = mutableMapOf<String, String>()
        var completedFetches = 0
        val totalFetches = studentIds.size
        
        studentIds.forEach { studentId ->
            db.collection("users")
                .document(studentId)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        studentNameMap[studentId] = doc.getString("fullName") 
                            ?: doc.getString("name")
                            ?: "Unknown Student"
                    }
                    
                    completedFetches++
                    if (completedFetches == totalFetches) {
                        finalizeStudentNames(grades, examIdBatch, allGradesList, studentNameMap)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("GradesTab", "Error fetching student $studentId: ${e.message}")
                    completedFetches++
                    if (completedFetches == totalFetches) {
                        finalizeStudentNames(grades, examIdBatch, allGradesList, studentNameMap)
                    }
                }
        }
    }
    
    private fun finalizeStudentNames(
        grades: List<GradeItem>,
        examIdBatch: List<String>,
        allGradesList: MutableList<GradeItem>,
        studentNameMap: Map<String, String>
    ) {
        // Update grades with student names
        val updatedGrades = grades.map { grade ->
            grade.copy(
                studentName = studentNameMap[grade.studentId] 
                    ?: "Student #${grade.studentId.takeLast(4)}"
            )
        }
        
        synchronized(allGradesList) {
            allGradesList.removeAll { it.examId in examIdBatch }
            allGradesList.addAll(updatedGrades)
            
            Log.d("GradesTab", "Total grades after merge: ${allGradesList.size}")
            updateGradesUI(allGradesList)
        }
    }
    
    private fun updateGradesUI(gradesList: List<GradeItem>) {
        allGrades.value = gradesList.toList()
        calculateStatistics(gradesList)
        generateClassSummaries(gradesList)
        generateExamSummaries(gradesList)
        isLoadingGrades.value = false
    }
    
    private fun finalizeGradesLoading(grades: List<GradeItem>) {
        allGrades.value = grades
        calculateStatistics(grades)
        generateClassSummaries(grades)
        generateExamSummaries(grades)
        isLoadingGrades.value = false
    }
    
    private fun calculateStatistics(grades: List<GradeItem>) {
        if (grades.isEmpty()) {
            gradeStatistics.value = GradeStatistics()
            return
        }
        
        val totalExams = grades.size
        val avgScore = grades.map { it.percentage }.average().toFloat()
        val highest = grades.maxOfOrNull { it.score } ?: 0
        val lowest = grades.minOfOrNull { it.score } ?: 0
        val passing = grades.count { it.percentage >= 75 }
        val passingRate = (passing.toFloat() / totalExams * 100)
        
        gradeStatistics.value = GradeStatistics(
            totalExamsGraded = totalExams,
            averageScore = avgScore,
            pendingExams = 0, // Calculate based on exams without results
            totalStudents = grades.map { it.studentId }.distinct().size,
            highestScore = highest,
            lowestScore = lowest,
            passingRate = passingRate
        )
    }
    
    private fun generateClassSummaries(grades: List<GradeItem>) {
        val summaries = grades.groupBy { it.classId }.map { (classId, classGrades) ->
            ClassGradesSummary(
                className = classGrades.firstOrNull()?.className ?: "",
                classId = classId,
                examCount = classGrades.map { it.examId }.distinct().size,
                studentCount = classGrades.map { it.studentId }.distinct().size,
                averageScore = classGrades.map { it.percentage }.average().toFloat(),
                grades = classGrades
            )
        }.sortedBy { it.className }
        
        classSummaries.value = summaries
    }
    
    private fun generateExamSummaries(grades: List<GradeItem>) {
        val summaries = grades.groupBy { it.examId }.map { (examId, examGrades) ->
            ExamGradesSummary(
                examName = examGrades.firstOrNull()?.examName ?: "",
                examId = examId,
                className = examGrades.firstOrNull()?.className ?: "",
                totalQuestions = examGrades.firstOrNull()?.totalPoints ?: 0,
                averageScore = examGrades.map { it.percentage }.average().toFloat(),
                highestScore = examGrades.maxOfOrNull { it.score } ?: 0,
                lowestScore = examGrades.minOfOrNull { it.score } ?: 0,
                studentsTaken = examGrades.size,
                grades = examGrades
            )
        }.sortedByDescending { it.grades.firstOrNull()?.gradedDate ?: 0 }
        
        examSummaries.value = summaries
    }
    
    // Filter and sort functions
    fun getFilteredAndSortedGrades(): List<GradeItem> {
        var filtered = allGrades.value
        
        // Apply class filter
        selectedClassFilter.value?.let { classId ->
            filtered = filtered.filter { it.classId == classId }
        }
        
        // Apply exam filter
        selectedExamFilter.value?.let { examId ->
            filtered = filtered.filter { it.examId == examId }
        }
        
        // Apply search
        if (searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter {
                it.studentName.contains(searchQuery.value, ignoreCase = true) ||
                it.className.contains(searchQuery.value, ignoreCase = true) ||
                it.examName.contains(searchQuery.value, ignoreCase = true)
            }
        }
        
        // Apply sorting
        return when (selectedSortOption.value) {
            GradeSortOption.DATE_DESC -> filtered.sortedByDescending { it.gradedDate }
            GradeSortOption.DATE_ASC -> filtered.sortedBy { it.gradedDate }
            GradeSortOption.SCORE_DESC -> filtered.sortedByDescending { it.percentage }
            GradeSortOption.SCORE_ASC -> filtered.sortedBy { it.percentage }
            GradeSortOption.STUDENT_NAME -> filtered.sortedBy { it.studentName }
            GradeSortOption.CLASS_NAME -> filtered.sortedBy { it.className }
            GradeSortOption.EXAM_NAME -> filtered.sortedBy { it.examName }
        }
    }
    
    fun setViewMode(mode: GradeViewMode) {
        selectedViewMode.value = mode
    }
    
    fun setSortOption(option: GradeSortOption) {
        selectedSortOption.value = option
    }
    
    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    
    fun setClassFilter(classId: String?) {
        selectedClassFilter.value = classId
    }
    
    fun setExamFilter(examId: String?) {
        selectedExamFilter.value = examId
    }
    
    fun logout(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
    
    // Profile update functions
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
                professorName.value = newName.trim()
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
    
    // Refresh grades manually (can be called from UI)
    fun refreshGrades() {
        loadAllGrades()
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
    
    // Notifications functions
    private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        Log.d("Notifications", "Loading notifications for user: $userId")
        
        notificationsListener?.remove()
        // Simple query without orderBy to avoid needing composite index
        notificationsListener = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Notifications", "Error loading notifications: ${error.message}")
                    // Generate notifications from activity if no notifications collection exists
                    generateNotificationsFromActivity()
                    return@addSnapshotListener
                }
                
                Log.d("Notifications", "Received ${snapshot?.size() ?: 0} notifications")
                
                if (snapshot == null || snapshot.isEmpty) {
                    // Generate notifications from activity
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
                            relatedId = doc.getString("relatedId"),
                            classLabel = doc.getString("classLabel")
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
        // Generate notifications from recent grades and classes
        val notificationsList = mutableListOf<NotificationItem>()
        
        // Add notifications from recent grades
        allGrades.value.take(10).forEach { grade ->
            notificationsList.add(
                NotificationItem(
                    id = "grade_${grade.id}",
                    type = NotificationType.NEW_GRADE,
                    title = "New Grade Recorded",
                    message = "${grade.studentName} scored ${grade.percentage.toInt()}% on ${grade.examName}",
                    timestamp = grade.gradedDate,
                    isRead = true,
                    relatedId = grade.examId
                )
            )
        }
        
        // Add notifications from classes
        classes.value.take(5).forEach { classItem ->
            if (classItem.studentCount > 0) {
                notificationsList.add(
                    NotificationItem(
                        id = "class_${classItem.id}",
                        type = NotificationType.CLASS_UPDATE,
                        title = classItem.className,
                        message = "${classItem.studentCount} students enrolled in ${classItem.section}",
                        timestamp = System.currentTimeMillis() - (3600000 * classItem.studentCount), // Spread timestamps
                        isRead = true,
                        relatedId = classItem.id
                    )
                )
            }
        }
        
        // Sort by timestamp and take the most recent
        notifications.value = notificationsList.sortedByDescending { it.timestamp }.take(20)
        unreadNotificationCount.value = 0
    }
    
    fun markNotificationAsRead(notificationId: String) {
        val userId = auth.currentUser?.uid ?: return
        
        db.collection("notifications")
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                // Update local state
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
        
        // Update local state
        notifications.value = notifications.value.map { it.copy(isRead = true) }
        unreadNotificationCount.value = 0
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove all Firestore listeners when ViewModel is destroyed
        classesListener?.remove()
        gradesListener?.remove()
        examResultsListeners.forEach { it.remove() }
        examResultsListeners.clear()
        notificationsListener?.remove()
        Log.d("GradesTab", "ViewModel cleared, listeners removed")
    }
}
