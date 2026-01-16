package com.example.iskorko.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.iskorko.R
import kotlinx.coroutines.delay

/**
 * Custom Toast with IskorKo logo
 * Use this instead of regular Android Toast for branded messages
 */
@Composable
fun IskorKoToast(
    message: String,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isError: Boolean = false,
    isSuccess: Boolean = false,
    duration: Long = 2500L
) {
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(duration)
            onDismiss()
        }
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                modifier = Modifier
                    .shadow(8.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = when {
                    isError -> Color(0xFFFFEBEE)
                    isSuccess -> Color(0xFFE8F5E9)
                    else -> Color.White
                }
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo
                    Image(
                        painter = painterResource(id = R.drawable.logo_iskorko),
                        contentDescription = "IskorKo",
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Message
                    Text(
                        text = message,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isError -> Color(0xFFB71C1C)
                            isSuccess -> Color(0xFF2E7D32)
                            else -> Color.DarkGray
                        }
                    )
                }
            }
        }
    }
}

/**
 * Toast state holder for easy management
 */
class ToastState {
    var isVisible by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var isError by mutableStateOf(false)
        private set
    var isSuccess by mutableStateOf(false)
        private set
    
    fun show(message: String, isError: Boolean = false, isSuccess: Boolean = false) {
        this.message = message
        this.isError = isError
        this.isSuccess = isSuccess
        this.isVisible = true
    }
    
    fun showSuccess(message: String) = show(message, isSuccess = true)
    fun showError(message: String) = show(message, isError = true)
    fun showInfo(message: String) = show(message)
    
    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun rememberToastState(): ToastState {
    return remember { ToastState() }
}

/**
 * Composable that hosts the toast - add this to your screen's Box or Scaffold
 */
@Composable
fun IskorKoToastHost(state: ToastState) {
    IskorKoToast(
        message = state.message,
        isVisible = state.isVisible,
        onDismiss = { state.dismiss() },
        isError = state.isError,
        isSuccess = state.isSuccess
    )
}