package com.example.spendwise.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(context: Context) {

    private val dataStore = context.dataStore

    // This is the key we'll use to save our theme
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val FONT_KEY = stringPreferencesKey("font_preference")
    }

    /**
     * Saves the selected theme name (e.g., "Blue", "Green")
     */
    suspend fun saveTheme(themeName: String) {
        dataStore.edit { preferences ->
            preferences[THEME_KEY] = themeName
        }
    }

    /**
     * A flow that emits the currently saved theme name.
     * It defaults to "Blue" if no theme is set.
     */
    val theme: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[THEME_KEY] ?: "Rose" // Default to "Rose"
        }

    /**
     * Saves the selected font name (e.g., "Default", "Serif")
     */
    suspend fun saveFont(fontName: String) {
        dataStore.edit { preferences ->
            preferences[FONT_KEY] = fontName
        }
    }

    /**
     * A flow that emits the currently saved font name.
     * It defaults to "Default" if no font is set.
     */
    val font: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[FONT_KEY] ?: "Default"
        }
}