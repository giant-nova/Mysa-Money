package com.giantnovadevs.mysamoney.auth

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.FileOutputStream
import java.util.Collections

class GoogleDriveManager(
    private val context: Context,
    private val account: GoogleSignInAccount
) {
    // The database file name
    companion object {
        const val BACKUP_FILE_NAME = "mysamoney.db"
    }

    // 1. Create the authorized Drive service client
    private val driveService: Drive by lazy {
        // Use the account to get the credentials
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account.account
        }

        // Build the Drive service
        Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("MysaMoney")
            .build()
    }

    /**
     * Searches the appDataFolder for our backup file.
     * Returns the File ID if found, or null if not.
     */
    private suspend fun findBackupFile(): String? {
        val query = "name = '$BACKUP_FILE_NAME'"
        val result = driveService.files().list()
            .setQ(query)
            .setSpaces("appDataFolder") // <-- Search *only* the hidden folder
            .setFields("files(id, name)")
            .execute()

        return result.files.firstOrNull()?.id
    }

    /**
     * Uploads the app's database to the appDataFolder.
     * It will overwrite any existing backup.
     */
    suspend fun backupDatabase(): Boolean {
        return try {
            val localDbFile = context.getDatabasePath(BACKUP_FILE_NAME)
            if (!localDbFile.exists()) {
                throw Exception("Local database not found")
            }

            val mediaContent = com.google.api.client.http.FileContent(
                "application/x-sqlite3",
                localDbFile
            )

            // Check if a file already exists
            val fileId = findBackupFile()

            if (fileId == null) {
                // Not found, create a new file
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    // Set the parent to the appDataFolder (writable in CREATE)
                    parents = listOf("appDataFolder")
                }
                driveService.files().create(fileMetadata, mediaContent).execute()
            } else {
                // Found, update the existing file (omit parents—already set and not writable here)
                val fileMetadata = File().apply {
                    name = BACKUP_FILE_NAME
                    // No parents: Use addParents/removeParents params if needed in future
                }
                driveService.files().update(fileId, fileMetadata, mediaContent).execute()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Downloads the backup file from Google Drive and replaces the
     * local database.
     */
    suspend fun restoreDatabase(): Boolean {
        return try {
            val fileId = findBackupFile()
                ?: throw Exception("No backup file found in Google Drive")

            val localDbFile = context.getDatabasePath(BACKUP_FILE_NAME)

            // Close the database before trying to replace it
            // (We'll add this to the ViewModel)

            // Download the file from Drive
            FileOutputStream(localDbFile).use { outputStream ->
                driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}