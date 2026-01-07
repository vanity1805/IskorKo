package com.example.iskorko.ui.exams

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ExamDetailViewModel : ViewModel() {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var examName = mutableStateOf("")
        private set
    
    var totalQuestions = mutableStateOf(0)
        private set
    
    var answerKey = mutableStateOf<List<String>>(emptyList())
        private set
    
    var className = mutableStateOf("")
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set

    var classId = mutableStateOf("")
    private set
    
    fun loadExamDetails(examId: String) {
        isLoading.value = true
        
        db.collection("exams")
            .document(examId)
            .get()
            .addOnSuccessListener { doc ->
                examName.value = doc.getString("examName") ?: ""
                totalQuestions.value = doc.getLong("totalQuestions")?.toInt() ?: 0
                answerKey.value = doc.get("answerKey") as? List<String> ?: emptyList()
                classId.value = doc.getString("classId") ?: "" 
                
                // Load class name
                loadClassName(classId.value)
                isLoading.value = false
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to load exam: ${e.message}"
                isLoading.value = false
            }
    }
    
    private fun loadClassName(classId: String) {
        db.collection("classes")
            .document(classId)
            .get()
            .addOnSuccessListener { doc ->
                className.value = doc.getString("className") ?: ""
            }
    }
    
    fun deleteExam(examId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        db.collection("exams")
            .document(examId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Failed to delete exam: ${e.message}")
            }
    }
}