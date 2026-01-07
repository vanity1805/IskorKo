package com.example.iskorko.ui.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradeEntryScreen(
    navController: NavHostController,
    examId: String,
    viewModel: GradeEntryViewModel = viewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(examId) {
        viewModel.loadExamAndStudents(examId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.examName.value,
                            fontFamily = NeueMachina,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.className.value,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
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
        ) {
            // Stats Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.students.value.size}",
                            fontFamily = NeueMachina,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Text(
                            text = "Total Students",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.students.value.count { it.isGraded }}",
                            fontFamily = NeueMachina,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Graded",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${viewModel.students.value.count { !it.isGraded }}",
                            fontFamily = NeueMachina,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                        Text(
                            text = "Pending",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Students List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Students",
                    fontFamily = NeueMachina,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Out of ${viewModel.totalQuestions.value}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            // Students List
            if (viewModel.isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF800202))
                }
            } else if (viewModel.students.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No students enrolled",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.students.value) { student ->
                        StudentGradeCard(
                            student = student,
                            totalQuestions = viewModel.totalQuestions.value,
                            onSaveGrade = { score ->
                                viewModel.saveGrade(
                                    examId = examId,
                                    studentId = student.studentId,
                                    score = score,
                                    onSuccess = {
                                        Toast.makeText(
                                            context,
                                            "Grade saved for ${student.studentName}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        // Reload to update the list
                                        viewModel.loadExamAndStudents(examId)
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentGradeCard(
    student: StudentGradeItem,
    totalQuestions: Int,
    onSaveGrade: (Int) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (student.isGraded) Color(0xFF4CAF50) else Color(0xFF1976D2),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.studentName.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Student Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.studentName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = student.studentEmail,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Score or Enter Button
            if (student.isGraded) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = "${student.score}/$totalQuestions",
                        fontFamily = NeueMachina,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    student.percentage?.let { percentage ->
                        Text(
                            text = String.format("%.1f%%", percentage),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            // Edit/Enter Button
            IconButton(
                onClick = { showDialog = true }
            ) {
                Icon(
                    if (student.isGraded) Icons.Filled.Edit else Icons.Filled.Add,
                    contentDescription = if (student.isGraded) "Edit grade" else "Enter grade",
                    tint = Color(0xFF800202)
                )
            }
        }
    }
    
    // Grade Entry Dialog
    if (showDialog) {
        GradeEntryDialog(
            studentName = student.studentName,
            currentScore = student.score,
            totalQuestions = totalQuestions,
            onDismiss = { showDialog = false },
            onSave = { score ->
                onSaveGrade(score)
                showDialog = false
            }
        )
    }
}

@Composable
fun GradeEntryDialog(
    studentName: String,
    currentScore: Int?,
    totalQuestions: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var scoreText by remember { mutableStateOf(currentScore?.toString() ?: "") }
    var errorText by remember { mutableStateOf<String?>(null) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter Grade",
                fontFamily = NeueMachina,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Student: $studentName")
                Text(
                    text = "Total Questions: $totalQuestions",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            scoreText = it
                            errorText = null
                        }
                    },
                    label = { Text("Score") },
                    placeholder = { Text("Enter score (0-$totalQuestions)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorText != null
                )
                
                errorText?.let { error ->
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val score = scoreText.toIntOrNull()
                    when {
                        score == null -> {
                            errorText = "Please enter a valid number"
                        }
                        score < 0 -> {
                            errorText = "Score cannot be negative"
                        }
                        score > totalQuestions -> {
                            errorText = "Score cannot exceed $totalQuestions"
                        }
                        else -> {
                            onSave(score)
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}