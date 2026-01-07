package com.example.iskorko.ui.dashboard

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

class StudentDashboardViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var studentName = mutableStateOf("Student")
        private set
    
    var classes = mutableStateOf<List<StudentClassItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    private var classesListener: ListenerRegistration? = null
    
    init {
        loadStudentData()
        loadClasses()
    }
    
    private fun loadStudentData() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                studentName.value = document.getString("fullName") ?: "Student"
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
                db.collection("classes")
                    .document(classDoc.id)
                    .update("studentIds", FieldValue.arrayUnion(uid))
                    .addOnSuccessListener {
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
    
    override fun onCleared() {
        super.onCleared()
        classesListener?.remove()
    }
}