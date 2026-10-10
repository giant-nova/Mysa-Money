package com.giantnovadevs.mysamoney.utils

import android.content.IntentSender
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "InAppUpdateManager"

enum class UpdateState { NONE, AVAILABLE, DOWNLOADING, DOWNLOADED, DISMISSED }

class InAppUpdateManager(activity: ComponentActivity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private var pendingInfo: AppUpdateInfo? = null

    private val _updateState = MutableStateFlow(UpdateState.NONE)
    val updateState = _updateState.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = state.totalBytesToDownload()
                _downloadProgress.value = if (total > 0) state.bytesDownloaded().toFloat() / total else 0f
                _updateState.value = UpdateState.DOWNLOADING
            }
            InstallStatus.DOWNLOADED -> {
                _downloadProgress.value = 1f
                _updateState.value = UpdateState.DOWNLOADED
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                // Allow the user to try again from the update screen.
                if (_updateState.value == UpdateState.DOWNLOADING) {
                    _updateState.value = UpdateState.AVAILABLE
                }
            }
            else -> Unit
        }
    }

    /**
     * Call in onCreate. Checks Play Store for an available update and sets state accordingly.
     * Does NOT start the download — the user triggers that from the UpdateScreen.
     */
    fun checkForUpdate() {
        appUpdateManager.registerListener(installStateListener)
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> {
                        // Download already finished (e.g. from Play Store auto-update).
                        _downloadProgress.value = 1f
                        _updateState.value = UpdateState.DOWNLOADED
                    }
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        pendingInfo = info
                        _updateState.value = UpdateState.AVAILABLE
                    }
                }
            }
            .addOnFailureListener {
                Log.w(TAG, "Update check failed: ${it.message}")
            }
    }

    /**
     * Call when the user taps "Update Now". Shows Play's consent dialog, then starts download.
     */
    fun startDownload(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val info = pendingInfo ?: return
        try {
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Failed to launch update flow: ${e.message}")
        }
    }

    /**
     * Call in onResume — re-prompts if a download finished while the app was backgrounded.
     */
    fun onResume() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _downloadProgress.value = 1f
                _updateState.value = UpdateState.DOWNLOADED
            }
        }
    }

    fun dismiss() {
        _updateState.value = UpdateState.DISMISSED
    }

    /** Triggers the restart + install. */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
    }

    fun unregister() {
        appUpdateManager.unregisterListener(installStateListener)
    }
}
