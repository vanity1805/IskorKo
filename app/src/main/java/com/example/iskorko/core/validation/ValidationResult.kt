package com.example.iskorko.core.validation

data class ValidationResult(
    val isValid: Boolean,
    val message: String? = null
)
