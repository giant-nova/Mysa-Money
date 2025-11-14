package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel
import com.giantnovadevs.mysamoney.viewmodel.ThemeOption
import androidx.compose.material3.SegmentedButtonDefaults
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.BackupRestoreState
import com.giantnovadevs.mysamoney.viewmodel.BackupViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlin.system.exitProcess
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    backupViewModel: BackupViewModel = viewModel(),
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val freeThemes = viewModel.freeThemes
    val proThemes = viewModel.proThemes
    val currentTheme by viewModel.currentTheme.collectAsState()

    val account by authViewModel.account.collectAsState()
    val backupState by backupViewModel.state.collectAsState()

    // Create the SnackbarHostState
    val snackbarHostState = remember { SnackbarHostState() }

    // Local state to track the last operation for success/error messaging
    var lastOperation by remember { mutableStateOf<String?>(null) }
    val isPro by proViewModel.isProUser.collectAsState() // ✅ Get Pro status
    val proPrice by proViewModel.proProductPrice.collectAsState() // ✅ Get Pro price

    // --- 1. Flaw Fix: Tell the BackupViewModel who is signed in ---
    LaunchedEffect(account) {
        backupViewModel.setAccount(account)
    }

    // --- 2. Flaw Fix: Correct Backup/Restore State Handling (The major fix) ---
    LaunchedEffect(backupState) {
        when (backupState) {
            BackupRestoreState.SUCCESS -> {
                val message = when (lastOperation) {
                    "backup" -> "Backup Complete!"
                    "restore" -> "Restore successful. Restarting app..."
                    else -> "Operation Complete!"
                }
                val duration = if (lastOperation == "restore") SnackbarDuration.Long else SnackbarDuration.Short
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = duration
                    )
                }

                // CRITICAL FLAW FIX: This must ONLY run after a successful RESTORE.
                if (lastOperation == "restore") {
                    delay(2500L) // Wait for snackbar to show
                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                    exitProcess(0)
                }
                backupViewModel.resetState()
                lastOperation = null
            }
            BackupRestoreState.ERROR -> {
                val message = when (lastOperation) {
                    "backup" -> "Backup failed. Check internet or permissions."
                    "restore" -> "Restore failed. Check internet or permissions."
                    else -> "Operation Failed. Check internet/permissions."
                }
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Long
                    )
                }
                backupViewModel.resetState()
                lastOperation = null
            }
            else -> { /* IDLE / LOADING, do nothing */ }
        }
    }

    // The Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            authViewModel.handleSignInResult(result.data)
        } else {
            // Handle sign-in cancellation or error (e.g., via Snackbar)
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Sign-in cancelled.",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // ✅ Added Snackbar Host
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- Theme Selector ---
            Text(
                "Select Theme",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Free Themes", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(freeThemes) { theme ->
                    ThemeColorButton(
                        theme = theme,
                        isSelected = theme.palette == currentTheme,
                        isLocked = false, // Free themes are never locked
                        onClick = {
                            viewModel.saveTheme(theme)
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Pro Themes (Unlock with Pro)", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(proThemes) { theme ->
                    ThemeColorButton(
                        theme = theme,
                        isSelected = theme.palette == currentTheme,
                        isLocked = !isPro, // ✅ Lock if user is NOT Pro
                        onClick = {
                            if (isPro) {
                                viewModel.saveTheme(theme)
                            } else {
                                // User is free, send to upgrade screen
                                navController.navigate("upgrade")
                            }
                        }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Backup & Sync Section ---
            Text(
                "Backup & Sync",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Show Loading Spinner if busy
            if (backupState == BackupRestoreState.LOADING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = when (lastOperation) {
                            "backup" -> "Backing up..."
                            "restore" -> "Restoring..."
                            else -> "Please wait..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (account == null) {
                // User is SIGNED OUT
                Button(
                    onClick = {
                        val signInIntent = authViewModel.getSignInIntent()
                        signInLauncher.launch(signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign In with Google to Enable Backup")
                }
            } else {
                // User is SIGNED IN
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // "Signed in as" text (unchanged)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Signed in as:\n${account!!.email ?: "Unknown account"}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { authViewModel.signOut() }) {
                            Text("Sign Out")
                        }
                    }

                    // New Button Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Backup button (Primary)
                        Button(
                            onClick = {
                                lastOperation = "backup"
                                backupViewModel.backupDatabase()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = backupState == BackupRestoreState.IDLE,
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Backup")
                        }

                        // Restore button (Secondary)
                        FilledTonalButton(
                            onClick = {
                                lastOperation = "restore"
                                backupViewModel.restoreDatabase()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = backupState == BackupRestoreState.IDLE,
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("Restore")
                        }
                    }
                }
            }
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Pro Upgrade Section ---
            Text(
                "Mysa Money Pro",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isPro) {
                // User is Pro
                Text(
                    "You are a Pro member! Thank you for your support.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Button(
                    onClick = {
                        proViewModel.consumeTestPurchase {
                            // This will force a restart to clear the state
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                            exitProcess(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("DEBUG: Consume Pro Purchase (to re-test)")
                }

            } else {
                // User is not Pro
                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            proViewModel.launchPurchase(activity)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade to Pro ($proPrice)")
                }
            }
        }
    }
}

@Composable
private fun ThemeColorButton(
    theme: ThemeOption,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val color = theme.palette.lightColorScheme.primary
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .size(50.dp)
            .border(2.dp, borderColor, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(enabled = !isLocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "${theme.name} selected",
                tint = theme.palette.lightColorScheme.onPrimary
            )
        } else if (isLocked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "${theme.name} locked",
                tint = theme.palette.lightColorScheme.onPrimary.copy(alpha = 0.8f)
            )
        }
    }
}