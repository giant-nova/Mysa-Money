package com.giantnovadevs.mysamoney.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category // ✅ Now you can import this!
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard // ✅ A better icon for "Summary"
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.collectAsState
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel

@Composable
fun AppDrawer(navController: NavController, onClose: () -> Unit, proViewModel: ProViewModel,) {
    // ✅ Get the current route to show the selected item
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isPro by proViewModel.isProUser.collectAsState()
    ModalDrawerSheet {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Mysa Money",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Divider()
            Spacer(Modifier.height(16.dp))
            if (!isPro) {
                Button(
                    onClick = { navController.navigate("upgrade"); onClose() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Upgrade to Pro")
                }
                Divider()
            }

            // ✅ Pass the current route to the DrawerItem
            DrawerItem(
                label = "Home",
                icon = Icons.Default.Home,
                isSelected = currentRoute == "home",
                onClick = { navController.navigate("home"); onClose() }
            )
            DrawerItem(
                label = "Add Expense",
                icon = Icons.Default.Add,
                isSelected = currentRoute == "add",
                onClick = { navController.navigate("add"); onClose() }
            )
            DrawerItem(
                label = "All Expenses",
                icon = Icons.AutoMirrored.Filled.List,
                isSelected = currentRoute == "list",
                onClick = { navController.navigate("list"); onClose() }
            )
            DrawerItem(
                label = "AI Coach",
                icon = Icons.Default.AutoAwesome,
                isSelected = currentRoute == "coach",
                onClick = { navController.navigate("coach"); onClose() }
            )
            DrawerItem(
                label = "Summary",
                icon = Icons.Default.Leaderboard, // ✅ Better icon!
                isSelected = currentRoute == "summary",
                onClick = { navController.navigate("summary"); onClose() }
            )
            DrawerItem(
                label = "Categories",
                icon = Icons.Default.Category, // ✅ The icon you wanted!
                isSelected = currentRoute == "categories",
                onClick = { navController.navigate("categories"); onClose() }
            )
            DrawerItem(
                label = "Budgets",
                icon = Icons.Default.Savings, // A nice icon for budgets
                isSelected = currentRoute == "budgets",
                onClick = { navController.navigate("budgets"); onClose() }
            )
            DrawerItem(
                label = "Subscriptions",
                icon = Icons.Default.Refresh,
                isSelected = currentRoute == "recurring_expenses",
                onClick = { navController.navigate("recurring_expenses"); onClose() }
            )
            DrawerItem(
                label = "Incomes",
                icon = Icons.Default.TrendingUp,
                isSelected = currentRoute == "incomes",
                onClick = { navController.navigate("incomes"); onClose() }
            )
            DrawerItem(
                label = "Settings",
                icon = Icons.Default.Settings,
                isSelected = currentRoute == "settings",
                onClick = { navController.navigate("settings"); onClose() }
            )
            DrawerItem(
                label = "About",
                icon = Icons.Default.Info,
                isSelected = currentRoute == "about",
                onClick = { navController.navigate("about"); onClose() }
            )

            Spacer(Modifier.weight(1f)) // This pushes the footer to the bottom

            DrawerFooter()
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean, // ✅ Added this
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = isSelected, // ✅ Check if this item is the selected one
        icon = { Icon(icon, contentDescription = label) },
        onClick = onClick,
        // Add padding to make items cleaner
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

@Composable
fun DrawerFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Divider(modifier = Modifier.padding(bottom = 16.dp))
        Text("Version 1.2.0", style = MaterialTheme.typography.bodySmall)
        Text("Made with ❤️ in India", style = MaterialTheme.typography.bodySmall)
    }
}