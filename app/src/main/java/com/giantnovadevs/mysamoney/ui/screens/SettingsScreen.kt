package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.AuthViewModel
import com.giantnovadevs.mysamoney.viewmodel.BackupRestoreState
import com.giantnovadevs.mysamoney.viewmodel.BackupViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.giantnovadevs.mysamoney.viewmodel.SettingsViewModel
import com.giantnovadevs.mysamoney.viewmodel.ThemeOption
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

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
    val snackbarHostState = remember { SnackbarHostState() }

    var lastOperation by remember { mutableStateOf<String?>(null) }
    val isPro by proViewModel.isProUser.collectAsState()
    val proPrice by proViewModel.proProductPrice.collectAsState()

    LaunchedEffect(account) { backupViewModel.setAccount(account) }

    LaunchedEffect(backupState) {
        if (backupState == BackupRestoreState.SUCCESS) {
            val message = if (lastOperation == "restore") "Restoring..." else "Backup Complete!"
            scope.launch { snackbarHostState.showSnackbar(message) }

            if (lastOperation == "restore") {
                delay(2000)
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                (context as? Activity)?.finish()
                exitProcess(0)
            }
            backupViewModel.resetState()
        } else if (backupState == BackupRestoreState.ERROR) {
            scope.launch { snackbarHostState.showSnackbar("Operation Failed. Check connection.") }
            backupViewModel.resetState()
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) authViewModel.handleSignInResult(result.data)
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- 1. THEME SECTION ---
            SettingsCard(title = "App Appearance") {
                Text("Free Themes", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(freeThemes) { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = theme.palette == currentTheme,
                            isLocked = false,
                            onClick = { viewModel.saveTheme(theme) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("Pro Themes", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(proThemes) { theme ->
                        ThemeCard(
                            theme = theme,
                            isSelected = theme.palette == currentTheme,
                            isLocked = !isPro,
                            onClick = {
                                if (isPro) viewModel.saveTheme(theme)
                                else navController.navigate("upgrade")
                            }
                        )
                    }
                }
            }

            // --- 2. BACKUP & SYNC (Pro feature) ---
            SettingsCard(title = "Cloud Sync") {
                if (!isPro) {
                    // Non-Pro: show locked state with upgrade CTA
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Google Drive Backup",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Upgrade to Pro to enable cloud backup & restore.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { navController.navigate("upgrade") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Upgrade to Pro — $proPrice")
                    }
                } else if (account == null) {
                    Button(
                        onClick = { signInLauncher.launch(authViewModel.getSignInIntent()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Sign In to Enable Backup")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Signed in as", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(account!!.email ?: "User", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        TextButton(onClick = { authViewModel.signOut() }) { Text("Sign Out") }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { lastOperation = "backup"; backupViewModel.backupDatabase() },
                            modifier = Modifier.weight(1f),
                            enabled = backupState == BackupRestoreState.IDLE
                        ) {
                            if (backupState == BackupRestoreState.LOADING && lastOperation == "backup") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Backup")
                            }
                        }

                        OutlinedButton(
                            onClick = { lastOperation = "restore"; backupViewModel.restoreDatabase() },
                            modifier = Modifier.weight(1f),
                            enabled = backupState == BackupRestoreState.IDLE
                        ) {
                            if (backupState == BackupRestoreState.LOADING && lastOperation == "restore") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }

            if (!isPro) {
                AdMobBanner(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }

            // --- 3. PRO UPGRADE ---
            if (!isPro) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Upgrade to Pro",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Unlock premium themes, unlimited scans, and remove ads.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = { (context as? Activity)?.let { proViewModel.launchPurchase(it) } },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Get Pro for $proPrice")
                        }
                    }
                }
            }
        }
    }
}

// --- REUSABLE COMPONENTS ---

@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ThemeCard(
    theme: ThemeOption,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val primaryColor = theme.palette.lightColorScheme.primary
    val secondaryColor = theme.palette.lightColorScheme.secondaryContainer
    val borderColor = if (isSelected) primaryColor else CardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 100.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardBackground)
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .clickable { onClick() }
        ) {
            // Theme Preview Visual
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Primary Color)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(primaryColor)
                )
                // Body (Secondary Color)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .background(secondaryColor.copy(alpha = 0.5f))
                )
            }

            // Selection Indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Locked Indicator
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = theme.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) primaryColor else TextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}