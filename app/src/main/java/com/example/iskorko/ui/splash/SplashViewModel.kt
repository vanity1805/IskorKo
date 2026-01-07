package com.example.iskorko.ui.splash

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.mutableStateOf

class SplashViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    val isLoading = mutableStateOf(true)

    fun checkSession(
        onNavigate: (String) -> Unit
    ) {
        val user = auth.currentUser

        // ❌ No logged-in user
        if (user == null) {
            onNavigate("chooseProfile")
            return
        }

        // 🔥 FORCE token refresh (VERY IMPORTANT)
        user.reload().addOnCompleteListener { reloadTask ->
            if (!reloadTask.isSuccessful) {
                auth.signOut()
                isLoading.value = false
                onNavigate("chooseProfile")
                return@addOnCompleteListener
            }

        // 🔍 Check Firestore profile
        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                isLoading.value = false

                if (!document.exists()) {
                    auth.signOut()
                    onNavigate("chooseProfile")
                    return@addOnSuccessListener
                }

                when (document.getString("userType")) {
                    "Student" -> onNavigate("studentDashboard")
                    "Professor" -> onNavigate("professorDashboard")
                    else -> {
                        auth.signOut()
                        onNavigate("chooseProfile")
                    }
                }
            }
            .addOnFailureListener {
                auth.signOut()
                isLoading.value = false
                onNavigate("chooseProfile")
            }
        }    
    }
}
