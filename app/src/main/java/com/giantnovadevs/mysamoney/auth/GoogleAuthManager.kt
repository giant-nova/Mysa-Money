package com.giantnovadevs.mysamoney.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.drive.DriveScopes
import com.giantnovadevs.mysamoney.R // Import your R file
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(context: Context) {

    private val appContext = context.applicationContext

    // 1. Configure Google Sign-In
    private val gso: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Request the Web Client ID you stored in strings.xml
            .requestIdToken(context.getString(R.string.your_web_client_id))
            .requestEmail() // Get the user's email to display
            // ✅ This is the key: Request permission ONLY for the appDataFolder
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

    // 2. Build the client
    private val googleSignInClient = GoogleSignIn.getClient(appContext, gso)

    /**
     * Checks if a user is already signed in when the app starts.
     * This is a fast, non-blocking check.
     */
    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(appContext)
    }


    /**
     * Gets the Intent for the Google Sign-In flow.
     * This is NOT a suspend function.
     */
    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    /**
     * Handles the result from the sign-in pop-up.
     * This returns the signed-in account if successful.
     */
    fun handleSignInResult(data: Intent?): GoogleSignInAccount? {
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            task.getResult(ApiException::class.java) // This throws if it failed
        } catch (e: ApiException) {
            e.printStackTrace()
            null // Sign-in failed
        }
    }

    /**
     * Signs the user out of the app.
     */
    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}