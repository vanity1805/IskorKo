package com.example.iskorko.ui.forgotpassword

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    var email = mutableStateOf("")
        private set

    var isLoading = mutableStateOf(false)
        private set

    var message = mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) {
        email.value = value
        message.value = null
    }

    fun resetPassword(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.value.isBlank()) {
            onError("Please enter your email")
            return
        }

        isLoading.value = true

        auth.sendPasswordResetEmail(email.value.trim())
            .addOnCompleteListener { task ->
                isLoading.value = false
                if (task.isSuccessful) {
                    message.value = "Password reset email sent!"
                    onSuccess()
                } else {
                    val errorMsg = task.exception?.message ?: "Failed to send reset email"
                    message.value = errorMsg
                    onError(errorMsg)
                }
            }
    }
}
