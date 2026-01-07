package com.example.iskorko.ui.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iskorko.ui.theme.NeueMachina
import com.example.iskorko.ui.components.ProfileButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.clickable
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun LoginScreen(
    navController: NavHostController, 
    userType: String,
    viewModel: LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) 
{
    val context = LocalContext.current
    val isLoading by viewModel.isLoading
    val errorMessage by viewModel.errorMessage
    var passwordVisible by remember { mutableStateOf(false) }

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
                text = "$userType Login",
                fontFamily = NeueMachina,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sign Up clickable text
            Row {
                Text(
                    text = "Don't have an account? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF800202),
                    modifier = Modifier.clickable {
                        // Navigate to SignUp screen while passing userType
                        navController.navigate("signUp/$userType")
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = viewModel.email.value,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()   
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = { viewModel.onPasswordChange(it) },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else
                        Icons.Filled.VisibilityOff

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Forgot your password?",
                fontFamily = NeueMachina,
                fontSize = 14.sp,
                color = Color(0xFF800202), // same as your button color
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(enabled = !isLoading) {
                        navController.navigate("forgotPassword/$userType")
                        // TODO: navigate to forgot password screen
                    }
                )

            Spacer(modifier = Modifier.height(32.dp))

            // 🔴 Error message
            errorMessage?.let {
                Text(text = it, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

             // 🔵 Loading indicator
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = {
                    viewModel.login(
                        expectedUserType = userType,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "$userType Logged In",
                                Toast.LENGTH_SHORT
                            ).show()

                            val destination =
                                if (userType == "Student") "studentDashboard"
                                else "professorDashboard"

                            navController.navigate(destination) {
                                popUpTo("login/$userType") { inclusive = true }
                            }
                        },
                        onError = { /* handled by ViewModel */ }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                )
            ) {
                Text(
                    text = if (isLoading) "Logging in..." else "Login",
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

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