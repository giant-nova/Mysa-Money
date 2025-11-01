package com.example.spendwise.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.auth.GoogleDriveManager
import com.example.spendwise.data.AppDatabase
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Simple class to represent the UI state of the backup/restore buttons
enum class BackupRestoreState {
    IDLE, // Ready
    LOADING, // In progress
    SUCCESS, // Completed successfully
    ERROR // Failed
}

class BackupViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(BackupRestoreState.IDLE)
    val state = _state.asStateFlow()

    private var driveManager: GoogleDriveManager? = null

    /**
     * This must be called from the UI when the signed-in
     * account is available.
     */
    fun setAccount(account: GoogleSignInAccount?) {
        driveManager = if (account != null) {
            GoogleDriveManager(getApplication(), account)
        } else {
            null
        }
    }

    /**
     * Called when the "Backup" button is clicked.
     */
    fun backupDatabase() {
        if (driveManager == null) {
            _state.value = BackupRestoreState.ERROR
            return
        }

        viewModelScope.launch(Dispatchers.IO) {  // <-- Background dispatcher for I/O
            try {
                _state.value = BackupRestoreState.LOADING  // Safe: StateFlow is thread-safe

                val success = driveManager!!.backupDatabase()

                withContext(Dispatchers.Main) {  // <-- Switch to Main for UI update
                    _state.value = if (success) BackupRestoreState.SUCCESS else BackupRestoreState.ERROR
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = BackupRestoreState.ERROR
                }
            }
        }
    }

    /**
     * Called when the "Restore" button is clicked.
     * This is the most complex operation.
     */
    fun restoreDatabase() {
        if (driveManager == null) {
            _state.value = BackupRestoreState.ERROR
            return
        }

        viewModelScope.launch(Dispatchers.IO) {  // <-- Background dispatcher for I/O
            try {
                _state.value = BackupRestoreState.LOADING  // Safe: StateFlow is thread-safe

                // 1. CRITICAL: Close the database completely.
                // This ensures no files are "locked" by Room.
                AppDatabase.closeInstance()

                // 2. Perform the download/restore
                val success = driveManager!!.restoreDatabase()

                withContext(Dispatchers.Main) {  // <-- Switch to Main for UI update
                    if (success) {
                        _state.value = BackupRestoreState.SUCCESS
                        // The app must be restarted to re-open the database
                    } else {
                        _state.value = BackupRestoreState.ERROR
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = BackupRestoreState.ERROR
                }
            }
        }
    }

    /**
     * Resets the state back to IDLE (e.g., to hide a "Success" message).
     */
    fun resetState() {
        _state.value = BackupRestoreState.IDLE
    }
}