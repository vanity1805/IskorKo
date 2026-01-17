package com.example.iskorko.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iskorko.ui.components.IskorKoToastHost
import com.example.iskorko.ui.components.rememberToastState
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    navController: NavHostController,
    viewModel: StudentDashboardViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    val toastState = rememberToastState()
    
    Box(modifier = Modifier.fillMaxSize()) {
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
                    BadgedBox(
                        badge = {
                            if (viewModel.unreadNotificationCount.value > 0) {
                                Badge(
                                    containerColor = Color(0xFF800202)
                                ) {
                                    Text(
                                        text = if (viewModel.unreadNotificationCount.value > 9) "9+" 
                                               else viewModel.unreadNotificationCount.value.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = { showNotifications = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                    }
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
                    archivedClasses = viewModel.archivedClasses.value,
                    isLoading = viewModel.isLoading.value,
                    navController = navController,
                    onLeaveClass = { classId ->
                        viewModel.leaveClass(
                            classId = classId,
                            onSuccess = {
                                toastState.showSuccess("Left class successfully")
                            },
                            onError = { error ->
                                toastState.showError(error)
                            }
                        )
                    }
                )
                1 -> StudentGradesTab(viewModel = viewModel)
                2 -> StudentProfileTab(
                    viewModel = viewModel,
                    toastState = toastState,
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
                        toastState.showSuccess("Joined class successfully!")
                        showJoinDialog = false
                    },
                    onError = { error ->
                        toastState.showError(error)
                    }
                )
            }
        )
    }
    
    // Notifications Bottom Sheet
    if (showNotifications) {
        val classLabelById = remember(viewModel.classes.value, viewModel.archivedClasses.value) {
            (viewModel.classes.value + viewModel.archivedClasses.value).associate { classItem ->
                classItem.id to "${classItem.className} - ${classItem.section}"
            }
        }
        StudentNotificationsBottomSheet(
            notifications = viewModel.notifications.value,
            classLabelById = classLabelById,
            onDismiss = { showNotifications = false },
            onMarkAsRead = { viewModel.markNotificationAsRead(it) },
            onMarkAllAsRead = { viewModel.markAllNotificationsAsRead() }
        )
    }
    
    // Toast overlay
    IskorKoToastHost(state = toastState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentNotificationsBottomSheet(
    notifications: List<NotificationItem>,
    classLabelById: Map<String, String>,
    onDismiss: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
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
                Text(
                    text = "Notifications",
                    fontFamily = NeueMachina,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (notifications.any { !it.isRead }) {
                    TextButton(onClick = onMarkAllAsRead) {
                        Text("Mark all as read", color = Color(0xFF800202))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No notifications yet",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(notifications) { notification ->
                        StudentNotificationCard(
                            notification = notification,
                            classLabel = classLabelById[notification.relatedId]
                                ?: notification.classLabel,
                            onClick = { onMarkAsRead(notification.id) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StudentNotificationCard(
    notification: NotificationItem,
    classLabel: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) Color(0xFFF5F5F5) else Color(0xFFFFF3F3)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (notification.type) {
                            NotificationType.NEW_GRADE -> Color(0xFF4CAF50)
                            NotificationType.NEW_STUDENT -> Color(0xFF2196F3)
                            NotificationType.CLASS_UPDATE -> Color(0xFF9C27B0)
                            NotificationType.EXAM_CREATED -> Color(0xFFFF9800)
                            NotificationType.SYSTEM -> Color(0xFF607D8B)
                        }.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.type) {
                        NotificationType.NEW_GRADE -> Icons.Filled.Assessment
                        NotificationType.NEW_STUDENT -> Icons.Filled.PersonAdd
                        NotificationType.CLASS_UPDATE -> Icons.Filled.Class
                        NotificationType.EXAM_CREATED -> Icons.Filled.Quiz
                        NotificationType.SYSTEM -> Icons.Filled.Info
                    },
                    contentDescription = null,
                    tint = when (notification.type) {
                        NotificationType.NEW_GRADE -> Color(0xFF4CAF50)
                        NotificationType.NEW_STUDENT -> Color(0xFF2196F3)
                        NotificationType.CLASS_UPDATE -> Color(0xFF9C27B0)
                        NotificationType.EXAM_CREATED -> Color(0xFFFF9800)
                        NotificationType.SYSTEM -> Color(0xFF607D8B)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = notification.getTimeAgo(),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                if (!classLabel.isNullOrBlank()) {
                    Text(
                        text = classLabel,
                        fontSize = 12.sp,
                        color = Color(0xFF800202),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = notification.message,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }
            
            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF800202), CircleShape)
                )
            }
        }
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
    archivedClasses: List<StudentClassItem>,
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
        } else if (classes.isEmpty() && archivedClasses.isEmpty()) {
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
                if (classes.isEmpty()) {
                    item {
                        Text(
                            text = "No active classes",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
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
                
                if (archivedClasses.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Archived Classes",
                            fontFamily = NeueMachina,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(archivedClasses) { classItem ->
                        StudentClassCard(
                            classItem = classItem,
                            onClick = { navController.navigate("studentClassView/${classItem.id}") },
                            onLeave = { onLeaveClass(classItem.id) }
                        )
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentGradesTab(viewModel: StudentDashboardViewModel) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<StudentGradeItem?>(null) }
    var viewMode by remember { mutableStateOf(0) } // 0 = All, 1 = By Class
    var isContentVisible by remember { mutableStateOf(false) }
    
    // Trigger enter animation
    LaunchedEffect(Unit) {
        isContentVisible = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
    ) {
        // Animated Statistics Overview (slides down from top)
        AnimatedVisibility(
            visible = isContentVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> -fullHeight },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        ) {
            StudentGradeStatisticsSection(viewModel.studentStatistics.value)
        }
        
        // Animated Content Container (slides up from bottom)
        AnimatedVisibility(
            visible = isContentVisible,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = Color.White,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // View Mode Toggle
                    TabRow(
                        selectedTabIndex = viewMode,
                        containerColor = Color.White,
                        contentColor = Color(0xFF1976D2),
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    ) {
                        Tab(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            text = { Text("All Grades") }
                        )
                        Tab(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            text = { Text("By Class") }
                        )
                    }
                    
                    // Search and Filter Bar
                    StudentSearchAndFilterBar(
                        searchQuery = viewModel.searchQuery.value,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFilterClick = { showFilterMenu = true },
                        onSortClick = { showSortMenu = true },
                        hasFilters = viewModel.selectedClassFilter.value != null
                    )
                    
                    // Content
                    when (viewMode) {
                        0 -> StudentAllGradesView(
                            grades = viewModel.getFilteredAndSortedGrades(),
                            isLoading = viewModel.isLoadingGrades.value,
                            onGradeClick = { selectedGrade = it }
                        )
                        1 -> StudentByClassView(
                            classGrades = viewModel.classGrades.value,
                            isLoading = viewModel.isLoadingGrades.value,
                            onGradeClick = { selectedGrade = it }
                        )
                    }
                }
            }
        }
    }
    
    // Sort Bottom Sheet
    if (showSortMenu) {
        StudentSortBottomSheet(
            selectedOption = viewModel.selectedSortOption.value,
            onOptionSelected = {
                viewModel.setSortOption(it)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }
    
    // Filter Bottom Sheet
    if (showFilterMenu) {
        StudentFilterBottomSheet(
            classes = viewModel.classes.value,
            selectedClassId = viewModel.selectedClassFilter.value,
            onClassSelected = { viewModel.setClassFilter(it) },
            onDismiss = { showFilterMenu = false },
            onClearFilters = { viewModel.setClassFilter(null) }
        )
    }
    
    // Grade Detail Dialog
    selectedGrade?.let { grade ->
        StudentGradeDetailDialog(
            grade = grade,
            onDismiss = { selectedGrade = null }
        )
    }
}

@Composable
fun StudentGradeStatisticsSection(stats: StudentStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Performance",
                    fontFamily = NeueMachina,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Improvement trend indicator
                if (stats.improvementTrend != 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (stats.improvementTrend > 0) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (stats.improvementTrend > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "${if (stats.improvementTrend > 0) "+" else ""}${stats.improvementTrend.toInt()}%",
                            fontSize = 12.sp,
                            color = if (stats.improvementTrend > 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StudentStatItem(
                    label = "Exams",
                    value = stats.totalExamsTaken.toString(),
                    icon = Icons.Filled.Quiz
                )
                StudentStatItem(
                    label = "Average",
                    value = "${stats.averageScore.toInt()}%",
                    icon = Icons.Filled.Assessment,
                    valueColor = getScoreColor(stats.averageScore)
                )
                StudentStatItem(
                    label = "Best",
                    value = "${stats.highestScore.toInt()}%",
                    icon = Icons.Filled.EmojiEvents,
                    valueColor = Color(0xFF4CAF50)
                )
                StudentStatItem(
                    label = "Perfect",
                    value = stats.perfectScores.toString(),
                    icon = Icons.Filled.Star,
                    valueColor = Color(0xFFFFC107)
                )
            }
        }
    }
}

@Composable
fun StudentStatItem(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = Color(0xFF1976D2)
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = valueColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = NeueMachina,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun StudentSearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    hasFilters: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Search exams, classes...") },
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = null)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1976D2),
                focusedLabelColor = Color(0xFF1976D2)
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Badge(
            modifier = Modifier,
            containerColor = if (hasFilters) Color(0xFF1976D2) else Color.Transparent
        ) {
            IconButton(onClick = onFilterClick) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filter",
                    tint = if (hasFilters) Color(0xFF1976D2) else Color.Gray
                )
            }
        }
        
        IconButton(onClick = onSortClick) {
            Icon(Icons.Filled.Sort, contentDescription = "Sort")
        }
    }
}

@Composable
fun StudentAllGradesView(
    grades: List<StudentGradeItem>,
    isLoading: Boolean,
    onGradeClick: (StudentGradeItem) -> Unit
) {
    if (isLoading) {
    Box(
        modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF1976D2))
        }
    } else if (grades.isEmpty()) {
        EmptyStudentGradesView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(grades) { grade ->
                StudentGradeCard(grade = grade, onClick = { onGradeClick(grade) })
            }
        }
    }
}

@Composable
fun StudentGradeCard(grade: StudentGradeItem, onClick: () -> Unit) {
    val scoreColor = getScoreColor(grade.percentage)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score Circle
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(scoreColor.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${grade.percentage.toInt()}%",
                        fontFamily = NeueMachina,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = "${grade.score}/${grade.totalPoints}",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Grade Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.examName,
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = grade.className,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Date
            Icon(
                        Icons.Filled.CalendarToday,
                contentDescription = null,
                        modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
            Text(
                        text = " ${formatStudentDate(grade.gradedDate)}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    
                    // Rank if available
                    if (grade.rank > 0) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Filled.Leaderboard,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (grade.rank <= 3) Color(0xFFFFC107) else Color.Gray
                        )
                        Text(
                            text = " #${grade.rank}",
                            fontSize = 11.sp,
                            color = if (grade.rank <= 3) Color(0xFFFFC107) else Color.Gray,
                            fontWeight = if (grade.rank <= 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            
            // Comparison with class average
            if (grade.classAverage > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val diff = grade.percentage - grade.classAverage
                    Icon(
                        if (diff >= 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${if (diff >= 0) "+" else ""}${diff.toInt()}%",
                        fontSize = 10.sp,
                        color = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontWeight = FontWeight.Bold
            )
            Text(
                        text = "vs avg",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun StudentByClassView(
    classGrades: List<StudentClassGrades>,
    isLoading: Boolean,
    onGradeClick: (StudentGradeItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF1976D2))
        }
    } else if (classGrades.isEmpty()) {
        EmptyStudentGradesView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(classGrades) { classGroup ->
                StudentClassGradesCard(
                    classGroup = classGroup,
                    onGradeClick = onGradeClick
                )
            }
        }
    }
}

@Composable
fun StudentClassGradesCard(
    classGroup: StudentClassGrades,
    onGradeClick: (StudentGradeItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Class icon with average color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            getScoreColor(classGroup.averageScore).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${classGroup.averageScore.toInt()}%",
                        fontFamily = NeueMachina,
                fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = getScoreColor(classGroup.averageScore)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = classGroup.className,
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${classGroup.examCount} exams · Avg: ${classGroup.averageScore.toInt()}%",
                        fontSize = 12.sp,
                color = Color.Gray
            )
        }
                
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                classGroup.grades.forEach { grade ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGradeClick(grade) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = grade.examName,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${grade.percentage.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = getScoreColor(grade.percentage)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStudentGradesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.Assessment,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.LightGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Grades Yet",
                fontFamily = NeueMachina,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your exam results will appear here\nonce your professor grades them",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSortBottomSheet(
    selectedOption: StudentGradeSortOption,
    onOptionSelected: (StudentGradeSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
    Column(
        modifier = Modifier
                .fillMaxWidth()
            .padding(16.dp)
    ) {
            Text(
                text = "Sort Grades",
                fontFamily = NeueMachina,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val sortOptions = listOf(
                StudentGradeSortOption.DATE_DESC to "Newest First",
                StudentGradeSortOption.DATE_ASC to "Oldest First",
                StudentGradeSortOption.SCORE_DESC to "Highest Score",
                StudentGradeSortOption.SCORE_ASC to "Lowest Score",
                StudentGradeSortOption.EXAM_NAME to "Exam Name (A-Z)",
                StudentGradeSortOption.CLASS_NAME to "Class Name (A-Z)"
            )
            
            sortOptions.forEach { (option, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOptionSelected(option) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = { onOptionSelected(option) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF1976D2)
                        )
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Text(
                        text = label,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentFilterBottomSheet(
    classes: List<StudentClassItem>,
    selectedClassId: String?,
    onClassSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    onClearFilters: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
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
                Text(
                    text = "Filter by Class",
                    fontFamily = NeueMachina,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onClearFilters) {
                    Text("Clear", color = Color(0xFF1976D2))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All Classes option
            FilterChip(
                selected = selectedClassId == null,
                onClick = { 
                    onClassSelected(null)
                    onDismiss()
                },
                label = { Text("All Classes") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Individual classes
            classes.forEach { classItem ->
                FilterChip(
                    selected = selectedClassId == classItem.id,
                    onClick = { 
                        onClassSelected(classItem.id)
                        onDismiss()
                    },
                    label = { Text("${classItem.className} - ${classItem.section}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StudentGradeDetailDialog(
    grade: StudentGradeItem,
    onDismiss: () -> Unit
) {
    val scoreColor = getScoreColor(grade.percentage)
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Text(
                        text = "Grade Details",
                        fontFamily = NeueMachina,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Score Display
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(scoreColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                            text = "${grade.percentage.toInt()}%",
                            fontFamily = NeueMachina,
                            fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Text(
                            text = "${grade.score} out of ${grade.totalPoints}",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Performance badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = scoreColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = when {
                                    grade.percentage >= 90 -> "🌟 Excellent!"
                                    grade.percentage >= 75 -> "✅ Passed"
                                    grade.percentage >= 60 -> "📝 Fair"
                                    else -> "📚 Needs Improvement"
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = scoreColor
                            )
                        }
                        
                        // Rank and comparison
                        if (grade.rank > 0 || grade.classAverage > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (grade.rank > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "#${grade.rank}",
                                            fontFamily = NeueMachina,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (grade.rank <= 3) Color(0xFFFFC107) else Color.Gray
                                        )
                                        Text(
                                            text = "Class Rank",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                if (grade.classAverage > 0) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        val diff = grade.percentage - grade.classAverage
                                        Text(
                                            text = "${if (diff >= 0) "+" else ""}${diff.toInt()}%",
                                            fontFamily = NeueMachina,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (diff >= 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                                        )
                                        Text(
                                            text = "vs Class Avg",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Exam Info
                StudentDetailRow(label = "Exam", value = grade.examName)
                StudentDetailRow(label = "Class", value = grade.className)
                StudentDetailRow(label = "Date", value = formatStudentDate(grade.gradedDate))
                if (grade.classAverage > 0) {
                    StudentDetailRow(
                        label = "Class Average",
                        value = "${grade.classAverage.toInt()}%"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                // Answer Statistics
                    Text(
                    text = "Answer Breakdown",
                        fontFamily = NeueMachina,
                    fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StudentStatBox(
                        label = "Correct",
                        value = grade.correctAnswers.toString(),
                        color = Color(0xFF4CAF50)
                    )
                    StudentStatBox(
                        label = "Incorrect",
                        value = grade.incorrectAnswers.toString(),
                        color = Color(0xFFE53935)
                    )
                    StudentStatBox(
                        label = "Unanswered",
                        value = grade.unanswered.toString(),
                        color = Color(0xFFFFC107)
                    )
                }
                
                // Answers Grid (if available)
                if (grade.answers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Your Answers",
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val answersToShow = grade.answers.take(20)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(answersToShow.size) { index ->
                            val answer = answersToShow[index]
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (answer.isEmpty() || answer == "-") 
                                            Color.LightGray.copy(alpha = 0.3f)
                                        else Color(0xFF1976D2).copy(alpha = 0.1f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                                    Text(
                                        text = if (answer.isEmpty() || answer == "-") "-" else answer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (answer.isEmpty() || answer == "-") 
                                            Color.Gray 
                                        else Color(0xFF1976D2)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (grade.answers.size > 20) {
                        Text(
                            text = "+${grade.answers.size - 20} more answers",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
                // Close Button
                Button(
                    onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StudentDetailRow(label: String, value: String) {
            Row(
                modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StudentStatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

// Helper functions
fun getScoreColor(percentage: Float): Color {
    return when {
        percentage >= 90 -> Color(0xFF4CAF50)  // Green - Excellent
        percentage >= 75 -> Color(0xFF2196F3)  // Blue - Passed
        percentage >= 60 -> Color(0xFFFFC107)  // Yellow - Fair
        else -> Color(0xFFE53935)              // Red - Needs Improvement
    }
}

fun formatStudentDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown date"
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@Composable
fun StudentProfileTab(
    viewModel: StudentDashboardViewModel,
    toastState: com.example.iskorko.ui.components.ToastState,
    onLogout: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    
    val studentName = viewModel.studentName.value
    val studentEmail = viewModel.studentEmail.value
    val profilePictureUrl = viewModel.profilePictureUrl.value
    val isUploadingPhoto = viewModel.isUploadingPhoto.value
    val stats = viewModel.studentStatistics.value
    val classes = viewModel.classes.value
    
    // Image picker launcher
    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.uploadProfilePicture(
                imageUri = it,
                context = context,
                onSuccess = { toastState.showSuccess("Profile picture updated!") },
                onError = { error -> toastState.showError(error) }
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
            modifier = Modifier
                .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar with edit button
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    if (isUploadingPhoto) {
                        Box(
                modifier = Modifier
                                .size(100.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF1976D2),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    } else if (profilePictureUrl != null && profilePictureUrl.startsWith("data:image")) {
                        // Decode Base64 image
                        val bitmap = remember(profilePictureUrl) {
                            try {
                                val base64Data = profilePictureUrl.substringAfter("base64,")
                                val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .clickable { showPhotoOptionsDialog = true },
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            // Fallback if decode fails
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(Color(0xFF1976D2), Color(0xFF2196F3))
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { showPhotoOptionsDialog = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = studentName.firstOrNull()?.uppercase() ?: "S",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(Color(0xFF1976D2), Color(0xFF2196F3))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { showPhotoOptionsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = studentName.firstOrNull()?.uppercase() ?: "S",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Camera edit button
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .background(Color(0xFF1976D2), CircleShape)
                            .clickable { showPhotoOptionsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Change Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = studentName,
                    fontFamily = NeueMachina,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF1976D2)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Student",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (studentEmail.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = studentEmail,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Academic Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Academic Overview",
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StudentProfileStatItem(
                        icon = Icons.Filled.School,
                        value = classes.size.toString(),
                        label = "Classes"
                    )
                    StudentProfileStatItem(
                        icon = Icons.Filled.Quiz,
                        value = stats.totalExamsTaken.toString(),
                        label = "Exams"
                    )
                    StudentProfileStatItem(
                        icon = Icons.Filled.Assessment,
                        value = "${stats.averageScore.toInt()}%",
                        label = "Average"
                    )
                    StudentProfileStatItem(
                        icon = Icons.Filled.Star,
                        value = stats.perfectScores.toString(),
                        label = "Perfect"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Account Settings Section
        Text(
            text = "Account",
            fontFamily = NeueMachina,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                StudentSettingsOption(
                    icon = Icons.Filled.Edit,
                    title = "Edit Profile",
                    subtitle = "Change your name",
                    onClick = { showEditProfileDialog = true }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                StudentSettingsOption(
                    icon = Icons.Filled.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = { showChangePasswordDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Settings Section
        Text(
            text = "App",
            fontFamily = NeueMachina,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                StudentSettingsOption(
                    icon = Icons.Filled.Info,
                    title = "About IskorKo",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                StudentSettingsOption(
                    icon = Icons.Filled.Help,
                    title = "Help & Support",
                    subtitle = "Get help with the app",
                    onClick = { showHelpDialog = true }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logout Button
        Button(
            onClick = { showLogoutConfirmDialog = true },
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
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Edit Profile Dialog
    if (showEditProfileDialog) {
        StudentEditProfileDialog(
            currentName = studentName,
            onDismiss = { showEditProfileDialog = false },
            onSave = { newName ->
                viewModel.updateProfile(
                    newName = newName,
                    onSuccess = { 
                        showEditProfileDialog = false
                        toastState.showSuccess("Profile updated successfully!")
                    },
                    onError = { error -> toastState.showError(error) }
                )
            }
        )
    }
    
    // Change Password Dialog
    if (showChangePasswordDialog) {
        StudentChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSave = { currentPassword, newPassword ->
                viewModel.changePassword(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                    onSuccess = { 
                        showChangePasswordDialog = false
                        toastState.showSuccess("Password changed successfully!")
                    },
                    onError = { error -> toastState.showError(error) }
                )
            }
        )
    }
    
    // Photo Options Dialog
    if (showPhotoOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoOptionsDialog = false },
            title = {
                Text(
                    text = "Profile Photo",
                    fontFamily = NeueMachina,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Choose from Gallery") },
                        leadingContent = {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFF1976D2))
                        },
                        modifier = Modifier.clickable {
                            showPhotoOptionsDialog = false
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                    if (profilePictureUrl != null) {
                        ListItem(
                            headlineContent = { Text("Remove Photo", color = Color.Red) },
                            leadingContent = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red)
                            },
                            modifier = Modifier.clickable {
                                showPhotoOptionsDialog = false
                                viewModel.removeProfilePicture(
                                    onSuccess = { toastState.showSuccess("Profile photo removed") },
                                    onError = { error -> toastState.showError(error) }
                                )
                            }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPhotoOptionsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // About Dialog
    if (showAboutDialog) {
        StudentAboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help & Support", fontFamily = NeueMachina) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("For Students", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Q: My login isn't working.", fontWeight = FontWeight.Bold)
                    Text("A: Double-check your email and password. If the issue persists, use the \"Forgot Password\" feature to reset your credentials.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Q: How do I join my professor's class?", fontWeight = FontWeight.Bold)
                    Text("A: Your professor will provide a unique \"Class Code.\" You must enter this code in the app to be added to the class roster.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Q: Can I take the exam directly on my phone?", fontWeight = FontWeight.Bold)
                    Text("A: No. IskorKo is a tool for checking paper-based exams. You will still answer on a physical paper sheet.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Q: What devices can I use?", fontWeight = FontWeight.Bold)
                    Text("A: The app is designed for Android devices. It requires Android 10.0 or higher and at least 2 GB of RAM.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
    
    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFF1976D2)
                )
            },
            title = {
                Text(
                    text = "Logout",
                    fontFamily = NeueMachina,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to logout?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800202)
                    )
                ) {
                    Text("Logout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentProfileStatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = NeueMachina,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun StudentSettingsOption(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF1976D2).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
        
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun StudentEditProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var isLoading by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Profile",
                        fontFamily = NeueMachina,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Filled.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            isLoading = true
                            onSave(name)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        enabled = name.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showCurrentPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Change Password",
                        fontFamily = NeueMachina,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { 
                        currentPassword = it
                        error = null
                    },
                    label = { Text("Current Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showCurrentPassword = !showCurrentPassword }) {
                            Icon(
                                if (showCurrentPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showCurrentPassword) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { 
                        newPassword = it
                        error = null
                    },
                    label = { Text("New Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.VpnKey, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { showNewPassword = !showNewPassword }) {
                            Icon(
                                if (showNewPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = "Toggle"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showNewPassword) 
                        androidx.compose.ui.text.input.VisualTransformation.None 
                    else 
                        androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { 
                        confirmPassword = it
                        error = null
                    },
                    label = { Text("Confirm New Password") },
                    leadingIcon = {
                        Icon(Icons.Filled.VpnKey, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                            Text("Passwords don't match", color = Color.Red)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1976D2),
                        focusedLabelColor = Color(0xFF1976D2)
                    )
                )
                
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = it, color = Color.Red, fontSize = 12.sp)
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            if (newPassword.length < 6) {
                                error = "Password must be at least 6 characters"
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                error = "Passwords don't match"
                                return@Button
                            }
                            isLoading = true
                            onSave(currentPassword, newPassword)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        enabled = currentPassword.isNotEmpty() && 
                                  newPassword.isNotEmpty() && 
                                  confirmPassword.isNotEmpty() &&
                                  !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Change")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAboutDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo
                Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.iskorko.R.drawable.logo_iskorko),
                    contentDescription = "IskorKo Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "IskorKo",
                    fontFamily = NeueMachina,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Version 1.0.0",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "A Mobile Class Record System with OMR Solution",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudentAboutFeatureItem(
                        icon = Icons.Filled.Assessment,
                        text = "View your exam grades instantly"
                    )
                    StudentAboutFeatureItem(
                        icon = Icons.Filled.TrendingUp,
                        text = "Track your academic progress"
                    )
                    StudentAboutFeatureItem(
                        icon = Icons.Filled.School,
                        text = "Join multiple classes"
                    )
                    StudentAboutFeatureItem(
                        icon = Icons.Filled.CloudSync,
                        text = "Real-time sync with teachers"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1976D2)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StudentAboutFeatureItem(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.DarkGray
        )
    }
}
