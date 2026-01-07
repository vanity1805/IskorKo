package com.example.iskorko.ui.login

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.iskorko.core.validation.ValidationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class LoginViewModel : ViewModel() {

    var email = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    var isLoading = mutableStateOf(false)
    private set

    fun setLoading(value: Boolean) {
    isLoading.value = value
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance() 
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    
    fun onEmailChange(value: String) {
        email.value = value
        errorMessage.value = null
    }

    fun onPasswordChange(value: String) {
        password.value = value
        errorMessage.value = null
    }

    // 🔹 SINGLE source of validation truth
    private fun validateLogin(): ValidationResult {
        if (email.value.isBlank()) {
            return ValidationResult(false, "Email is required")
        }

        if (password.value.isBlank()) {
            return ValidationResult(false, "Password is required")
        }

        if (password.value.length < 6) {
            return ValidationResult(false, "Password must be at least 6 characters")
        }

        return ValidationResult(true)
    }

    fun login(
        expectedUserType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val result = validateLogin()

        if (!result.isValid) {
            errorMessage.value = result.message
            onError(result.message ?: "Invalid login")
            return
        }

        isLoading.value = true // 🔹 start loading

        auth.signInWithEmailAndPassword(
            email.value.trim(),
            password.value
        ).addOnCompleteListener { authTask ->
            if (!authTask.isSuccessful) {
                val msg = authTask.exception?.message ?: "Login failed"
                errorMessage.value = msg
                isLoading.value = false // 🔹 stop loading
                onError(msg)
                return@addOnCompleteListener
            }

            val uid = auth.currentUser?.uid
                ?: run {
                    onError("User not found")
                    return@addOnCompleteListener
                }

            // 🔐 Fetch Firestore role
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (!document.exists()) {
                        onError("User profile not found")
                        return@addOnSuccessListener
                    }

                    val storedUserType = document.getString("userType")

                    if (storedUserType == expectedUserType) {
                        onSuccess()
                    } else {
                        auth.signOut() // IMPORTANT
                        onError("Access denied for $expectedUserType account")
                    }
                }
                .addOnFailureListener {
                    onError("Failed to verify user role")
                }
        }
    }

    fun checkIfLoggedIn(
        onAlreadyLoggedIn: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return

        isLoading.value = true

        db.collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                isLoading.value = false

                val role = document.getString("userType")
                if (role != null) {
                    onAlreadyLoggedIn(role)
                } else {
                    auth.signOut()
                }
            }
            .addOnFailureListener {
                isLoading.value = false
                auth.signOut()
            }
    }
}
