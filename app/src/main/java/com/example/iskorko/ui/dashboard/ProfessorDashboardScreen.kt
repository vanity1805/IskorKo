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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.example.iskorko.ui.components.IskorKoToastHost
import com.example.iskorko.ui.components.rememberToastState


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorDashboardScreen(
    navController: NavHostController,
    viewModel: ProfessorDashboardViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val toastState = rememberToastState()
    var showNotifications by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Hello, ${viewModel.professorName.value.split(" ").firstOrNull() ?: "Professor"}",
                            fontFamily = NeueMachina,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Welcome back!",
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
                    onClick = { navController.navigate("createClass") },
                    containerColor = Color(0xFF800202),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Class")
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
                0 -> ClassesTab(
                    classes = viewModel.classes.value,
                    isLoading = viewModel.isLoading.value,
                    navController = navController
                )
                1 -> GradesTab(viewModel = viewModel)
                2 -> ProfileTab(
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
    
    // Notifications Bottom Sheet
    if (showNotifications) {
        NotificationsBottomSheet(
            notifications = viewModel.notifications.value,
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
fun NotificationsBottomSheet(
    notifications: List<NotificationItem>,
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
                        NotificationCard(
                            notification = notification,
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
fun NotificationCard(
    notification: NotificationItem,
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
            // Icon based on notification type
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
fun ClassesTab(
    classes: List<ClassItem>,
    isLoading: Boolean,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard(
                number = classes.size.toString(),
                label = "Classes",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                number = classes.sumOf { it.studentCount }.toString(),
                label = "Students",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            StatCard(
                number = classes.sumOf { it.examCount }.toString(),
                label = "Exams",
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // My Classes Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Classes",
                fontFamily = NeueMachina,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Classes List
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
                        text = "Tap + to create your first class",
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
                    ClassCard(classItem = classItem, onClick = {
                        navController.navigate("classDetail/${classItem.id}")
                    })
                }
            }
        }
    }
}

@Composable
fun StatCard(number: String, label: String, modifier: Modifier = Modifier) {
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
                color = Color(0xFF800202)
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
fun ClassCard(classItem: ClassItem, onClick: () -> Unit) {
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
            // Class Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFFFFEBEE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.School,
                    contentDescription = null,
                    tint = Color(0xFF800202)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Class Info
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
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${classItem.studentCount} students",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = " · ",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${classItem.examCount} exams",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            // Class Code Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE3F2FD)
            ) {
                Text(
                    text = classItem.classCode,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesTab(viewModel: ProfessorDashboardViewModel) {
    var showFilterMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var selectedGrade by remember { mutableStateOf<GradeItem?>(null) }
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
        GradeStatisticsSection(viewModel.gradeStatistics.value)
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
        // View Mode Tabs
        ViewModeTabs(
            selectedMode = viewModel.selectedViewMode.value,
            onModeSelected = { viewModel.setViewMode(it) }
        )
        
        // Search and Filter Bar
        SearchAndFilterBar(
            searchQuery = viewModel.searchQuery.value,
            onSearchChange = { viewModel.setSearchQuery(it) },
            onFilterClick = { showFilterMenu = true },
            onSortClick = { showSortMenu = true },
            hasFilters = viewModel.selectedClassFilter.value != null || 
                        viewModel.selectedExamFilter.value != null
        )
        
        // Content based on view mode
        when (viewModel.selectedViewMode.value) {
            GradeViewMode.ALL_GRADES -> AllGradesView(
                grades = viewModel.getFilteredAndSortedGrades(),
                isLoading = viewModel.isLoadingGrades.value,
                onGradeClick = { selectedGrade = it }
            )
            GradeViewMode.BY_CLASS -> ByClassView(
                classSummaries = viewModel.classSummaries.value,
                isLoading = viewModel.isLoadingGrades.value,
                onGradeClick = { selectedGrade = it }
            )
            GradeViewMode.BY_EXAM -> ByExamView(
                examSummaries = viewModel.examSummaries.value,
                isLoading = viewModel.isLoadingGrades.value,
                onGradeClick = { selectedGrade = it }
            )
            GradeViewMode.BY_STUDENT -> ByStudentView(
                grades = viewModel.getFilteredAndSortedGrades(),
                isLoading = viewModel.isLoadingGrades.value,
                onGradeClick = { selectedGrade = it }
            )
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterMenu) {
        FilterBottomSheet(
            classes = viewModel.classes.value,
            selectedClassId = viewModel.selectedClassFilter.value,
            selectedExamId = viewModel.selectedExamFilter.value,
            onClassSelected = { viewModel.setClassFilter(it) },
            onExamSelected = { viewModel.setExamFilter(it) },
            onDismiss = { showFilterMenu = false },
            onClearFilters = {
                viewModel.setClassFilter(null)
                viewModel.setExamFilter(null)
            }
        )
    }
    
    // Sort Bottom Sheet
    if (showSortMenu) {
        SortBottomSheet(
            selectedOption = viewModel.selectedSortOption.value,
            onOptionSelected = { 
                viewModel.setSortOption(it)
                showSortMenu = false
            },
            onDismiss = { showSortMenu = false }
        )
    }
    
    // Grade Detail Dialog
    selectedGrade?.let { grade ->
        GradeDetailDialog(
            grade = grade,
            onDismiss = { selectedGrade = null }
        )
    }
}

@Composable
fun GradeStatisticsSection(stats: GradeStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                fontFamily = NeueMachina,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Total Graded",
                    value = stats.totalExamsGraded.toString(),
                    icon = Icons.Filled.Assessment
                )
                StatItem(
                    label = "Avg Score",
                    value = "${stats.averageScore.toInt()}%",
                    icon = Icons.Filled.TrendingUp
                )
                StatItem(
                    label = "Students",
                    value = stats.totalStudents.toString(),
                    icon = Icons.Filled.People
                )
                StatItem(
                    label = "Pass Rate",
                    value = "${stats.passingRate.toInt()}%",
                    icon = Icons.Filled.CheckCircle
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF800202),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = NeueMachina,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF800202)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ViewModeTabs(selectedMode: GradeViewMode, onModeSelected: (GradeViewMode) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedMode.ordinal,
        containerColor = Color.White,
        contentColor = Color(0xFF800202),
        edgePadding = 16.dp
    ) {
        Tab(
            selected = selectedMode == GradeViewMode.ALL_GRADES,
            onClick = { onModeSelected(GradeViewMode.ALL_GRADES) },
            text = { Text("All Grades") }
        )
        Tab(
            selected = selectedMode == GradeViewMode.BY_CLASS,
            onClick = { onModeSelected(GradeViewMode.BY_CLASS) },
            text = { Text("By Class") }
        )
        Tab(
            selected = selectedMode == GradeViewMode.BY_EXAM,
            onClick = { onModeSelected(GradeViewMode.BY_EXAM) },
            text = { Text("By Exam") }
        )
        Tab(
            selected = selectedMode == GradeViewMode.BY_STUDENT,
            onClick = { onModeSelected(GradeViewMode.BY_STUDENT) },
            text = { Text("By Student") }
        )
    }
}

@Composable
fun SearchAndFilterBar(
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
            placeholder = { Text("Search students, classes, exams...") },
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
                focusedBorderColor = Color(0xFF800202),
                focusedLabelColor = Color(0xFF800202)
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Badge(
            modifier = Modifier,
            containerColor = if (hasFilters) Color(0xFF800202) else Color.Transparent
        ) {
            IconButton(onClick = onFilterClick) {
                Icon(
                    Icons.Filled.FilterList,
                    contentDescription = "Filter",
                    tint = if (hasFilters) Color(0xFF800202) else Color.Gray
                )
            }
        }
        
        IconButton(onClick = onSortClick) {
            Icon(Icons.Filled.Sort, contentDescription = "Sort")
        }
    }
}

@Composable
fun AllGradesView(
    grades: List<GradeItem>,
    isLoading: Boolean,
    onGradeClick: (GradeItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (grades.isEmpty()) {
        EmptyGradesView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(grades) { grade ->
                GradeCard(grade = grade, onClick = { onGradeClick(grade) })
            }
        }
    }
}

@Composable
fun GradeCard(grade: GradeItem, onClick: () -> Unit) {
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
                    .size(56.dp)
                    .background(
                        when {
                            grade.percentage >= 90 -> Color(0xFF4CAF50)
                            grade.percentage >= 75 -> Color(0xFF2196F3)
                            grade.percentage >= 60 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        }.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${grade.percentage.toInt()}%",
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            grade.percentage >= 90 -> Color(0xFF4CAF50)
                            grade.percentage >= 75 -> Color(0xFF2196F3)
                            grade.percentage >= 60 -> Color(0xFFFFC107)
                            else -> Color(0xFFE53935)
                        }
                    )
                    Text(
                        text = "${grade.score}/${grade.totalPoints}",
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Grade Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.studentName,
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = grade.examName,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = grade.className,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = formatDate(grade.gradedDate),
                    fontSize = 11.sp,
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
fun ByClassView(
    classSummaries: List<ClassGradesSummary>,
    isLoading: Boolean,
    onGradeClick: (GradeItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (classSummaries.isEmpty()) {
        EmptyGradesView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(classSummaries) { summary ->
                ClassGradesCard(summary = summary, onGradeClick = onGradeClick)
            }
        }
    }
}

@Composable
fun ClassGradesCard(summary: ClassGradesSummary, onGradeClick: (GradeItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.className,
                        fontFamily = NeueMachina,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        Text(
                            text = "${summary.studentCount} students",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(text = " · ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "${summary.examCount} exams",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(text = " · ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = "Avg: ${summary.averageScore.toInt()}%",
                            fontSize = 12.sp,
                            color = Color(0xFF800202),
                            fontWeight = FontWeight.Bold
                        )
                    }
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
                
                val gradesToShow = if (showAll) summary.grades else summary.grades.take(5)
                
                gradesToShow.forEach { grade ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGradeClick(grade) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = grade.studentName,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = grade.examName,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "${grade.percentage.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF800202)
                        )
                    }
                }
                
                if (summary.grades.size > 5) {
                    Text(
                        text = if (showAll) "Show less" else "+${summary.grades.size - 5} more",
                        fontSize = 12.sp,
                        color = Color(0xFF800202),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { showAll = !showAll }
                    )
                }
            }
        }
    }
}

@Composable
fun ByExamView(
    examSummaries: List<ExamGradesSummary>,
    isLoading: Boolean,
    onGradeClick: (GradeItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (examSummaries.isEmpty()) {
        EmptyGradesView()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(examSummaries) { summary ->
                ExamGradesCard(summary = summary, onGradeClick = onGradeClick)
            }
        }
    }
}

@Composable
fun ExamGradesCard(summary: ExamGradesSummary, onGradeClick: (GradeItem) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showAll by remember { mutableStateOf(false) }
    
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.examName,
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.className,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = "${summary.studentsTaken} taken",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(text = " · ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "Avg: ${summary.averageScore.toInt()}%",
                            fontSize = 11.sp,
                            color = Color(0xFF800202),
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Only show High/Low if there's variation in scores
                        if (summary.highestScore != summary.lowestScore) {
                        Text(text = " · ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "High: ${summary.highestScore}",
                            fontSize = 11.sp,
                            color = Color(0xFF4CAF50)
                        )
                        Text(text = " · ", fontSize = 11.sp, color = Color.Gray)
                        Text(
                            text = "Low: ${summary.lowestScore}",
                            fontSize = 11.sp,
                            color = Color(0xFFE53935)
                        )
                        } else if (summary.studentsTaken > 0) {
                            Text(text = " · ", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "All scored: ${summary.highestScore}",
                                fontSize = 11.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Grade Distribution Chart (simple bar representation)
                GradeDistributionChart(summary.grades)
                
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                val sortedGrades = summary.grades.sortedByDescending { it.percentage }
                val gradesToShow = if (showAll) sortedGrades else sortedGrades.take(10)
                
                gradesToShow.forEach { grade ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGradeClick(grade) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = grade.studentName,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${grade.score}/${grade.totalPoints}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${grade.percentage.toInt()}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF800202)
                        )
                    }
                }
                
                if (summary.grades.size > 10) {
                    Text(
                        text = if (showAll) "Show less" else "+${summary.grades.size - 10} more",
                        fontSize = 12.sp,
                        color = Color(0xFF800202),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clickable { showAll = !showAll }
                    )
                }
            }
        }
    }
}

@Composable
fun GradeDistributionChart(grades: List<GradeItem>) {
    val ranges = mapOf(
        "90-100%" to grades.count { it.percentage >= 90 },
        "75-89%" to grades.count { it.percentage in 75.0..89.9 },
        "60-74%" to grades.count { it.percentage in 60.0..74.9 },
        "Below 60%" to grades.count { it.percentage < 60 }
    )
    
    val maxCount = ranges.values.maxOrNull() ?: 1
    
    Column {
        Text(
            text = "Grade Distribution",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        ranges.forEach { (range, count) ->
            Row(
                modifier = Modifier.padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = range,
                    fontSize = 11.sp,
                    modifier = Modifier.width(80.dp)
                )
                
                LinearProgressIndicator(
                    progress = { if (maxCount > 0) count.toFloat() / maxCount else 0f },
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = when (range) {
                        "90-100%" -> Color(0xFF4CAF50)
                        "75-89%" -> Color(0xFF2196F3)
                        "60-74%" -> Color(0xFFFFC107)
                        else -> Color(0xFFE53935)
                    },
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
            }
        }
    }
}

@Composable
fun ByStudentView(
    grades: List<GradeItem>,
    isLoading: Boolean,
    onGradeClick: (GradeItem) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF800202))
        }
    } else if (grades.isEmpty()) {
        EmptyGradesView()
    } else {
        val groupedByStudent = grades.groupBy { it.studentId }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(groupedByStudent.toList()) { (studentId, studentGrades) ->
                StudentGradesCard(
                    studentName = studentGrades.first().studentName,
                    grades = studentGrades,
                    onGradeClick = onGradeClick
                )
            }
        }
    }
}

@Composable
fun StudentGradesCard(
    studentName: String,
    grades: List<GradeItem>,
    onGradeClick: (GradeItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val avgScore = grades.map { it.percentage }.average().toFloat()
    
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
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF800202), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = studentName.firstOrNull()?.toString() ?: "?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studentName,
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${grades.size} exams · Avg: ${avgScore.toInt()}%",
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
                
                grades.sortedByDescending { it.gradedDate }.forEach { grade ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onGradeClick(grade) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = grade.examName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${grade.className} · ${formatDate(grade.gradedDate)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Text(
                            text = "${grade.percentage.toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                grade.percentage >= 90 -> Color(0xFF4CAF50)
                                grade.percentage >= 75 -> Color(0xFF2196F3)
                                grade.percentage >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFE53935)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGradesView() {
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
                text = "No grades yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                text = "Grades will appear here once exams are completed",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

// Helper function to format timestamp
fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Unknown date"
    val sdf = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    classes: List<ClassItem>,
    selectedClassId: String?,
    selectedExamId: String?,
    onClassSelected: (String?) -> Unit,
    onExamSelected: (String?) -> Unit,
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
                    text = "Filter Grades",
                    fontFamily = NeueMachina,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onClearFilters) {
                    Text("Clear All", color = Color(0xFF800202))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filter by Class
            Text(
                text = "Filter by Class",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // All Classes option
                FilterChip(
                    selected = selectedClassId == null,
                    onClick = { onClassSelected(null) },
                    label = { Text("All Classes") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Individual classes
                classes.forEach { classItem ->
                    FilterChip(
                        selected = selectedClassId == classItem.id,
                        onClick = { onClassSelected(classItem.id) },
                        label = { 
                            Text("${classItem.className} - ${classItem.section}") 
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Apply button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF800202)
                )
            ) {
                Text("Apply Filters")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    selectedOption: GradeSortOption,
    onOptionSelected: (GradeSortOption) -> Unit,
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
                GradeSortOption.DATE_DESC to "Newest First",
                GradeSortOption.DATE_ASC to "Oldest First",
                GradeSortOption.SCORE_DESC to "Highest Score",
                GradeSortOption.SCORE_ASC to "Lowest Score",
                GradeSortOption.STUDENT_NAME to "Student Name (A-Z)",
                GradeSortOption.CLASS_NAME to "Class Name",
                GradeSortOption.EXAM_NAME to "Exam Name"
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
                            selectedColor = Color(0xFF800202)
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

@Composable
fun GradeDetailDialog(
    grade: GradeItem,
    onDismiss: () -> Unit
) {
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
                        .background(
                            when {
                                grade.percentage >= 90 -> Color(0xFF4CAF50)
                                grade.percentage >= 75 -> Color(0xFF2196F3)
                                grade.percentage >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFE53935)
                            }.copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${grade.percentage.toInt()}%",
                            fontFamily = NeueMachina,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                grade.percentage >= 90 -> Color(0xFF4CAF50)
                                grade.percentage >= 75 -> Color(0xFF2196F3)
                                grade.percentage >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFE53935)
                            }
                        )
                        Text(
                            text = "${grade.score} out of ${grade.totalPoints}",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = when {
                                grade.percentage >= 90 -> "Excellent"
                                grade.percentage >= 75 -> "Passed"
                                grade.percentage >= 60 -> "Fair"
                                else -> "Needs Improvement"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                grade.percentage >= 90 -> Color(0xFF4CAF50)
                                grade.percentage >= 75 -> Color(0xFF2196F3)
                                grade.percentage >= 60 -> Color(0xFFFFC107)
                                else -> Color(0xFFE53935)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Student Info
                DetailRow(label = "Student", value = grade.studentName)
                DetailRow(label = "Class", value = grade.className)
                DetailRow(label = "Exam", value = grade.examName)
                DetailRow(label = "Date", value = formatDate(grade.gradedDate))
                
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
                    StatBox(
                        label = "Correct",
                        value = grade.correctAnswers.toString(),
                        color = Color(0xFF4CAF50)
                    )
                    StatBox(
                        label = "Incorrect",
                        value = grade.incorrectAnswers.toString(),
                        color = Color(0xFFE53935)
                    )
                    StatBox(
                        label = "Unanswered",
                        value = grade.unanswered.toString(),
                        color = Color(0xFFFFC107)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Answers Grid (if available)
                if (grade.answers.isNotEmpty()) {
                    Text(
                        text = "Answers",
                        fontFamily = NeueMachina,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Display answers in a grid (first 20 for preview)
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
                                        if (answer.isEmpty()) Color.LightGray.copy(alpha = 0.3f)
                                        else Color(0xFF800202).copy(alpha = 0.1f),
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
                                        text = if (answer.isEmpty()) "-" else answer,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (answer.isEmpty()) Color.Gray else Color(0xFF800202)
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
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: Export functionality */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF800202)
                        )
                    ) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export")
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF800202)
                        )
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
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
fun StatBox(label: String, value: String, color: Color) {
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

@Composable
fun ProfileTab(
    viewModel: ProfessorDashboardViewModel, 
    toastState: com.example.iskorko.ui.components.ToastState,
    onLogout: () -> Unit
) {
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showPhotoOptionsDialog by remember { mutableStateOf(false) }
    
    val professorName = viewModel.professorName.value
    val professorEmail = viewModel.professorEmail.value
    val profilePictureUrl = viewModel.profilePictureUrl.value
    val isUploadingPhoto = viewModel.isUploadingPhoto.value
    val stats = viewModel.gradeStatistics.value
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
                                color = Color(0xFF800202),
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
                                            colors = listOf(Color(0xFF800202), Color(0xFFB71C1C))
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { showPhotoOptionsDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                                    text = professorName.firstOrNull()?.uppercase() ?: "P",
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
                                        colors = listOf(Color(0xFF800202), Color(0xFFB71C1C))
                                    ),
                                    shape = CircleShape
                                )
                                .clickable { showPhotoOptionsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = professorName.firstOrNull()?.uppercase() ?: "P",
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
                            .background(Color(0xFF800202), CircleShape)
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
                        text = professorName,
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
                        tint = Color(0xFF800202)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Professor",
                        fontSize = 14.sp,
                        color = Color(0xFF800202),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (professorEmail.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = professorEmail,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Teaching Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Teaching Overview",
                    fontFamily = NeueMachina,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(
                        icon = Icons.Filled.School,
                        value = classes.size.toString(),
                        label = "Classes"
                    )
                    ProfileStatItem(
                        icon = Icons.Filled.People,
                        value = stats.totalStudents.toString(),
                        label = "Students"
                    )
                    ProfileStatItem(
                        icon = Icons.Filled.Assessment,
                        value = stats.totalExamsGraded.toString(),
                        label = "Graded"
                    )
                    ProfileStatItem(
                        icon = Icons.Filled.TrendingUp,
                        value = "${stats.averageScore.toInt()}%",
                        label = "Avg Score"
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
        SettingsOption(
            icon = Icons.Filled.Edit,
            title = "Edit Profile",
                    subtitle = "Change your name",
                    onClick = { showEditProfileDialog = true }
        )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
        
        SettingsOption(
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
        SettingsOption(
            icon = Icons.Filled.Info,
                    title = "About IskorKo",
                    subtitle = "Version 1.0.0",
                    onClick = { showAboutDialog = true }
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsOption(
                    icon = Icons.Filled.Help,
                    title = "Help & Support",
                    subtitle = "Get help with the app",
                    onClick = { /* TODO: Open help */ }
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
        EditProfileDialog(
            currentName = professorName,
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
        ChangePasswordDialog(
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
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color(0xFF800202))
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
        AboutDialog(onDismiss = { showAboutDialog = false })
    }
    
    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFF800202)
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
fun ProfileStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF800202),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontFamily = NeueMachina,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF800202)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SettingsOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                .background(Color(0xFF800202).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF800202),
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
fun EditProfileDialog(
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
                        focusedBorderColor = Color(0xFF800202),
                        focusedLabelColor = Color(0xFF800202)
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
                            containerColor = Color(0xFF800202)
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
fun ChangePasswordDialog(
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
                
                // Current Password
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
                                contentDescription = "Toggle password visibility"
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
                        focusedBorderColor = Color(0xFF800202),
                        focusedLabelColor = Color(0xFF800202)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // New Password
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
                                contentDescription = "Toggle password visibility"
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
                        focusedBorderColor = Color(0xFF800202),
                        focusedLabelColor = Color(0xFF800202)
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Confirm Password
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
                        focusedBorderColor = Color(0xFF800202),
                        focusedLabelColor = Color(0xFF800202)
                    )
                )
                
                // Error message
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
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
                            containerColor = Color(0xFF800202)
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
fun AboutDialog(onDismiss: () -> Unit) {
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
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Features
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AboutFeatureItem(
                        icon = Icons.Filled.CameraAlt,
                        text = "Scan answer sheets instantly"
                    )
                    AboutFeatureItem(
                        icon = Icons.Filled.Assessment,
                        text = "Automatic grading & analytics"
                    )
                    AboutFeatureItem(
                        icon = Icons.Filled.School,
                        text = "Manage classes & students"
                    )
                    AboutFeatureItem(
                        icon = Icons.Filled.CloudSync,
                        text = "Real-time sync across devices"
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF800202)
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AboutFeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF800202),
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