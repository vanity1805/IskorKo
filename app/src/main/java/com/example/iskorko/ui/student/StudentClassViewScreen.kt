package com.example.iskorko.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentClassViewScreen(
    navController: NavHostController,
    classId: String,
    viewModel: StudentClassViewModel = viewModel()
) {
    LaunchedEffect(classId) {
        viewModel.loadClassDetails(classId)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.className.value,
                            fontFamily = NeueMachina,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.section.value,
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
            // Class Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Class Code",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = viewModel.classCode.value,
                                fontFamily = NeueMachina,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Divider()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Professor",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = viewModel.professorName.value,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Students",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color(0xFF1976D2)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${viewModel.studentCount.value}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
            
            // Exams Section
            Text(
                text = "Exams & Grades",
                fontFamily = NeueMachina,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (viewModel.isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1976D2))
                }
            } else if (viewModel.exams.value.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No exams yet",
                            fontSize = 18.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Your professor hasn't created any exams",
                            fontSize = 14.sp,
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
                    items(viewModel.exams.value) { exam ->
                        StudentExamCard(exam)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentExamCard(exam: StudentExamItem) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (exam.studentScore != null) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (exam.studentScore != null) Icons.Filled.CheckCircle else Icons.Filled.Description,
                        contentDescription = null,
                        tint = if (exam.studentScore != null) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exam.examName,
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${exam.totalQuestions} questions",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Score or Status
                if (exam.studentScore != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${exam.studentScore}/${exam.totalQuestions}",
                            fontFamily = NeueMachina,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        exam.percentage?.let { percentage ->
                            Text(
                                text = String.format("%.1f%%", percentage),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF3E0)
                    ) {
                        Text(
                            text = "Not taken",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }
            
            // Grade Breakdown (if graded)
            if (exam.studentScore != null && exam.percentage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = (exam.percentage / 100f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        exam.percentage >= 90 -> Color(0xFF4CAF50)
                        exam.percentage >= 75 -> Color(0xFF2196F3)
                        exam.percentage >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    trackColor = Color(0xFFEEEEEE)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = when {
                            exam.percentage >= 90 -> "Excellent!"
                            exam.percentage >= 75 -> "Good job!"
                            exam.percentage >= 60 -> "Passed"
                            else -> "Needs improvement"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            exam.percentage >= 90 -> Color(0xFF4CAF50)
                            exam.percentage >= 75 -> Color(0xFF2196F3)
                            exam.percentage >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    
                    Text(
                        text = "${exam.totalQuestions - (exam.studentScore ?: 0)} incorrect",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}