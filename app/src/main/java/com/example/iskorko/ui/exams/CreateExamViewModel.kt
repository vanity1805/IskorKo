package com.example.iskorko.ui.exams

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class CreateExamViewModel : ViewModel() {
    
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var examName = mutableStateOf("")
        private set
    
    var totalQuestions = mutableStateOf("")
        private set
    
    var answerKey = mutableStateOf<List<String>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(false)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    fun onExamNameChange(value: String) {
        examName.value = value
        errorMessage.value = null
    }
    
    fun onTotalQuestionsChange(value: String) {
        // Only allow numbers
        if (value.isEmpty() || value.all { it.isDigit() }) {
            totalQuestions.value = value
            errorMessage.value = null
            
            // Initialize answer key when questions change
            val numQuestions = value.toIntOrNull() ?: 0
            if (numQuestions > 0 && numQuestions <= 100) {
                answerKey.value = List(numQuestions) { "" }
            }
        }
    }
    
    fun onAnswerChange(index: Int, answer: String) {
        if (index >= 0 && index < answerKey.value.size) {
            val newAnswerKey = answerKey.value.toMutableList()
            newAnswerKey[index] = answer
            answerKey.value = newAnswerKey
        }
    }
    
    private fun validateInputs(): Boolean {
        return when {
            examName.value.isBlank() -> {
                errorMessage.value = "Please enter an exam name"
                false
            }
            totalQuestions.value.isBlank() -> {
                errorMessage.value = "Please enter the number of questions"
                false
            }
            (totalQuestions.value.toIntOrNull() ?: 0) < 1 -> {
                errorMessage.value = "Number of questions must be at least 1"
                false
            }
            (totalQuestions.value.toIntOrNull() ?: 0) > 100 -> {
                errorMessage.value = "Number of questions cannot exceed 100"
                false
            }
            answerKey.value.any { it.isBlank() } -> {
                errorMessage.value = "Please set all answer keys"
                false
            }
            else -> true
        }
    }
    
    fun createExam(classId: String, onSuccess: () -> Unit) {
        if (!validateInputs()) return
        
        isLoading.value = true
        errorMessage.value = null
        
        val examData = hashMapOf(
            "examName" to examName.value.trim(),
            "classId" to classId,
            "totalQuestions" to totalQuestions.value.toInt(),
            "answerKey" to answerKey.value,
            "createdAt" to System.currentTimeMillis()
        )
        
        db.collection("exams")
            .add(examData)
            .addOnSuccessListener {
                isLoading.value = false
                onSuccess()
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Failed to create exam: ${e.message}"
                isLoading.value = false
            }
    }
}