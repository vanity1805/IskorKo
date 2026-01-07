package com.example.iskorko.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.iskorko.ui.chooseprofile.ChooseProfileScreen
import com.example.iskorko.ui.login.LoginScreen
import com.example.iskorko.ui.forgotpassword.ForgotPasswordScreen
import com.example.iskorko.ui.signup.SignUpScreen
import com.example.iskorko.ui.dashboard.StudentDashboardScreen
import com.example.iskorko.ui.dashboard.ProfessorDashboardScreen
import com.example.iskorko.ui.splash.SplashScreen
import com.example.iskorko.ui.classes.CreateClassScreen 
import com.example.iskorko.ui.classes.ClassDetailScreen
import com.example.iskorko.ui.exams.CreateExamScreen
import com.example.iskorko.ui.exams.ExamDetailScreen
import com.example.iskorko.ui.student.StudentClassViewScreen 
import com.example.iskorko.ui.grades.GradeEntryScreen
import com.example.iskorko.ui.scan.ScanAnswerSheetScreen


@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(navController)
        }

        composable("chooseProfile") {
            ChooseProfileScreen(navController)
        }

        composable("login/{userType}") { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "User"
            LoginScreen(navController, userType)
        }

        composable("forgotPassword/{userType}") { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "User"
            ForgotPasswordScreen(navController, userType)
        }

        composable("signUp/{userType}") { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "User"
            SignUpScreen(navController, userType)
        }

        composable("studentDashboard") {
            StudentDashboardScreen(navController)
        }

        composable("professorDashboard") {
            ProfessorDashboardScreen(navController)
        }

        composable("createClass") {
            CreateClassScreen(navController)
        }

        composable("classDetail/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ClassDetailScreen(navController, classId)
        }

        composable("createExam/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            CreateExamScreen(navController, classId)
        }

        composable("examDetail/{examId}") { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            ExamDetailScreen(navController, examId)
        }

        composable("studentClassView/{classId}") { backStackEntry ->
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            StudentClassViewScreen(navController, classId)
        }

        composable("gradeEntry/{examId}") { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            GradeEntryScreen(navController, examId)
        }

        composable("scanAnswerSheet/{examId}/{classId}") { backStackEntry ->
            val examId = backStackEntry.arguments?.getString("examId") ?: ""
            val classId = backStackEntry.arguments?.getString("classId") ?: ""
            ScanAnswerSheetScreen(navController, examId, classId)
        }

    }
}