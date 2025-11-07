package com.giantnovadevs.mysamoney.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowCompat

@Composable
fun MysaMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // --- Our New Dynamic Parameters ---
    // 1. Allow Dynamic Color (Android 12+) to be turned on/off
    dynamicColor: Boolean = true,
    // 2. The selected color palette (defaults to our Blue)
    palette: AppColorPalette = RosePalette,
    // 3. The selected font family
    fontFamily: FontFamily = DefaultFontFamily,
    // ---
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // User wants dynamic color AND their phone supports it
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> palette.darkColorScheme // Use the palette's dark theme
        else -> palette.lightColorScheme      // Use the palette's light theme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    // ✅ Here we pass our dynamic font family to our new function
    val typography = AppTypography(fontFamily = fontFamily)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography, // ✅ Pass our new dynamic typography
        content = content
    )
}