package com.example.spendwise.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendwise.auth.GoogleAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val authManager = GoogleAuthManager(app)

    // This holds the currently signed-in user
    private val _account = MutableStateFlow<GoogleSignInAccount?>(null)
    val account = _account.asStateFlow()

    init {
        // Check if a user is already signed in when the app starts
        _account.value = authManager.getSignedInAccount()
    }

    /**
     * Launches the Google Sign-In pop-up.
     * The UI will observe this flow and launch the intent.
     */
    fun getSignInIntent(): Intent {
        return authManager.getSignInIntent()
    }

    /**
     * Handles the result from the sign-in pop-up
     * and updates the account state.
     */
    fun handleSignInResult(data: Intent?) {
        val account = authManager.handleSignInResult(data)
        if (account != null) {
            _account.value = account
        } else {
            // Handle sign-in failure
        }
    }

    /**
     * Signs the user out.
     */
    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _account.value = null
        }
    }
}