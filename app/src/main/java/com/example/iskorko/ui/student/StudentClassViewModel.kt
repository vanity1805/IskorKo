package com.example.iskorko.ui.student

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class StudentExamItem(
    val id: String = "",
    val examName: String = "",
    val totalQuestions: Int = 0,
    val studentScore: Int? = null, // null if not taken yet
    val percentage: Float? = null,
    val createdAt: Long = 0
)

class StudentClassViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var className = mutableStateOf("")
        private set
    
    var section = mutableStateOf("")
        private set
    
    var classCode = mutableStateOf("")
        private set
    
    var professorName = mutableStateOf("")
        private set
    
    var studentCount = mutableStateOf(0)
        private set
    
    var exams = mutableStateOf<List<StudentExamItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    private var classListener: ListenerRegistration? = null
    private var examsListener: ListenerRegistration? = null
    
    fun loadClassDetails(classId: String) {
        isLoading.value = true
        val studentId = auth.currentUser?.uid ?: return
        
        // Load class info
        classListener = db.collection("classes")
            .document(classId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage.value = "Failed to load class: ${error.message}"
                    isLoading.value = false
                    return@addSnapshotListener
                }
                
                snapshot?.let { doc ->
                    className.value = doc.getString("className") ?: ""
                    section.value = doc.getString("section") ?: ""
                    classCode.value = doc.getString("classCode") ?: ""
                    studentCount.value = (doc.get("studentIds") as? List<*>)?.size ?: 0
                    
                    // Load professor name
                    val professorId = doc.getString("professorId") ?: ""
                    loadProfessorName(professorId)
                }
                
                isLoading.value = false
            }
        
        // Load exams for this class
        loadExams(classId, studentId)
    }
    
    private fun loadProfessorName(professorId: String) {
        db.collection("users")
            .document(professorId)
            .get()
            .addOnSuccessListener { doc ->
                professorName.value = doc.getString("fullName") ?: "Unknown"
            }
    }
    
    private fun loadExams(classId: String, studentId: String) {
        examsListener = db.collection("exams")
            .whereEqualTo("classId", classId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                if (snapshot == null || snapshot.isEmpty) {
                    exams.value = emptyList()
                    return@addSnapshotListener
                }
                
                val examList = snapshot.documents.map { doc ->
                    val examId = doc.id
                    val totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0
                    
                    StudentExamItem(
                        id = examId,
                        examName = doc.getString("examName") ?: "",
                        totalQuestions = totalQuestions,
                        studentScore = null, // Will be loaded separately
                        percentage = null,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }
                
                // Load grades for each exam
                loadGrades(examList, studentId)
            }
    }
    
    private fun loadGrades(examList: List<StudentExamItem>, studentId: String) {
        if (examList.isEmpty()) {
            exams.value = emptyList()
            return
        }
        
        val updatedExams = mutableListOf<StudentExamItem>()
        var processedCount = 0
        
        examList.forEach { exam ->
            // Check if student has a grade for this exam
            db.collection("grades")
                .whereEqualTo("examId", exam.id)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener { gradeSnapshot ->
                    if (gradeSnapshot.isEmpty) {
                        // No grade yet
                        updatedExams.add(exam)
                    } else {
                        // Grade exists
                        val gradeDoc = gradeSnapshot.documents[0]
                        val score = gradeDoc.getLong("score")?.toInt()
                        val percentage = if (score != null && exam.totalQuestions > 0) {
                            (score.toFloat() / exam.totalQuestions.toFloat()) * 100
                        } else null
                        
                        updatedExams.add(
                            exam.copy(
                                studentScore = score,
                                percentage = percentage
                            )
                        )
                    }
                    
                    processedCount++
                    if (processedCount == examList.size) {
                        exams.value = updatedExams.sortedByDescending { it.createdAt }
                    }
                }
                .addOnFailureListener {
                    updatedExams.add(exam)
                    processedCount++
                    if (processedCount == examList.size) {
                        exams.value = updatedExams.sortedByDescending { it.createdAt }
                    }
                }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        classListener?.remove()
        examsListener?.remove()
    }
}