package com.giantnovadevs.mysamoney.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey

// Create the DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(context: Context) {

    private val dataStore = context.dataStore

    // This is the key we'll use to save our theme
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_preference")
        val FONT_KEY = stringPreferencesKey("font_preference")
        val PRO_KEY = booleanPreferencesKey("pro_status")
        val FREE_SCANS_KEY = intPreferencesKey("free_scans_remaining")
    }

    /**
     * A flow that emits how many free scans are left.
     * It defaults to 5.
     */
    val freeScansRemaining: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[FREE_SCANS_KEY] ?: 5 // Default to 5 free scans
        }

    /**
     * Decrements the free scan count by one.
     */
    suspend fun decrementFreeScans() {
        dataStore.edit { preferences ->
            val currentScans = preferences[FREE_SCANS_KEY] ?: 5
            if (currentScans > 0) {
                preferences[FREE_SCANS_KEY] = currentScans - 1
            }
        }
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

    suspend fun saveProStatus(isPro: Boolean) {
        dataStore.edit { preferences ->
            preferences[PRO_KEY] = isPro
        }
    }

    /**
     * A flow that emits the currently saved Pro status.
     * Defaults to false.
     */
    val isProUser: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[PRO_KEY] ?: false
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