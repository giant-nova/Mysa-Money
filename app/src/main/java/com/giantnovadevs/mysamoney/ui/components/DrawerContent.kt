package com.giantnovadevs.mysamoney.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.giantnovadevs.mysamoney.BuildConfig
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.google.android.gms.ads.MobileAds

// --- Theme Colors ---
private val DrawerBackground = Color(0xFFF6F7F9)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

@Composable
fun AppDrawer(navController: NavController, proViewModel: ProViewModel, onClose: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPro by proViewModel.isProUser.collectAsState()
    val context = LocalContext.current

    ModalDrawerSheet(
        drawerContainerColor = DrawerBackground,
        drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            // --- 1. Header ---
            DrawerHeader()

            Spacer(Modifier.height(24.dp))

            // --- 2. Pro Upgrade Banner ---
            if (!isPro) {
                PremiumUpgradeCard(onClick = {
                    navController.navigate("upgrade")
                    onClose()
                })
                Spacer(Modifier.height(16.dp))
            }

            // --- 3. Dashboard Group ---
            DrawerSectionTitle("Dashboard")
            DrawerItem("Home", Icons.Default.Home, currentRoute == "home") {
                navController.navigate("home"); onClose()
            }
            DrawerItem("Summary", Icons.Default.Leaderboard, currentRoute == "summary") {
                navController.navigate("summary"); onClose()
            }
            DrawerItem("AI Coach", Icons.Default.AutoAwesome, currentRoute == "coach") {
                navController.navigate("coach"); onClose()
            }

            Spacer(Modifier.height(16.dp))

            // --- 4. Tracking Group ---
            DrawerSectionTitle("Tracking")
            DrawerItem("All Expenses", Icons.AutoMirrored.Filled.List, currentRoute == "list") {
                navController.navigate("list"); onClose()
            }
            DrawerItem("Incomes", Icons.Default.TrendingUp, currentRoute == "incomes") {
                navController.navigate("incomes"); onClose()
            }
            DrawerItem("Subscriptions", Icons.Default.Refresh, currentRoute == "recurring_expenses") {
                navController.navigate("recurring_expenses"); onClose()
            }

            Spacer(Modifier.height(16.dp))

            // --- 5. Manage Group ---
            DrawerSectionTitle("Manage")
            DrawerItem("Budgets", Icons.Default.Savings, currentRoute == "budgets") {
                navController.navigate("budgets"); onClose()
            }
            DrawerItem("Categories", Icons.Default.Category, currentRoute == "categories") {
                navController.navigate("categories"); onClose()
            }

            Spacer(Modifier.height(16.dp))

            // --- 6. App Group ---
            HorizontalDivider(color = Color(0xFFEBEBEB), modifier = Modifier.padding(vertical = 8.dp))
            DrawerItem("Settings", Icons.Default.Settings, currentRoute == "settings") {
                navController.navigate("settings"); onClose()
            }
            DrawerItem("About", Icons.Default.Info, currentRoute == "about") {
                navController.navigate("about"); onClose()
            }
            DrawerItem("Ad Inspector", Icons.Default.Build, false) {
                MobileAds.openAdInspector(context) { error ->
                    if (error != null) {
                        Toast.makeText(context, "Ad Inspector failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                onClose()
            }

            // --- Footer ---
            Spacer(Modifier.weight(1f))
            DrawerFooter()
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountBalanceWallet,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = "Mysa Money",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Your Financial Hub",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PremiumUpgradeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            Color(0xFF8B5CF6)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Pro",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Unlock Mysa Pro",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "No ads, advanced tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 8.dp)
    )
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = label,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        selected = isSelected,
        icon = { Icon(icon, contentDescription = label) },
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedContainerColor = Color.Transparent,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextPrimary
        ),
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(48.dp)
    )
}

@Composable
private fun DrawerFooter() {
    val appVersion = BuildConfig.VERSION_NAME
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Made with ❤️ in India",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Text(
            text = "Version $appVersion",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.6f)
        )
    }
}
