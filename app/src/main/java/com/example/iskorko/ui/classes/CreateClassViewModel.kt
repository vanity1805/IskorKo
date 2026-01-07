package com.example.iskorko.ui.classes

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class CreateClassViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var className = mutableStateOf("")
        private set
    
    var section = mutableStateOf("")
        private set
    
    var classCode = mutableStateOf(generateClassCode())
        private set
    
    var isLoading = mutableStateOf(false)
        private set
    
    var errorMessage = mutableStateOf<String?>(null)
        private set
    
    fun onClassNameChange(value: String) {
        className.value = value
        errorMessage.value = null
    }
    
    fun onSectionChange(value: String) {
        section.value = value
        errorMessage.value = null
    }
    
    fun regenerateClassCode() {
        classCode.value = generateClassCode()
    }
    
    private fun validateInputs(): Boolean {
        return when {
            className.value.isBlank() -> {
                errorMessage.value = "Please enter a class name"
                false
            }
            section.value.isBlank() -> {
                errorMessage.value = "Please enter a section"
                false
            }
            else -> true
        }
    }
    
    fun createClass(onSuccess: () -> Unit) {
        if (!validateInputs()) return
        
        isLoading.value = true
        errorMessage.value = null
        
        val professorId = auth.currentUser?.uid
        if (professorId == null) {
            errorMessage.value = "User not authenticated"
            isLoading.value = false
            return
        }
        
        // Check if class code already exists
        db.collection("classes")
            .whereEqualTo("classCode", classCode.value)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Class code conflict - generate new one
                    classCode.value = generateClassCode()
                    errorMessage.value = "Class code conflict, new code generated"
                    isLoading.value = false
                } else {
                    // Create the class
                    val classData = hashMapOf(
                        "className" to className.value.trim(),
                        "section" to section.value.trim(),
                        "classCode" to classCode.value,
                        "professorId" to professorId,
                        "studentIds" to emptyList<String>(),
                        "createdAt" to System.currentTimeMillis()
                    )
                    
                    db.collection("classes")
                        .add(classData)
                        .addOnSuccessListener {
                            isLoading.value = false
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            errorMessage.value = "Failed to create class: ${e.message}"
                            isLoading.value = false
                        }
                }
            }
            .addOnFailureListener { e ->
                errorMessage.value = "Error checking class code: ${e.message}"
                isLoading.value = false
            }
    }
    
    private fun generateClassCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}