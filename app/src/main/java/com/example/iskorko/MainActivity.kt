package com.example.iskorko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.example.iskorko.navigation.NavGraph
import org.opencv.android.OpenCVLoader
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install and immediately dismiss the system splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }
        
        super.onCreate(savedInstanceState)

        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Failed to load OpenCV")
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully")
        }

        setContent {
            val navController = rememberNavController()
            NavGraph(navController = navController)
        }
    }
}