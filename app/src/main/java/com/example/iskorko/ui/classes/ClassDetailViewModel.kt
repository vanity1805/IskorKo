package com.example.iskorko.ui.classes

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class StudentInfo(
    val id: String = "",
    val fullName: String = "",
    val email: String = ""
)

data class ExamInfo(
    val id: String = "",
    val examName: String = "",
    val totalQuestions: Int = 0,
    val createdAt: Long = 0
)

class ClassDetailViewModel : ViewModel() {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var className = mutableStateOf("")
        private set
    
    var section = mutableStateOf("")
        private set
    
    var classCode = mutableStateOf("")
        private set
    
    var students = mutableStateOf<List<StudentInfo>>(emptyList())
        private set
    
    var exams = mutableStateOf<List<ExamInfo>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    private var classListener: ListenerRegistration? = null
    private var examsListener: ListenerRegistration? = null
    
    fun loadClassDetails(classId: String) {
        isLoading.value = true
        
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
                    
                    // Load students
                    val studentIds = doc.get("studentIds") as? List<String> ?: emptyList()
                    loadStudents(studentIds)
                }
                
                isLoading.value = false
            }
        
        // Load exams
        loadExams(classId)
    }
    
    private fun loadStudents(studentIds: List<String>) {
        if (studentIds.isEmpty()) {
            students.value = emptyList()
            return
        }
        
        // Load student details from users collection
        db.collection("users")
            .whereIn("__name__", studentIds)
            .get()
            .addOnSuccessListener { documents ->
                val loadedStudents = documents.map { doc ->
                    StudentInfo(
                        id = doc.id,
                        fullName = doc.getString("fullName") ?: "Unknown",
                        email = doc.getString("email") ?: ""
                    )
                }
                students.value = loadedStudents
            }
            .addOnFailureListener {
                // Silently fail, just show empty list
                students.value = emptyList()
            }
    }
    
    private fun loadExams(classId: String) {
        examsListener = db.collection("exams")
            .whereEqualTo("classId", classId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                val loadedExams = snapshot?.documents?.map { doc ->
                    ExamInfo(
                        id = doc.id,
                        examName = doc.getString("examName") ?: "",
                        totalQuestions = doc.getLong("totalQuestions")?.toInt() ?: 0,
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                
                exams.value = loadedExams
            }
    }
    
    fun deleteClass(classId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        // First, find all exams associated with this class
        db.collection("exams")
            .whereEqualTo("classId", classId)
            .get()
            .addOnSuccessListener { examsSnapshot ->
                val examIds = examsSnapshot.documents.map { it.id }
                
                if (examIds.isEmpty()) {
                    // No exams, just delete the class
                    deleteClassDocument(classId, onSuccess, onError)
                } else {
                    // Delete grades for all exams, then exams, then class
                    deleteGradesForExams(examIds) {
                        deleteExamsAndClass(examIds, classId, onSuccess, onError)
                    }
                }
            }
            .addOnFailureListener { e ->
                // If we can't get exams, still try to delete the class
                deleteClassDocument(classId, onSuccess, onError)
            }
    }
    
    private fun deleteGradesForExams(examIds: List<String>, onComplete: () -> Unit) {
        if (examIds.isEmpty()) {
            onComplete()
            return
        }
        
        var completedCount = 0
        val totalBatches = examIds.size
        
        examIds.forEach { examId ->
            db.collection("grades")
                .whereEqualTo("examId", examId)
                .get()
                .addOnSuccessListener { gradesSnapshot ->
                    if (gradesSnapshot.isEmpty) {
                        completedCount++
                        if (completedCount == totalBatches) onComplete()
                    } else {
                        val batch = db.batch()
                        gradesSnapshot.documents.forEach { gradeDoc ->
                            batch.delete(gradeDoc.reference)
                        }
                        batch.commit()
                            .addOnSuccessListener {
                                Log.d("ClassDelete", "Deleted ${gradesSnapshot.size()} grades for exam $examId")
                                completedCount++
                                if (completedCount == totalBatches) onComplete()
                            }
                            .addOnFailureListener {
                                completedCount++
                                if (completedCount == totalBatches) onComplete()
                            }
                    }
                }
                .addOnFailureListener {
                    completedCount++
                    if (completedCount == totalBatches) onComplete()
                }
        }
    }
    
    private fun deleteExamsAndClass(examIds: List<String>, classId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val batch = db.batch()
        
        // Add all exam deletions to batch
        examIds.forEach { examId ->
            batch.delete(db.collection("exams").document(examId))
        }
        
        // Add class deletion to batch
        batch.delete(db.collection("classes").document(classId))
        
        batch.commit()
            .addOnSuccessListener {
                Log.d("ClassDelete", "Deleted class and ${examIds.size} exams")
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to delete class: ${e.message}")
            }
    }
    
    private fun deleteClassDocument(classId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("classes")
            .document(classId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError("Failed to delete class: ${e.message}")
            }
    }
    
    override fun onCleared() {
        super.onCleared()
        classListener?.remove()
        examsListener?.remove()
    }
}