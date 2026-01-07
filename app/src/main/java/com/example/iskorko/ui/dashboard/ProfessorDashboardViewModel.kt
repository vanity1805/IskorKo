package com.example.iskorko.ui.dashboard

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
    val examCount: Int = 0
)

class ProfessorDashboardViewModel : ViewModel() {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    var professorName = mutableStateOf("Professor")
        private set
    
    var classes = mutableStateOf<List<ClassItem>>(emptyList())
        private set
    
    var isLoading = mutableStateOf(true)
        private set
    
    private var classesListener: ListenerRegistration? = null
    
    init {
        loadProfessorData()
        loadClasses()
    }
    
    private fun loadProfessorData() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->
                professorName.value = document.getString("fullName") ?: "Professor"
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
                            examCount = examSnapshot.size()
                        )
                    )
                    
                    processedCount++
                    if (processedCount == classDocs.size) {
                        classes.value = loadedClasses.sortedBy { it.className }
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
                            examCount = 0
                        )
                    )
                    
                    processedCount++
                    if (processedCount == classDocs.size) {
                        classes.value = loadedClasses.sortedBy { it.className }
                        isLoading.value = false
                    }
                }
        }
    }
    
    fun logout(onSuccess: () -> Unit) {
        auth.signOut()
        onSuccess()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove Firestore listener when ViewModel is destroyed
        classesListener?.remove()
    }
}