package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    navController: NavController,
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val proPrice by proViewModel.proProductPrice.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade to Pro") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Go Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        // A persistent "call to action" button at the bottom
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            proViewModel.launchPurchase(activity)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    // If they are already Pro, this screen is just informational
                    enabled = !isPro
                ) {
                    Text(
                        text = if (isPro) "You are already a Pro member!" else "Upgrade Now ($proPrice)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            item {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Pro",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Mysa Money Pro",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Get peace of mind and unlock your full financial potential. All for a one-time purchase.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                Divider()
            }

            // --- The Comparison Table ---
            item {
                ComparisonTable()
            }
        }
    }
}

/**
 * The comparison table
 */
@Composable
private fun ComparisonTable() {
    Column(
        modifier = Modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Feature",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(2f),
                fontWeight = FontWeight.Bold
            )
            Text(
                "Free",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Pro",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Feature Rows
        FeatureComparisonRow("Remove All Ads", false, true)
        FeatureComparisonRow("Unlimited AI Coach", false, true)
        FeatureComparisonRow("Google Drive Backup", false, true)
        FeatureComparisonRow("Receipt Scanning (OCR)", false, true)
        FeatureComparisonRow("Premium App Themes", false, true)
        FeatureComparisonRow("Track Expenses & Income", true, true)
        FeatureComparisonRow("Monthly Budgets", true, true)
        FeatureComparisonRow("Advanced Reports", true, true)
    }
}

/**
 * A helper composable for one row in the comparison table
 */
@Composable
private fun FeatureComparisonRow(
    featureName: String,
    isFree: Boolean,
    isPro: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Feature Name
        Text(
            featureName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(2f)
        )
        // Free Checkmark
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureCheckmark(isAvailable = isFree)
        }
        // Pro Checkmark
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureCheckmark(isAvailable = isPro, isPro = true)
        }
    }
}

/**
 * A simple checkmark or cross icon
 */
@Composable
private fun FeatureCheckmark(isAvailable: Boolean, isPro: Boolean = false) {
    val icon: ImageVector
    val tint: Color

    if (isAvailable) {
        icon = Icons.Filled.Check
        tint = if (isPro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        icon = Icons.Filled.Close
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Icon(icon, contentDescription = if (isAvailable) "Available" else "Not Available", tint = tint)
}