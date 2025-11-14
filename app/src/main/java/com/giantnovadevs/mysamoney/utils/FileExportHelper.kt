package com.giantnovadevs.mysamoney.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.OutputStream

/**
 * A helper class to handle saving files to the "Download" folder,
 * respecting Scoped Storage on modern Android.
 */
class FileExportHelper(private val context: Context) {

    /**
     * Creates an empty file in the "Download" folder and returns an OutputStream
     * that you can write your content (CSV, PDF, etc.) into.
     *
     * @param fileName The name of the file to create (e.g., "export.csv")
     * @param mimeType The type of file (e.g., "text/csv", "application/pdf")
     * @return An OutputStream to write to, or null if it fails.
     */
    fun getOutputStream(fileName: String, mimeType: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // --- Modern way (Android 10+) ---
            // We use MediaStore to save to the "Download" collection.
            // No permissions are needed.
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { resolver.openOutputStream(it) }

        } else {
            // --- Legacy way (Android 9 and below) ---
            // We need the WRITE_EXTERNAL_STORAGE permission for this.
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = java.io.File(directory, fileName)
            file.outputStream()
        }
    }
}