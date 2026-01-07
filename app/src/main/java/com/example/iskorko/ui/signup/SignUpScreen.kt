package com.example.iskorko.ui.signup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import android.widget.Toast
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.DatePickerDialog
import java.util.Calendar
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun SignUpScreen(
    navController: NavHostController, 
    userType: String,
    viewModel: SignupViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) 
{

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
                text = "Create Your Account",
                fontFamily = NeueMachina,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row {
                Text(
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Login",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF800202),
                    modifier = Modifier.clickable {
                        // Navigate to Login screen while passing userType
                        navController.navigate("login/$userType")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = viewModel.fullName.value,
                onValueChange = viewModel::onFullNameChange,
                label = { Text("Full Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.email.value,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val context = LocalContext.current
            val calendar = Calendar.getInstance()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                viewModel.birthDate.value =
                                    "%02d/%02d/%04d".format(day, month + 1, year)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            ) {
                OutlinedTextField(
                    value = viewModel.birthDate.value,
                    onValueChange = {},
                    label = { Text("Birth Date") },
                    singleLine = true,
                    readOnly = true,
                    enabled = false, // important
                    modifier = Modifier.fillMaxWidth()
                )
            }


            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.phoneNumber.value,
                onValueChange = viewModel::onPhoneNumberChange,
                label = { Text("Phone Number") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = viewModel.password.value,
                onValueChange = viewModel::onPasswordChange,
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
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            viewModel.errorMessage.value?.let {
                Text(text = it, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.register(
                        userType = userType,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "$userType Registered Successfully",
                                Toast.LENGTH_SHORT
                            ).show()

                            navController.navigate("login/$userType") {
                                popUpTo("signUp/$userType") {
                                    inclusive = true
                                }
                            }
                        },
                        onError = {message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()}
                    )
                },
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                )
            ) {
                Text(
                    text = "Register",
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
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
