package com.example.iskorko.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.R
import com.example.iskorko.ui.theme.NeueMachina
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplashScreen(
    navController: NavHostController,
    viewModel: SplashViewModel = viewModel()
) {
    // Slide down animation
    var startAnimation by remember { mutableStateOf(false) }
    
    val slideOffset by animateDpAsState(
        targetValue = if (startAnimation) 0.dp else (-150).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slideOffset"
    )
    
    // Scale animation for bounce effect
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Jiggle/shake animation (continuous)
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    
    val jiggleRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleRotation"
    )
    
    // Subtle vertical bounce during loading
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )
    
    LaunchedEffect(Unit) {
        // Start the slide-down animation immediately
        startAnimation = true
        
        // Wait for animation + branding delay
        delay(1500)

        viewModel.checkSession { destination ->
            navController.navigate(destination) {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = slideOffset)
        ) {
            // Logo with jiggle animation
            Image(
                painter = painterResource(id = R.drawable.logo_iskorko),
                contentDescription = "IskorKo Logo",
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = jiggleRotation
                        translationY = bounceOffset
                    }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // App Name
            Text(
                text = "IskorKo",
                fontFamily = NeueMachina,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF800202),
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Tagline
            Text(
                text = "Iskorko, Iskormo!",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.graphicsLayer {
                    alpha = if (startAnimation) 1f else 0f
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Loading indicator
            CircularProgressIndicator(
                color = Color(0xFF800202),
                strokeWidth = 3.dp,
                modifier = Modifier
                    .size(36.dp)
                    .graphicsLayer {
                        alpha = if (startAnimation) 1f else 0f
                    }
            )
        }
    }
}

// Easing function for smooth bounce
private val EaseInOutQuad: Easing = Easing { fraction ->
    if (fraction < 0.5f) {
        2 * fraction * fraction
    } else {
        1 - (-2 * fraction + 2).let { it * it } / 2
    }
}
