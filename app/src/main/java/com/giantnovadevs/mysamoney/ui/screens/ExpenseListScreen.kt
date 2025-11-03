package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
// ✅ We now import our shared component
import com.giantnovadevs.mysamoney.ui.components.ExpenseItem
import androidx.compose.animation.animateContentSize
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(onMenuClick: () -> Unit) { // <-- Added onMenuClick
    val expVm: ExpenseViewModel = viewModel()
    val expenses by expVm.expenses.collectAsState()
    val context = LocalContext.current
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Expenses") },
                actions = {
                    IconButton(onClick = {
                        expVm.exportCSV { success, path ->
                            if (success) {
                                Toast.makeText(context, "Exported to: $path", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Export failed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.IosShare,
                            contentDescription = "Export as CSV",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                }
            )
        },
        bottomBar = {
            BannerAd()
        }
    ) { padding ->
        // ✅ Use Crossfade to animate between empty and list states
        Crossfade(
            targetState = expenses.isEmpty(),
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "Empty/List"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    contentPadding = PaddingValues(16.dp),
                    // ✅ Principle 1: Generous whitespace
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(expenses, key = { it.id }) { expense ->
                        val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
                        ExpenseItem(
                            expense = expense,
                            categoryName = categoryName, // <-- Pass the name here
                            onDelete = { expVm.deleteExpense(expense) },
                            modifier = Modifier.animateContentSize() // Your chosen animation
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    // ✅ A much cleaner, professional empty state
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = "No Expenses",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No expenses found",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Add a new expense from the home screen to see your list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A reusable composable for showing a banner ad
 * with an error listener for debugging.
 */
@Composable
private fun BannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current // Get context

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = {
            AdView(it).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111"

                // ✅ ADD THIS LISTENER
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        super.onAdFailedToLoad(adError)
                        // Log the error
                        Log.e("AdMobBanner", "Ad failed to load: ${adError.message}")
                        Log.e("AdMobBanner", "Error Code: ${adError.code}")
                        Log.e("AdMobBanner", "Error Domain: ${adError.domain}")
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.i("AdMobBanner", "Ad loaded successfully!")
                    }
                }

                // Load the ad
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}