package com.example.iskorko.ui.classes

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    navController: NavHostController,
    classId: String,
    viewModel: ClassDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    
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
                actions = {
                    IconButton(
                        onClick = {
                            val shareText = "Join my class \"${viewModel.className.value}\" " +
                                "(${viewModel.section.value}) with code: ${viewModel.classCode.value}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share class code")
                            )
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = {navController.navigate("createExam/$classId") 
                        // TODO: Navigate to create exam
                        // navController.navigate("createExam/$classId")
                    },
                    containerColor = Color(0xFF800202),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Exam")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(paddingValues)
        ) {
            // Class Code Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
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
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            // Copy to clipboard
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                                as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText(
                                "Class Code", 
                                viewModel.classCode.value
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            contentDescription = "Copy code",
                            tint = Color(0xFF800202)
                        )
                    }
                }
            }
            
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { 
                        Text(
                            "Roster (${viewModel.students.value.size})",
                            fontFamily = NeueMachina
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { 
                        Text(
                            "Exams (${viewModel.exams.value.size})",
                            fontFamily = NeueMachina
                        ) 
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { 
                        Text("Settings", fontFamily = NeueMachina) 
                    }
                )
            }
            
            // Tab Content
            when (selectedTab) {
                0 -> RosterTab(
                    students = viewModel.students.value,
                    isLoading = viewModel.isLoading.value,
                    onRemoveStudent = { student ->
                        viewModel.removeStudentFromClass(
                            classId = classId,
                            studentId = student.id,
                            onSuccess = {
                                Toast.makeText(context, "Student removed", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                )
                1 -> ExamsTab(viewModel.exams.value, viewModel.isLoading.value, navController)
                2 -> SettingsTab(
                    classId = classId,
                    className = viewModel.className.value,
                    section = viewModel.section.value,
                    archived = viewModel.archived.value,
                    students = viewModel.students.value,
                    onUpdateClassInfo = { updatedName, updatedSection ->
                        viewModel.updateClassInfo(
                            classId = classId,
                            updatedName = updatedName,
                            updatedSection = updatedSection,
                            onSuccess = {
                                Toast.makeText(context, "Class updated", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onArchiveClass = {
                        viewModel.archiveClass(
                            classId = classId,
                            onSuccess = {
                                Toast.makeText(context, "Class archived", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onUnarchiveClass = {
                        viewModel.unarchiveClass(
                            classId = classId,
                            onSuccess = {
                                Toast.makeText(context, "Class unarchived", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    onDeleteClass = { showDeleteDialog = true }
                )
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Class?", fontFamily = NeueMachina) },
            text = { 
                Text("Are you sure you want to delete this class? This action cannot be undone.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClass(
                            classId = classId,
                            onSuccess = {
                                Toast.makeText(context, "Class deleted", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { error ->
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RosterTab(
    students: List<StudentInfo>,
    isLoading: Boolean,
    onRemoveStudent: (StudentInfo) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (students.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Filled.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No students yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Students can join using the class code",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(students) { student ->
                StudentCard(student = student, onRemove = { onRemoveStudent(student) })
            }
        }
    }
}

@Composable
fun StudentCard(student: StudentInfo, onRemove: () -> Unit) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
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
                    fontSize = 16.sp
                )
                Text(
                    text = student.email,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = { showRemoveDialog = true }) {
                Icon(
                    Icons.Filled.PersonRemove,
                    contentDescription = "Remove student",
                    tint = Color(0xFF800202)
                )
            }
        }
    }
    
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove Student?", fontFamily = NeueMachina) },
            text = { Text("Remove ${student.fullName} from this class?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemove()
                        showRemoveDialog = false
                    }
                ) {
                    Text("Remove", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExamsTab(exams: List<ExamInfo>, isLoading: Boolean, navController: NavHostController) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (exams.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
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
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Text(
                    text = "Tap + to create your first exam",
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
            items(exams) { exam ->
                ExamCard(exam, onClick = {navController.navigate("examDetail/${exam.id}")
                    // TODO: Navigate to exam details
                    // navController.navigate("examDetail/${exam.id}")
                })
            }
        }
    }
}

@Composable
fun ExamCard(exam: ExamInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Description,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
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
            
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsTab(
    classId: String,
    className: String,
    section: String,
    archived: Boolean,
    students: List<StudentInfo>,
    onUpdateClassInfo: (String, String) -> Unit,
    onArchiveClass: () -> Unit,
    onUnarchiveClass: () -> Unit,
    onDeleteClass: () -> Unit
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var editName by remember(className) { mutableStateOf(className) }
    var editSection by remember(section) { mutableStateOf(section) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Class Actions",
                    fontFamily = NeueMachina,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE3F2FD),
                        contentColor = Color(0xFF1976D2)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Class Info", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = {
                        if (students.isEmpty()) {
                            Toast.makeText(context, "No students to export", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val header = "Full Name,Email"
                        val rows = students.joinToString("\n") { student ->
                            val safeName = student.fullName.replace("\"", "\"\"")
                            val safeEmail = student.email.replace("\"", "\"\"")
                            "\"$safeName\",\"$safeEmail\""
                        }
                        val csv = "$header\n$rows"
                        val file = File(context.cacheDir, "class_${classId}_roster.csv")
                        file.writeText(csv)

                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "Class roster")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Export roster")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8F5E9),
                        contentColor = Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Roster (CSV)", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { showArchiveDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (archived) Color(0xFFE8F5E9) else Color(0xFFFFF8E1),
                        contentColor = if (archived) Color(0xFF2E7D32) else Color(0xFFF57C00)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        if (archived) Icons.Filled.Unarchive else Icons.Filled.Inventory2,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (archived) "Unarchive Class" else "Archive Class",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = onDeleteClass,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFEBEE),
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Class", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Class Info", fontFamily = NeueMachina) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Class Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editSection,
                        onValueChange = { editSection = it },
                        label = { Text("Section") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmedName = editName.trim()
                        val trimmedSection = editSection.trim()
                        if (trimmedName.isBlank() || trimmedSection.isBlank()) {
                            Toast.makeText(
                                context,
                                "Class name and section are required",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@TextButton
                        }
                        onUpdateClassInfo(trimmedName, trimmedSection)
                        showEditDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = {
                Text(
                    if (archived) "Unarchive Class?" else "Archive Class?",
                    fontFamily = NeueMachina
                )
            },
            text = {
                Text(
                    if (archived) {
                        "This class will be restored to active lists."
                    } else {
                        "Archiving hides this class from active lists. You can restore it later."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (archived) {
                            onUnarchiveClass()
                        } else {
                            onArchiveClass()
                        }
                        showArchiveDialog = false
                    }
                ) {
                    Text(
                        if (archived) "Unarchive" else "Archive",
                        color = if (archived) Color(0xFF2E7D32) else Color(0xFFF57C00)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
