package com.giantnovadevs.mysamoney.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.giantnovadevs.mysamoney.data.PreferencesManager
import com.giantnovadevs.mysamoney.ui.theme.AmberPalette
import com.giantnovadevs.mysamoney.ui.theme.AppColorPalette
import com.giantnovadevs.mysamoney.ui.theme.BluePalette
import com.giantnovadevs.mysamoney.ui.theme.GreenPalette
import com.giantnovadevs.mysamoney.ui.theme.PurplePalette
import com.giantnovadevs.mysamoney.ui.theme.RosePalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import com.giantnovadevs.mysamoney.ui.theme.DefaultFontFamily
import com.giantnovadevs.mysamoney.ui.theme.SerifFontFamily

// Helper class to map string names to the actual palette objects
data class ThemeOption(val name: String, val palette: AppColorPalette)
data class FontOption(val name: String, val fontFamily: FontFamily)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val preferencesManager = PreferencesManager(app)

    // All available themes
    val themeOptions = listOf(
        ThemeOption("Blue", BluePalette),
        ThemeOption("Green", GreenPalette),
        ThemeOption("Rose", RosePalette),
        ThemeOption("Purple", PurplePalette),
        ThemeOption("Amber", AmberPalette)
    )

    // A flow that emits the *currently selected* AppColorPalette
    val currentTheme = preferencesManager.theme
        .map { themeName ->
            // Find the palette object that matches the saved name
            themeOptions.find { it.name == themeName }?.palette ?: BluePalette
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BluePalette // Default to Blue
        )

    /**
     * Called by the UI when a new theme is selected.
     */
    fun saveTheme(themeOption: ThemeOption) {
        viewModelScope.launch {
            preferencesManager.saveTheme(themeOption.name)
        }
    }

    // All available fonts
    val fontOptions = listOf(
        FontOption("Default", DefaultFontFamily),
        FontOption("Serif", SerifFontFamily)
    )

    // A flow that emits the *currently selected* FontFamily
    val currentFont = preferencesManager.font
        .map { fontName ->
            // Find the FontFamily that matches the saved name
            fontOptions.find { it.name == fontName }?.fontFamily ?: DefaultFontFamily
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DefaultFontFamily // Default to DefaultFontFamily
        )

    /**
     * Called by the UI when a new font is selected.
     */
    fun saveFont(fontOption: FontOption) {
        viewModelScope.launch {
            preferencesManager.saveFont(fontOption.name)
        }
    }
}