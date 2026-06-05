package com.giantnovadevs.mysamoney.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.ads.AdManager
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlinx.coroutines.delay
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)
private val DeleteColor = Color(0xFFFF3B30) // Standard Red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val expVm: ExpenseViewModel = viewModel()
    val expenses by expVm.expenses.collectAsState()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()

    val context = LocalContext.current
    val adManager = remember(context) { AdManager(context) }
    var showExportDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // --- Export Logic ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) Toast.makeText(context, "Permission granted. Tap export again.", Toast.LENGTH_SHORT).show()
        else Toast.makeText(context, "Permission denied.", Toast.LENGTH_SHORT).show()
    }

    val doExport = { format: String ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            expVm.exportExpenses(format) { success, message ->
                Toast.makeText(context, if (success) message else "Failed: $message", Toast.LENGTH_LONG).show()
            }
        } else {
            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val onExportRequest = { format: String ->
        showExportDialog = false
        doExport(format)
    }

    val onExportIconClick = {
        if (isPro) {
            showExportDialog = true
        } else {
            val activity = context as? Activity
            if (activity != null) {
                adManager.showRewardedAd(
                    activity = activity,
                    onAdNotAvailable = { showExportDialog = true },
                    onRewardEarned = { showExportDialog = true }
                )
            } else {
                showExportDialog = true
            }
        }
    }

    LaunchedEffect(isPro) {
        if (!isPro) {
            adManager.loadRewardedAd()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "All Expenses",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { onExportIconClick() }) {
                        Icon(Icons.Default.IosShare, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("expense_entry?categoryId=null&expenseId=null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        },
    ) { padding ->
        Crossfade(
            targetState = expenses.isEmpty(),
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "Empty/List"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(expenses, key = { _, expense -> expense.id }) { index, expense ->
                        val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "General"

                        // --- SWIPE TO DELETE LOGIC ---
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    expenseToDelete = expense
                                    false
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { DeleteBackground(dismissState) },
                            content = {
                                ExpenseListTile(
                                    expense = expense,
                                    categoryName = categoryName,
                                    onClick = { navController.navigate("expense_entry?expenseId=${expense.id}") }
                                )
                            },
                            enableDismissFromStartToEnd = false
                        )

                        if (!isPro && (index + 1) % 3 == 0) {
                            AdMobBanner(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }

    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense?") },
            text = { Text("Are you sure you want to remove this expense? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { expVm.deleteExpense(it) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format -> onExportRequest(format) }
        )
    }
}

/**
 * The Red Background that appears when swiping
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        DeleteColor
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp)) // Match the card shape
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = Color.White
        )
    }
}

@Composable
fun ExpenseListTile(
    expense: Expense,
    categoryName: String,
    onClick: () -> Unit
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val dateString = remember(expense.date) {
        val date = java.util.Date(expense.date)
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AppBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = expense.note?.ifBlank { categoryName } ?: "",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "-${decimalFormat.format(expense.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ... (Rest of the file: EmptyState, ExportOptionsDialog, BannerAd remain unchanged) ...
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No expenses yet",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your list is clean! Add a new expense from the home screen to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
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
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Export Data", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportDialogRow(
                    text = "CSV (Spreadsheet)",
                    icon = Icons.Default.TableView,
                    onClick = { onExport("CSV") }
                )
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                ExportDialogRow(
                    text = "PDF Document",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = { onExport("PDF") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun ExportDialogRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}
