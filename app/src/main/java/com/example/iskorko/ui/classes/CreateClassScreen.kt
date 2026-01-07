package com.example.iskorko.ui.classes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateClassScreen(
    navController: NavHostController,
    viewModel: CreateClassViewModel = viewModel()
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create New Class",
                        fontFamily = NeueMachina,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Class Information",
                            fontFamily = NeueMachina,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Class Name
                        OutlinedTextField(
                            value = viewModel.className.value,
                            onValueChange = viewModel::onClassNameChange,
                            label = { Text("Class Name") },
                            placeholder = { Text("e.g., Computer Science 101") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isLoading.value
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Section
                        OutlinedTextField(
                            value = viewModel.section.value,
                            onValueChange = viewModel::onSectionChange,
                            label = { Text("Section") },
                            placeholder = { Text("e.g., Section A") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !viewModel.isLoading.value
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Class Code Section
                        Text(
                            text = "Class Code",
                            fontFamily = NeueMachina,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Students will use this code to join your class",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFE3F2FD)
                            ) {
                                Text(
                                    text = viewModel.classCode.value,
                                    modifier = Modifier.padding(16.dp),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = NeueMachina,
                                    color = Color(0xFF1976D2)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            IconButton(
                                onClick = { viewModel.regenerateClassCode() },
                                enabled = !viewModel.isLoading.value
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Generate new code",
                                    tint = Color(0xFF800202)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error Message
                viewModel.errorMessage.value?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEBEE)
                        )
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFD32F2F),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Create Button
                Button(
                    onClick = {
                        viewModel.createClass(
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Class created successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.popBackStack()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !viewModel.isLoading.value,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800202)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (viewModel.isLoading.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Create Class",
                            fontFamily = NeueMachina,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}