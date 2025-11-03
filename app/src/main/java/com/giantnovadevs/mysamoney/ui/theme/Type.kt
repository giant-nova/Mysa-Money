package com.giantnovadevs.mysamoney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Define your available font families (Add more here in the future)
// You would add Google Font definitions here.
val DefaultFontFamily = FontFamily.Default
val SerifFontFamily = FontFamily.Serif

/**
 * Instead of a static object, this is now a function.
 * It takes a FontFamily and builds a Typography object from it.
 */
fun AppTypography(fontFamily: FontFamily): Typography {
    return Typography(
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp
        ),
        // ... define all your other styles (bodyMedium, titleMedium, etc.)
    )
}