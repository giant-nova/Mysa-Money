package com.example.spendwise.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * A data class to hold the light and dark color schemes for a theme.
 * This is what we will pass to our main Theme composable.
 */
data class AppColorPalette(
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
)

// --- Shared Semantic Colors ---
// These are used by all themes for consistency
private val md_theme_light_error = Color(0xFFBA1A1A)
private val md_theme_light_onError = Color(0xFFFFFFFF)
private val md_theme_light_errorContainer = Color(0xFFFFDAD6)
private val md_theme_light_onErrorContainer = Color(0xFF410002)

private val md_theme_dark_error = Color(0xFFFFB4AB)
private val md_theme_dark_onError = Color(0xFF690005)
private val md_theme_dark_errorContainer = Color(0xFF93000A)
private val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)


// --- 1. Blue Palette (Your Original) ---
// Seed: Color(0xFF0061A4)
private val BlueLightColors = lightColorScheme(
    primary = Color(0xFF0061A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF535F70),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3F7),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251431),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color(0xFFF5F5F5),     // Change to light gray
    onBackground = Color(0xFF1A1C1E),   // Black text on gray
    surface = Color(0xFFFFFFFF),        // Change to pure white
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF73777F)
)

private val BlueDarkColors = darkColorScheme(
    primary = Color(0xFF9ECAFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE4),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF52405F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = Color(0xFF121212),     // Change to pure black
    onBackground = Color(0xFFE2E2E6),   // White text on black
    surface = Color(0xFF1E1E1E),        // Change to a dark gray
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199)
)

// --- 2. Green Palette ---
// Seed: Color(0xFF006D39)
private val GreenLightColors = lightColorScheme(
    primary = Color(0xFF006D39),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7AF8A3),
    onPrimaryContainer = Color(0xFF00210E),
    secondary = Color(0xFF506352),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E8D3),
    onSecondaryContainer = Color(0xFF0E1F12),
    tertiary = Color(0xFF3B656F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color(0xFFFCFDF7),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFFCFDF7),
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = Color(0xFFDEE5DA),
    onSurfaceVariant = Color(0xFF424941),
    outline = Color(0xFF727971)
)

private val GreenDarkColors = darkColorScheme(
    primary = Color(0xFF5DDB89),
    onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF00532B),
    onPrimaryContainer = Color(0xFF7AF8A3),
    secondary = Color(0xFFB7CCB7),
    onSecondary = Color(0xFF233426),
    secondaryContainer = Color(0xFF394B3C),
    onSecondaryContainer = Color(0xFFD3E8D3),
    tertiary = Color(0xFFA3CEDC),
    onTertiary = Color(0xFF043640),
    tertiaryContainer = Color(0xFF224D57),
    onTertiaryContainer = Color(0xFFBFEAF7),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = Color(0xFF1A1C1A),
    onBackground = Color(0xFFE2E3DE),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE2E3DE),
    surfaceVariant = Color(0xFF424941),
    onSurfaceVariant = Color(0xFFC2C9BF),
    outline = Color(0xFF8C938A)
)

// --- 3. Purple (M3 Baseline) Palette ---
// Seed: Color(0xFF6750A4)
private val PurpleLightColors = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

private val PurpleDarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

// --- 4. Rose (Red) Palette ---
// Seed: Color(0xFFB3261E)
private val RoseLightColors = lightColorScheme(
    primary = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF775652),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD6),
    onSecondaryContainer = Color(0xFF2C1512),
    tertiary = Color(0xFF755A2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEAC),
    onTertiaryContainer = Color(0xFF281900),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF201A19),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF201A19),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    outline = Color(0xFF857371)
)

private val RoseDarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442926),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFE5C18D),
    onTertiary = Color(0xFF412D05),
    tertiaryContainer = Color(0xFF5B431A),
    onTertiaryContainer = Color(0xFFFFDEAC),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = Color(0xFF201A19),
    onBackground = Color(0xFFEDE0DE),
    surface = Color(0xFF201A19),
    onSurface = Color(0xFFEDE0DE),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    outline = Color(0xFFA08C8A)
)

// --- 5. Amber (Orange) Palette ---
// Seed: Color(0xFF994700)
private val AmberLightColors = lightColorScheme(
    primary = Color(0xFF994700),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF321300),
    secondary = Color(0xFF775747),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF656031),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFECE5AA),
    onTertiaryContainer = Color(0xFF201C00),
    error = md_theme_light_error,
    onError = md_theme_light_onError,
    errorContainer = md_theme_light_errorContainer,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = Color(0xFFFCFCFC),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED4),
    onSurfaceVariant = Color(0xFF53433C),
    outline = Color(0xFF85736B)
)

private val AmberDarkColors = darkColorScheme(
    primary = Color(0xFFFFB786),
    onPrimary = Color(0xFF522300),
    primaryContainer = Color(0xFF753400),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE7BDB0),
    onSecondary = Color(0xFF442A1E),
    secondaryContainer = Color(0xFF5D4032),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFD0C990),
    onTertiary = Color(0xFF363107),
    tertiaryContainer = Color(0xFF4D481C),
    onTertiaryContainer = Color(0xFFECE5AA),
    error = md_theme_dark_error,
    onError = md_theme_dark_onError,
    errorContainer = md_theme_dark_errorContainer,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = Color(0xFF201A18),
    onBackground = Color(0xFFEDE0DB),
    surface = Color(0xFF201A18),
    onSurface = Color(0xFFEDE0DB),
    surfaceVariant = Color(0xFF53433C),
    onSurfaceVariant = Color(0xFFD8C2B9),
    outline = Color(0xFFA08D85)
)

val BluePalette = AppColorPalette(
    lightColorScheme = BlueLightColors,
    darkColorScheme = BlueDarkColors
)

val GreenPalette = AppColorPalette(
    lightColorScheme = GreenLightColors,
    darkColorScheme = GreenDarkColors
)

val PurplePalette = AppColorPalette(
    lightColorScheme = PurpleLightColors,
    darkColorScheme = PurpleDarkColors
)

val RosePalette = AppColorPalette(
    lightColorScheme = RoseLightColors,
    darkColorScheme = RoseDarkColors
)

val AmberPalette = AppColorPalette(
    lightColorScheme = AmberLightColors,
    darkColorScheme = AmberDarkColors
)

// --- Public List of All Palettes ---

val AppPalettes = listOf(BluePalette, GreenPalette, PurplePalette, RosePalette, AmberPalette)