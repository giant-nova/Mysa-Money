package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlinx.coroutines.delay

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)
private val ProHighlight = Color(0xFF8B5CF6) // A premium purple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradeScreen(
    navController: NavController,
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val proPrice by proViewModel.proProductPrice.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()

    // --- Animation States ---
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100) // Slight delay for smoother entrance
        isVisible = true
    }

    // 1. Floating Animation for the Hero Icon
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconFloat"
    )

    // 2. Heartbeat/Pulse Animation for the Button
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.00f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )

    val navToHome: () -> Unit = {
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mysa Pro",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navToHome() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = AppBackground,
                modifier = Modifier.padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) proViewModel.launchPurchase(activity)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = if (!isPro) buttonScale else 1f
                            scaleY = if (!isPro) buttonScale else 1f
                        },
                    enabled = !isPro,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContentColor = MaterialTheme.colorScheme.primary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 8.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        text = if (isPro) "You're already a Pro!" else "Unlock Now ($proPrice)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- Hero Section ---
            item {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 16.dp)
                        .graphicsLayer { translationY = floatOffset } // Applying the float
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, ProHighlight)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AllInclusive,
                        contentDescription = "Pro",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = "Upgrade to Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Get peace of mind and unlock your full financial potential. One-time purchase, yours forever.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                )
            }

            // --- Comparison Table ---
            item {
                AnimatedVisibility(
                    visible = isVisible,
                    enter = slideInVertically(
                        initialOffsetY = { 100 },
                        animationSpec = tween(600, easing = EaseOutBack)
                    ) + fadeIn(tween(600))
                ) {
                    ComparisonTable()
                }

                // Extra padding for the bottom button
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * Revamped Comparison Table Card
 */
@Composable
private fun ComparisonTable() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Features",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Free",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "PRO",
                    style = MaterialTheme.typography.labelLarge,
                    color = ProHighlight, // Highlighted text
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(bottom = 8.dp))

            // Feature Rows
            FeatureComparisonRow("Remove All Ads", false, true)
            FeatureComparisonRow("Unlimited AI Coach", false, true)
            FeatureComparisonRow("Google Drive Backup", false, true)
            FeatureComparisonRow("Premium App Themes", false, true)
            FeatureComparisonRow("Monthly Budgets", true, true)
            FeatureComparisonRow("Advanced Reports", true, true)
        }
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
            .padding(vertical = 12.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Feature Name
        Text(
            text = featureName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(2f)
        )

        // Free Checkmark
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            FeatureCheckmark(isAvailable = isFree)
        }

        // Pro Checkmark (With subtle background highlight)
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(ProHighlight.copy(alpha = 0.05f)) // Subtle highlight
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            FeatureCheckmark(isAvailable = isPro, isPro = true)
        }
    }
}

/**
 * Modern checkmark or cross icon
 */
@Composable
private fun FeatureCheckmark(isAvailable: Boolean, isPro: Boolean = false) {
    if (isAvailable) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Available",
            tint = if (isPro) ProHighlight else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Not Available",
            tint = TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}