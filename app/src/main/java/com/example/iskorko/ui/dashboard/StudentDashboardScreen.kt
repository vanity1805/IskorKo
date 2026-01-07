package com.example.iskorko.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    navController: NavHostController,
    viewModel: StudentDashboardViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showJoinDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hello, ${viewModel.studentName.value.split(" ").firstOrNull() ?: "Student"}",
                            fontFamily = NeueMachina,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ready to learn!",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Notifications */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
                    IconButton(onClick = { selectedTab = 2 }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Classes") },
                    label = { Text("Classes") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Assessment, contentDescription = "Grades") },
                    label = { Text("Grades") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = Color(0xFF800202),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Join Class")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDEDED))
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> StudentClassesTab(
                    classes = viewModel.classes.value,
                    isLoading = viewModel.isLoading.value,
                    navController = navController,
                    onLeaveClass = { classId ->
                        viewModel.leaveClass(
                            classId = classId,
                            onSuccess = {
                                Toast.makeText(
                                    navController.context,
                                    "Left class successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onError = { error ->
                                Toast.makeText(
                                    navController.context,
                                    error,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                )
                1 -> StudentGradesTab()
                2 -> StudentProfileTab(
                    studentName = viewModel.studentName.value,
                    onLogout = {
                        viewModel.logout {
                            navController.navigate("chooseProfile") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }
        }
    }
    
    // Join Class Dialog
    if (showJoinDialog) {
        JoinClassDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { classCode ->
                viewModel.joinClass(
                    classCode = classCode,
                    onSuccess = {
                        Toast.makeText(
                            navController.context,
                            "Joined class successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        showJoinDialog = false
                    },
                    onError = { error ->
                        Toast.makeText(
                            navController.context,
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        )
    }
}

@Composable
fun JoinClassDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var classCode by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Join Class",
                fontFamily = NeueMachina,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Enter the class code provided by your professor")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = classCode,
                    onValueChange = { classCode = it.uppercase() },
                    label = { Text("Class Code") },
                    placeholder = { Text("e.g., ABC123") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(classCode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                )
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StudentClassesTab(
    classes: List<StudentClassItem>,
    isLoading: Boolean,
    navController: NavHostController,
    onLeaveClass: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StudentStatCard(
                number = classes.size.toString(),
                label = "Classes",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StudentStatCard(
                number = classes.sumOf { it.examCount }.toString(),
                label = "Exams",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "My Classes",
            fontFamily = NeueMachina,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF800202))
            }
        } else if (classes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No classes yet",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Tap + to join a class",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classes) { classItem ->
                    StudentClassCard(
                        classItem = classItem,
                        onClick = {navController.navigate("studentClassView/${classItem.id}")
                            // TODO: Navigate to student class view
                            // navController.navigate("studentClassView/${classItem.id}")
                        },
                        onLeave = { onLeaveClass(classItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentStatCard(number: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = number,
                fontFamily = NeueMachina,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StudentClassCard(
    classItem: StudentClassItem,
    onClick: () -> Unit,
    onLeave: () -> Unit
) {
    var showLeaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFE3F2FD), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = Color(0xFF1976D2)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = classItem.className,
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = classItem.section,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                IconButton(onClick = { showLeaveDialog = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = classItem.professorName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                Text(
                    text = "${classItem.examCount} exams",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
    
    // Leave Class Dialog
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Class?", fontFamily = NeueMachina) },
            text = {
                Text("Are you sure you want to leave ${classItem.className}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLeave()
                        showLeaveDialog = false
                    }
                ) {
                    Text("Leave", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentGradesTab() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Assessment,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Grades",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Coming soon...",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun StudentProfileTab(studentName: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF1976D2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = studentName.firstOrNull()?.toString() ?: "S",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = studentName,
                        fontFamily = NeueMachina,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Student",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO */ }
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Edit Profile", fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { /* TODO */ }
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF1976D2))
                Spacer(modifier = Modifier.width(16.dp))
                Text(text = "Change Password", fontSize = 16.sp, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF800202)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                fontFamily = NeueMachina,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}