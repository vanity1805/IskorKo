package com.example.iskorko.ui.grades

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class StudentGradeItem(
    val studentId: String = "",
    val studentName: String = "",
    val studentEmail: String = "",
    val score: Int? = null,
    val percentage: Float? = null,
    val isGraded: Boolean = false
)

class GradeEntryViewModel : ViewModel() {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var examName = mutableStateOf("")
        private set
    
    var className = mutableStateOf("")
        private set
    
    var totalQuestions = mutableStateOf(0)
        private set
    
    var students = mutableStateOf<List<StudentGradeItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    private var gradesListener: ListenerRegistration? = null
    
    fun loadExamAndStudents(examId: String) {
        isLoading.value = true
        
        // Load exam details
        db.collection("exams")
            .document(examId)
            .get()
            .addOnSuccessListener { examDoc ->
                examName.value = examDoc.getString("examName") ?: ""
                totalQuestions.value = examDoc.getLong("totalQuestions")?.toInt() ?: 0
                
                val classId = examDoc.getString("classId") ?: ""
                
                // Load class name
                db.collection("classes")
                    .document(classId)
                    .get()
                    .addOnSuccessListener { classDoc ->
                        className.value = classDoc.getString("className") ?: ""
                        
                        // Load students in this class
                        val studentIds = classDoc.get("studentIds") as? List<String> ?: emptyList()
                        loadStudentsWithGrades(studentIds, examId)
                    }
                    .addOnFailureListener { e ->
                        errorMessage.value = "Failed to load class: ${e.message}"
                        isLoading.value = false
                    }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to load exam: ${e.message}"
                isLoading.value = false
            }
    }
    
    private fun loadStudentsWithGrades(studentIds: List<String>, examId: String) {
        if (studentIds.isEmpty()) {
            students.value = emptyList()
            isLoading.value = false
            return
        }
        
        val loadedStudents = mutableListOf<StudentGradeItem>()
        var processedCount = 0
        
        studentIds.forEach { studentId ->
            // Load student info
            db.collection("users")
                .document(studentId)
                .get()
                .addOnSuccessListener { studentDoc ->
                    val studentName = studentDoc.getString("fullName") ?: "Unknown"
                    val studentEmail = studentDoc.getString("email") ?: ""
                    
                    // Check if student has a grade for this exam
                    db.collection("grades")
                        .whereEqualTo("examId", examId)
                        .whereEqualTo("studentId", studentId)
                        .get()
                        .addOnSuccessListener { gradeSnapshot ->
                            if (gradeSnapshot.isEmpty) {
                                // No grade yet
                                loadedStudents.add(
                                    StudentGradeItem(
                                        studentId = studentId,
                                        studentName = studentName,
                                        studentEmail = studentEmail,
                                        score = null,
                                        percentage = null,
                                        isGraded = false
                                    )
                                )
                            } else {
                                // Grade exists
                                val gradeDoc = gradeSnapshot.documents[0]
                                val score = gradeDoc.getLong("score")?.toInt()
                                val percentage = if (score != null && totalQuestions.value > 0) {
                                    (score.toFloat() / totalQuestions.value.toFloat()) * 100
                                } else null
                                
                                loadedStudents.add(
                                    StudentGradeItem(
                                        studentId = studentId,
                                        studentName = studentName,
                                        studentEmail = studentEmail,
                                        score = score,
                                        percentage = percentage,
                                        isGraded = true
                                    )
                                )
                            }
                            
                            processedCount++
                            if (processedCount == studentIds.size) {
                                students.value = loadedStudents.sortedBy { it.studentName }
                                isLoading.value = false
                            }
                        }
                        .addOnFailureListener {
                            loadedStudents.add(
                                StudentGradeItem(
                                    studentId = studentId,
                                    studentName = studentName,
                                    studentEmail = studentEmail,
                                    score = null,
                                    percentage = null,
                                    isGraded = false
                                )
                            )
                            
                            processedCount++
                            if (processedCount == studentIds.size) {
                                students.value = loadedStudents.sortedBy { it.studentName }
                                isLoading.value = false
                            }
                        }
                }
                .addOnFailureListener {
                    processedCount++
                    if (processedCount == studentIds.size) {
                        students.value = loadedStudents.sortedBy { it.studentName }
                        isLoading.value = false
                    }
                }
        }
    }
    
    fun saveGrade(
        examId: String,
        studentId: String,
        score: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (score < 0 || score > totalQuestions.value) {
            onError("Score must be between 0 and ${totalQuestions.value}")
            return
        }
        
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
                    "gradedAt" to System.currentTimeMillis()
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
    
    override fun onCleared() {
        super.onCleared()
        gradesListener?.remove()
    }
}