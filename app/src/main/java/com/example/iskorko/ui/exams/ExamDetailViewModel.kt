package com.example.iskorko.ui.exams

import android.util.Log
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
        // First, delete all grades associated with this exam
        db.collection("grades")
            .whereEqualTo("examId", examId)
            .get()
            .addOnSuccessListener { gradesSnapshot ->
                val batch = db.batch()
                
                // Add all grade deletions to batch
                gradesSnapshot.documents.forEach { gradeDoc ->
                    batch.delete(gradeDoc.reference)
                }
                
                // Add exam deletion to batch
                batch.delete(db.collection("exams").document(examId))
                
                // Commit the batch
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("ExamDelete", "Deleted exam and ${gradesSnapshot.size()} associated grades")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        onError("Failed to delete exam: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                // If we can't get grades, still try to delete the exam
                db.collection("exams")
                    .document(examId)
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { ex ->
                        onError("Failed to delete exam: ${ex.message}")
                    }
            }
    }
    
    fun updateAnswerKey(
        examId: String,
        newAnswerKey: List<String>,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (newAnswerKey.isEmpty()) {
            onError("Answer key cannot be empty")
            return
        }
        
        db.collection("exams")
            .document(examId)
            .update("answerKey", newAnswerKey)
            .addOnSuccessListener {
                // Update local state
                answerKey.value = newAnswerKey
                Log.d("ExamDetail", "Answer key updated successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("ExamDetail", "Failed to update answer key: ${e.message}")
                onError("Failed to update answer key: ${e.message}")
            }
    }
}