package com.giantnovadevs.mysamoney

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.giantnovadevs.mysamoney.ui.components.AppDrawer
import com.giantnovadevs.mysamoney.ui.navigation.AppNavGraph
import com.giantnovadevs.mysamoney.ui.theme.MysaMoneyTheme
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val proViewModel: ProViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentTheme by settingsViewModel.currentTheme.collectAsState()
            val currentFont by settingsViewModel.currentFont.collectAsState()

            MysaMoneyTheme(
                palette = currentTheme,
                fontFamily = currentFont
            ) {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val context = LocalContext.current

                var showExitDialog by remember { mutableStateOf(false) }

                val navToHome: () -> Unit = {
                    navController.navigate("home") {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }

                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                BackHandler(enabled = true) {
                    if (drawerState.isOpen) {
                        // If drawer is open, just close it
                        scope.launch { drawerState.close() }
                    } else if (currentRoute != "home") {
                        // If on any other screen, go home
                        navToHome()
                    } else {
                        // If on Home, show the exit dialog
                        showExitDialog = true
                    }
                }


                // --- The Exit Dialog (Unchanged) ---
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
                            MediumRectangleAd()
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

                // --- Your Main UI (Unchanged) ---
                ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
                    AppDrawer(navController, proViewModel) { scope.launch { drawerState.close() } }
                }) {
                    AppNavGraph(
                        navController = navController,
                        settingsViewModel = settingsViewModel,
                        authViewModel = authViewModel,
                        proViewModel = proViewModel,
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumRectangleAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp), // Set fixed height
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.MEDIUM_RECTANGLE)
                // Use the standard Test Ad Unit ID
                adUnitId = "ca-app-pub-3940256099942544/9214589741"
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}