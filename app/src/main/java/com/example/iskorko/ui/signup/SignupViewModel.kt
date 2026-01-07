package com.example.iskorko.ui.signup

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.iskorko.core.validation.ValidationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignupViewModel : ViewModel() {

    var fullName = mutableStateOf("")
        private set

    var email = mutableStateOf("")
        private set

    var birthDate = mutableStateOf("")
        private set

    var phoneNumber = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    var errorMessage = mutableStateOf<String?>(null)
        private set

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun onFullNameChange(value: String) {
        fullName.value = value
    }

    fun onEmailChange(value: String) {
        email.value = value
    }

    fun onBirthDateChange(value: String) {
        birthDate.value = value
    }

    fun onPhoneNumberChange(value: String) {
        phoneNumber.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

/* -------------------- Validation -------------------- */

    private fun validateFullName(): ValidationResult =
        if (fullName.value.isBlank())
            ValidationResult(false, "Full name is required")
        else
            ValidationResult(true)

    private fun validateEmail(): ValidationResult =
        if (email.value.isBlank())
            ValidationResult(false, "Email is required")
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.value).matches())
            ValidationResult(false, "Invalid email format")
        else
            ValidationResult(true)

    private fun validateBirthDate(): ValidationResult =
        if (birthDate.value.isBlank())
            ValidationResult(false, "Birth date is required")
        else
            ValidationResult(true)

    private fun validatePhoneNumber(): ValidationResult =
        if (phoneNumber.value.isBlank())
            ValidationResult(false, "Phone number is required")
        else
            ValidationResult(true)

    private fun validatePassword(): ValidationResult =
        if (password.value.length < 6)
            ValidationResult(false, "Password must be at least 6 characters")
        else
            ValidationResult(true)

    fun register(
    userType: String, 
    onSuccess: () -> Unit, 
    onError: (String) -> Unit
    ) {
        val validations = listOf(
            validateFullName(),
            validateEmail(),
            validateBirthDate(),
            validatePhoneNumber(),
            validatePassword()
        )

        val firstError = validations.firstOrNull { !it.isValid }

        if (firstError != null) {
            errorMessage.value = firstError.message
            onError(firstError.message ?: "Invalid input")
            return
        }

        errorMessage.value = null

        android.util.Log.d("SignupViewModel", "Starting Firebase auth...")

        // Firebase Authentication
        auth.createUserWithEmailAndPassword(email.value, password.value)
            .addOnCompleteListener { task ->
                android.util.Log.d("SignupViewModel", "Auth complete listener triggered")
                
                if (task.isSuccessful) {
                    android.util.Log.d("SignupViewModel", "Auth successful")
                    
                    val uid = auth.currentUser?.uid
                    if (uid == null) {
                        android.util.Log.e("SignupViewModel", "UID is null")
                        errorMessage.value = "User ID not found"
                        onError("User ID not found")
                        return@addOnCompleteListener
                    }

                    android.util.Log.d("SignupViewModel", "UID: $uid")

                    val userProfile = hashMapOf(
                        "fullName" to fullName.value.trim(),
                        "email" to email.value.trim(),
                        "birthDate" to birthDate.value,
                        "phoneNumber" to phoneNumber.value.trim(),
                        "userType" to userType,
                        "createdAt" to System.currentTimeMillis()
                    )

                    android.util.Log.d("SignupViewModel", "Saving to Firestore...")

                    db.collection("users")
                        .document(uid)
                        .set(userProfile)
                        .addOnSuccessListener { 
                            android.util.Log.d("SignupViewModel", "✅ Firestore save SUCCESS - calling onSuccess()")
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("SignupViewModel", "❌ Firestore save FAILED: ${e.message}", e)
                            errorMessage.value = "Failed to save profile: ${e.message}"
                            onError("Failed to save profile: ${e.message}")
                        }

                } else {
                    android.util.Log.e("SignupViewModel", "❌ Auth FAILED: ${task.exception?.message}")
                    val error = task.exception?.message ?: "Registration failed"
                    errorMessage.value = error
                    onError(error)
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("SignupViewModel", "❌ Auth addOnFailureListener: ${e.message}", e)
                errorMessage.value = e.message
                onError(e.message ?: "Authentication failed")
            }
    }
}
