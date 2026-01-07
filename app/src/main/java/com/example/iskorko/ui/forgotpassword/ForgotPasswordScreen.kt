package com.example.iskorko.ui.forgotpassword

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import com.google.firebase.auth.FirebaseAuth


@Composable
fun ForgotPasswordScreen(navController: NavHostController, userType: String) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val viewModel: ForgotPasswordViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val isLoading by viewModel.isLoading
    val message by viewModel.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "Forgot Password",
                fontFamily = NeueMachina,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Enter your email address",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Reset Password Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .background(Color(0xFF800202), RoundedCornerShape(24.dp))
                    .clickable(enabled = !isLoading) {
                        viewModel.resetPassword(
                            onSuccess = {
                                Toast.makeText(context, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Sending..." else "Reset Password",
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Back to Login
            Text(
                text = "Back to Login",
                fontSize = 14.sp,
                color = Color(0xFF800202),
                modifier = Modifier.clickable {
                    navController.popBackStack()
                }
            )
        }

        // Footer
        Text(
            text = "IskorKo",
            fontFamily = NeueMachina,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}
