package com.giantnovadevs.mysamoney.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current // Native Compose way to open links
    val appVersion = BuildConfig.VERSION_NAME
    val isPro by proViewModel.isProUser.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- 1. App Header ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(10.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet, // Or your app logo
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Mysa Money",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Version $appVersion",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }

            // --- 2. App Actions Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AboutActionTile(
                        icon = Icons.Default.Star,
                        title = "Rate on Play Store",
                        color = Color(0xFFFF9F0A), // Orange
                        onClick = { openPlayStore(context) }
                    )
                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    AboutActionTile(
                        icon = Icons.Default.Share,
                        title = "Share with Friends",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { shareApp(context) }
                    )
                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    AboutActionTile(
                        icon = Icons.Default.Email,
                        title = "Contact Support",
                        color = Color(0xFF34C759), // Green
                        onClick = { contactSupport(context) }
                    )
                }
            }

            // --- 3. Developer Profiles Card ---
            Text(
                text = "Meet the Developer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.align(Alignment.Start)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    AboutActionTile(
                        icon = Icons.Default.Code,
                        title = "GitHub",
                        subtitle = "@giant-nova",
                        color = Color(0xFF333333), // Dark Grey
                        onClick = { uriHandler.openUri("https://github.com/giant-nova") } // UPDATE LINK
                    )
                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    AboutActionTile(
                        icon = Icons.Default.WorkOutline,
                        title = "LinkedIn",
                        subtitle = "Let's connect",
                        color = Color(0xFF0077B5), // LinkedIn Blue
                        onClick = { uriHandler.openUri("https://www.linkedin.com/in/kunal7266/") } // UPDATE LINK
                    )
                    HorizontalDivider(color = CardBorder, modifier = Modifier.padding(horizontal = 20.dp))
                    AboutActionTile(
                        icon = Icons.Default.Public,
                        title = "Portfolio",
                        subtitle = "Please visit",
                        color = Color(0xFF5E5CE6), // Indigo
                        onClick = { uriHandler.openUri("https://dev-portfolio.mysalifestore.in/") } // UPDATE LINK
                    )
                }
            }

            // --- 4. Footer ---
            if (!isPro) {
                AdMobBanner(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Made with ❤️ in India",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Text(
                    text = "© ${java.time.Year.now()} GiantNova Devs",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AboutActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// --- Helper Functions ---

private fun openPlayStore(context: Context) {
    val packageName = context.packageName
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        )
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        )
    }
}

private fun shareApp(context: Context) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "Check out Mysa Money, the best expense tracker! Download here: https://play.google.com/store/apps/details?id=${context.packageName}")
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
}

private fun contactSupport(context: Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("giantnova.devs@gmail.com"))
        putExtra(Intent.EXTRA_SUBJECT, "Mysa Money Support Request")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Handle no email app case gracefully if needed
    }
}