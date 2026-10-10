package com.giantnovadevs.mysamoney

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giantnovadevs.mysamoney.ads.AppOpenAdManager
import com.giantnovadevs.mysamoney.ui.components.AppDrawer
import com.giantnovadevs.mysamoney.ui.screens.AdMobMediumRectangle
import com.giantnovadevs.mysamoney.ui.navigation.AppNavGraph
import com.giantnovadevs.mysamoney.ui.theme.MysaMoneyTheme
import com.giantnovadevs.mysamoney.ui.screens.UpdateScreen
import com.giantnovadevs.mysamoney.utils.InAppUpdateManager
import com.giantnovadevs.mysamoney.utils.UpdateState
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val proViewModel: ProViewModel by viewModels()

    private lateinit var inAppUpdateManager: InAppUpdateManager

    // Must be registered before onStart — property initializer runs before onCreate.
    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* download happens in background; result is tracked via installStateListener */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inAppUpdateManager = InAppUpdateManager(this)
        inAppUpdateManager.checkForUpdate()
        setContent {
            val currentTheme by settingsViewModel.currentTheme.collectAsState()
            val currentFont by settingsViewModel.currentFont.collectAsState()
            val updateState by inAppUpdateManager.updateState.collectAsState()
            val downloadProgress by inAppUpdateManager.downloadProgress.collectAsState()

            MysaMoneyTheme(
                palette = currentTheme,
                fontFamily = currentFont
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val scope = rememberCoroutineScope()
                    val context = LocalContext.current

                    var showExitDialog by remember { mutableStateOf(false) }
                    val isPro by proViewModel.isProUser.collectAsState()

                    val navToHome: () -> Unit = {
                        navController.navigate("home") {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }

                    val backStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = backStackEntry?.destination?.route

                    BackHandler(enabled = true) {
                        if (drawerState.isOpen) {
                            scope.launch { drawerState.close() }
                        } else if (currentRoute != "home") {
                            navToHome()
                        } else {
                            showExitDialog = true
                        }
                    }

                    if (showExitDialog) {
                        Dialog(
                            onDismissRequest = { showExitDialog = false },
                            properties = DialogProperties(usePlatformDefaultWidth = false)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (!isPro) AdMobMediumRectangle()
                                Card(
                                    shape = MaterialTheme.shapes.large,
                                    elevation = CardDefaults.cardElevation(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Do you really want to quit?", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                                        Spacer(Modifier.height(20.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { showExitDialog = false }) {
                                                Text("No")
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Button(onClick = {
                                                (context as? Activity)?.finish()
                                            }) {
                                                Text("Yes, Exit")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
                        AppDrawer(navController, proViewModel) { scope.launch { drawerState.close() } }
                    }) {
                        AppNavGraph(
                            navController = navController,
                            settingsViewModel = settingsViewModel,
                            authViewModel = authViewModel,
                            proViewModel = proViewModel,
                            onMenuClick = {
                                scope.launch { drawerState.open() }
                            }
                        )
                    }

                    // Update screen slides up over the app content when an update is available.
                    AnimatedVisibility(
                        visible = updateState != UpdateState.NONE && updateState != UpdateState.DISMISSED,
                        enter = fadeIn(tween(300)) + slideInVertically { it },
                        exit = fadeOut(tween(300))
                    ) {
                        UpdateScreen(
                            updateState = updateState,
                            downloadProgress = downloadProgress,
                            onUpdateNow = { inAppUpdateManager.startDownload(updateLauncher) },
                            onLater = { inAppUpdateManager.dismiss() },
                            onRestart = { inAppUpdateManager.completeUpdate() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-prompt if the update finished downloading while the app was backgrounded.
        inAppUpdateManager.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.unregister()
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            // Wait up to 2s for DataStore + Play Billing to emit the real Pro status.
            // After the first launch where billing confirms Pro, DataStore persists it,
            // so subsequent launches resolve in <50ms from DataStore alone.
            val isPro = withTimeoutOrNull(2_000) {
                proViewModel.isProUser.first { it }
            } != null
            if (!isPro) {
                // Pass the live predicate so if the ad is still loading when this runs,
                // it re-checks isProUser at actual show time (catches late billing responses).
                AppOpenAdManager.showIfAvailable(
                    activity = this@MainActivity,
                    shouldShow = { !proViewModel.isProUser.value }
                )
            }
        }
    }
}