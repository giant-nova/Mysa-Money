package com.giantnovadevs.mysamoney.ui.screens

import android.os.Build
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val expVm: ExpenseViewModel = viewModel()
    val expenses by expVm.expenses.collectAsState()
    val context = LocalContext.current
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted, but we don't need to do anything here
            // The user will just click the button again
            Toast.makeText(context, "Permission granted. Please tap export again.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permission denied. Cannot export file.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- This is the main function to call when an export button is tapped ---
    val onExportRequest = { format: String ->
        showExportDialog = false

        // This is the core logic
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // --- Android 10+ ---
            // No permission needed. Just export.
            expVm.exportExpenses(format) { success, message ->
                if (success) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed: $message", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // --- Android 9 and below ---
            // We MUST check for permission first.
            when (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted. Just export.
                    expVm.exportExpenses(format) { success, message ->
                        if (success) {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Export failed: $message", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                else -> {
                    // Permission is not granted. Launch the pop-up.
                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Expenses") },
                actions = {
                    IconButton(onClick = {
                        showExportDialog = true
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
            if(!isPro) {
                BannerAd()
            }
        }
    ) { padding ->
        // ✅ Use Crossfade to animate between empty and list states
        Crossfade(
            targetState = expenses.isEmpty(),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
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
                            onClick = {
                                navController.navigate("expense_entry?expenseId=${expense.id}")
                            },
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }
        }
    }
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = {
                    format ->
                onExportRequest(format)
            }
        )
    }
}

@Composable
private fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export As") },
        text = {
            // Use a LazyColumn in case we add more options
            LazyColumn {
                item {
                    ExportDialogRow(
                        text = "CSV (Comma-separated values)",
                        icon = Icons.Default.UploadFile,
                        onClick = { onExport("CSV") }
                    )
                }
                item {
                    ExportDialogRow(
                        text = "Excel (.xls) (Coming Soon)",
                        icon = Icons.Default.TableView,
                        onClick = {
                            Toast.makeText(context, "Excel export is not yet supported.", Toast.LENGTH_SHORT).show()
                        },
                        enabled = false
                    )
                }
                item {
                    ExportDialogRow(
                        text = "PDF (.pdf)",
                        icon = Icons.Default.PictureAsPdf,
                        onClick = { onExport("PDF") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * ✅ NEW: A helper composable for a single row in the dialog
 */
@Composable
private fun ExportDialogRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
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