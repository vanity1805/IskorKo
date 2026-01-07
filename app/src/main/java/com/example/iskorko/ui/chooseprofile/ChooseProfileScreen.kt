package com.example.iskorko.ui.chooseprofile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.iskorko.ui.theme.NeueMachina
import com.example.iskorko.ui.components.ProfileButton

@Composable
fun ChooseProfileScreen(navController: NavHostController) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEDEDED))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose Your Profile",
                fontFamily = NeueMachina,
                fontSize = 50.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )

            Spacer(modifier = Modifier.height(40.dp))

            ProfileButton(
                label = "Student",
                onClick = {
                    Toast.makeText(context, "Student Selected", Toast.LENGTH_SHORT).show()
                    navController.navigate("login/Student")
                },
                enabled = true // explicitly set to true, or a state variable if you want
            )

            Spacer(modifier = Modifier.height(20.dp))

           ProfileButton(
                label = "Professor",
                onClick = {
                    Toast.makeText(context, "Professor Selected", Toast.LENGTH_SHORT).show()
                    navController.navigate("login/Professor")
                },
                enabled = true
            )
        }

        Text(
            text = "IskorKo",
            fontFamily = NeueMachina,
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}