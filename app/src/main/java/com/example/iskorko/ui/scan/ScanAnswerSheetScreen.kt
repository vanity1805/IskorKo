package com.example.iskorko.ui.scan

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import com.example.iskorko.ui.scan.CameraScannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanAnswerSheetScreen(
    navController: NavHostController,
    examId: String,
    classId: String,
    viewModel: ScanAnswerSheetViewModel = viewModel()
) {
    val context = LocalContext.current
    var showStudentPicker by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    
    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }
    
    // Use content URI approach (simpler, no FileProvider needed)
    var imageUri by remember { mutableStateOf<android.net.Uri?>(null) }

    
    LaunchedEffect(examId) {
        viewModel.loadExamDetails(examId)
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Answer Sheet",
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
            when (viewModel.scanStep.value) {
                 ScanStep.CAMERA -> CameraScannerView(
                examName = viewModel.examName.value,
                totalQuestions = viewModel.totalQuestions.value,
                onScanComplete = { bitmap ->
                    viewModel.onImageCaptured(bitmap)
                    viewModel.processImage(context)
                },
                onCancel = { navController.popBackStack() }
            )
                
                ScanStep.PROCESSING -> ProcessingView()
                
                ScanStep.PREVIEW -> PreviewView(
                    bitmap = viewModel.capturedImage.value,
                    onRetake = { viewModel.retakePhoto() },
                    onProcess = { viewModel.processImage(context) }
                )
                
                ScanStep.REVIEW -> ReviewView(
                    viewModel = viewModel,
                    examId = examId,
                    classId = classId,
                    onSave = {
                        if (viewModel.selectedStudent.value == null) {
                            showStudentPicker = true
                        } else {
                            viewModel.saveGrade(
                                examId = examId,
                                classId = classId,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "Grade saved successfully!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.popBackStack()
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            }
        }
    }
    
    // Student Picker Dialog
    if (showStudentPicker) {
        StudentPickerDialog(
            classId = classId,
            onDismiss = { showStudentPicker = false },
            onStudentSelected = { studentId ->
                viewModel.selectStudent(studentId)
                showStudentPicker = false
            }
        )
    }
}

@Composable
fun CameraView(
    examName: String,
    onTakePhoto: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF800202)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = examName,
                    fontFamily = NeueMachina,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Position the answer sheet within the frame",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800202)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Choose Photo",
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Tips:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "• Ensure good lighting\n• Keep paper flat\n• Avoid shadows\n• Capture entire sheet",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PreviewView(
    bitmap: Bitmap?,
    onRetake: () -> Unit,
    onProcess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Captured image",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retake")
            }
            
            Button(
                onClick = onProcess,
                modifier = Modifier.weight(1f).height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Process")
            }
        }
    }
}

@Composable
fun ProcessingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = Color(0xFF800202)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Processing answer sheet...",
                fontFamily = NeueMachina,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "This may take a few seconds",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReviewView(
    viewModel: ScanAnswerSheetViewModel,
    examId: String,
    classId: String,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Score Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Detected Score",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${viewModel.calculateScore()}/${viewModel.totalQuestions.value}",
                    fontFamily = NeueMachina,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
                val percentage = (viewModel.calculateScore().toFloat() / viewModel.totalQuestions.value.toFloat()) * 100
                Text(
                    text = String.format("%.1f%%", percentage),
                    fontSize = 18.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Detection Confidence
                val confidence = viewModel.detectionConfidence.value
                val confidenceColor = when {
                    confidence >= 0.8f -> Color(0xFF4CAF50)
                    confidence >= 0.5f -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                }
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = confidenceColor.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (confidence >= 0.8f) Icons.Filled.CheckCircle 
                            else if (confidence >= 0.5f) Icons.Filled.Warning 
                            else Icons.Filled.Error,
                            contentDescription = null,
                            tint = confidenceColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Detection: ${String.format("%.0f%%", confidence * 100)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = confidenceColor
                        )
                    }
                }
                
                if (confidence < 0.8f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Please review and correct answers below",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Review & Edit Answers",
            fontFamily = NeueMachina,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Tap to edit any incorrect detections",
            fontSize = 12.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Answers List
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(viewModel.detectedAnswers.value) { index, answer ->
                AnswerReviewCard(
                    questionNumber = index + 1,
                    detectedAnswer = answer,
                    correctAnswer = viewModel.answerKey.value.getOrNull(index),
                    onAnswerChange = { newAnswer ->
                        viewModel.onAnswerChange(index, newAnswer)
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF800202)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Save Grade",
                fontFamily = NeueMachina,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AnswerReviewCard(
    questionNumber: Int,
    detectedAnswer: String,
    correctAnswer: String?,
    onAnswerChange: (String) -> Unit
) {
    val isCorrect = detectedAnswer == correctAnswer
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Q$questionNumber",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("A", "B", "C", "D", "E").forEach { option ->
                    SmallAnswerButton(
                        text = option,
                        isSelected = detectedAnswer == option,
                        onClick = { onAnswerChange(option) }
                    )
                }
            }
        }
    }
}

@Composable
fun SmallAnswerButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF800202) else Color(0xFFEEEEEE),
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        contentPadding = PaddingValues(0.dp),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StudentPickerDialog(
    classId: String,
    onDismiss: () -> Unit,
    onStudentSelected: (String) -> Unit
) {
    var students by remember { mutableStateOf<List<StudentInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val db = FirebaseFirestore.getInstance()
    
    LaunchedEffect(classId) {
        db.collection("classes")
            .document(classId)
            .get()
            .addOnSuccessListener { classDoc ->
                val studentIds = classDoc.get("studentIds") as? List<String> ?: emptyList()
                
                if (studentIds.isEmpty()) {
                    students = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }
                
                // Load student details
                val batchSize = 10
                val batches = studentIds.chunked(batchSize)
                val allStudents = mutableListOf<StudentInfo>()
                var processedBatches = 0
                
                batches.forEach { batch ->
                    db.collection("users")
                        .whereIn("__name__", batch)
                        .get()
                        .addOnSuccessListener { documents ->
                            val loadedStudents = documents.map { doc ->
                                StudentInfo(
                                    id = doc.id,
                                    fullName = doc.getString("fullName") ?: "Unknown",
                                    email = doc.getString("email") ?: ""
                                )
                            }
                            allStudents.addAll(loadedStudents)
                            
                            processedBatches++
                            if (processedBatches == batches.size) {
                                students = allStudents.sortedBy { it.fullName }
                                isLoading = false
                            }
                        }
                        .addOnFailureListener {
                            processedBatches++
                            if (processedBatches == batches.size) {
                                students = allStudents.sortedBy { it.fullName }
                                isLoading = false
                            }
                        }
                }
            }
            .addOnFailureListener {
                isLoading = false
            }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Student",
                fontFamily = NeueMachina,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF800202)
                    )
                } else if (students.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No students enrolled",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn {
                        items(students) { student ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onStudentSelected(student.id)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF1976D2), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = student.fullName.firstOrNull()?.toString() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column {
                                        Text(
                                            text = student.fullName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = student.email,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

data class StudentInfo(
    val id: String = "",
    val fullName: String = "",
    val email: String = ""
)