package com.example.iskorko.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import com.example.iskorko.R

val NeueMachina = FontFamily(
    Font(R.font.neuemachina_ultrabold, FontWeight.ExtraBold)
)

val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = NeueMachina,
        fontWeight = FontWeight.ExtraBold
    ),
    bodyLarge = TextStyle(
        fontFamily = NeueMachina
    )
)
