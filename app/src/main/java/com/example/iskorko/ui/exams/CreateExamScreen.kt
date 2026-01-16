package com.example.iskorko.ui.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateExamScreen(
    navController: NavHostController,
    classId: String,
    viewModel: CreateExamViewModel = viewModel()
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create Exam",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Exam Information Card
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
                        text = "Exam Information",
                        fontFamily = NeueMachina,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = viewModel.examName.value,
                        onValueChange = viewModel::onExamNameChange,
                        label = { Text("Exam Name") },
                        placeholder = { Text("e.g., Midterm Exam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isLoading.value
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Number of Questions Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    val questionOptions = listOf(20, 50, 100)
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { if (!viewModel.isLoading.value) expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = if (viewModel.totalQuestions.value.isNotEmpty()) 
                                "${viewModel.totalQuestions.value} Questions" else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Number of Questions") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            enabled = !viewModel.isLoading.value
                        )
                        
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            questionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text("$option Questions") },
                                    onClick = {
                                        viewModel.onTotalQuestionsChange(option.toString())
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Select 20, 50, or 100 questions",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Answer Key Card
            if (viewModel.answerKey.value.isNotEmpty()) {
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
                            text = "Answer Key",
                            fontFamily = NeueMachina,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Set the correct answer for each question",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Answer Options - Use Column instead of Grid
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            viewModel.answerKey.value.forEachIndexed { index, answer ->
                                QuestionAnswerRow(
                                    questionNumber = index + 1,
                                    selectedAnswer = answer,
                                    onAnswerSelected = { newAnswer ->
                                        viewModel.onAnswerChange(index, newAnswer)
                                    },
                                    enabled = !viewModel.isLoading.value
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
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
            
            // Create Button
            Button(
                onClick = {
                    viewModel.createExam(
                        classId = classId,
                        onSuccess = {
                            Toast.makeText(
                                context,
                                "Exam created successfully!",
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
                        text = "Create Exam",
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun QuestionAnswerRow(
    questionNumber: Int,
    selectedAnswer: String,
    onAnswerSelected: (String) -> Unit,
    enabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Q$questionNumber",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.width(40.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("A", "B", "C", "D", "E").forEach { option ->
                    AnswerButton(
                        text = option,
                        isSelected = selectedAnswer == option,
                        onClick = { onAnswerSelected(option) },
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF800202) else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black,
            disabledContainerColor = if (isSelected) Color(0xFF800202).copy(alpha = 0.6f) else Color.White,
            disabledContentColor = if (isSelected) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}