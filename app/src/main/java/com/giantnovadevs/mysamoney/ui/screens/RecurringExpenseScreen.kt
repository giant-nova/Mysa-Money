package com.giantnovadevs.mysamoney.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.RecurringExpense
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import com.giantnovadevs.mysamoney.viewmodel.RecurringExpenseViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)
private val DeleteColor = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val recurringVm: RecurringExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()

    val recurringExpenses by recurringVm.recurringExpenses.collectAsState()
    val categories by catVm.categories.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()

    // State for delete confirmation
    var expenseToDelete by remember { mutableStateOf<RecurringExpense?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Subscriptions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = TextPrimary
                        )
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
                onClick = { navController.navigate("recurring_expense_entry?id=null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        }
    ) { padding ->
        Crossfade(
            targetState = recurringExpenses.isEmpty(),
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "Empty/List"
        ) { isEmpty ->
            if (isEmpty) {
                RecurringEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(recurringExpenses, key = { _, expense -> expense.id }) { index, expense ->
                        val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "General"

                        // --- SWIPE TO DELETE ---
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    expenseToDelete = expense
                                    false // Snap back, show dialog
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { RecurringDeleteBackground(dismissState) },
                            content = {
                                RecurringExpenseTile(
                                    expense = expense,
                                    categoryName = categoryName,
                                    onClick = { navController.navigate("recurring_expense_entry?id=${expense.id}") }
                                )
                            },
                            enableDismissFromStartToEnd = false
                        )

                        if (!isPro && (index + 1) % 3 == 0) {
                            AdMobNative(modifier = Modifier.fillMaxWidth())
                        }
                    }

                    if (!isPro) {
                        item {
                            AdMobNative(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                        }
                    }

                    // Spacer for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Subscription?") },
            text = { Text("Are you sure you want to stop tracking this recurring expense? Future reminders will be cancelled.") },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { recurringVm.deleteRecurringExpense(it) }
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
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * Clean Tile for Recurring Expenses
 */
@Composable
private fun RecurringExpenseTile(
    expense: RecurringExpense,
    categoryName: String,
    onClick: () -> Unit
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val nextDueDate = dateFormatter.format(Date(expense.nextDueDate))
    // Capitalize frequency (e.g., "Monthly")
    val frequencyText = expense.frequency.name.lowercase().replaceFirstChar { it.titlecase() }

    // Icon Color
    val categoryColor = remember(categoryName) { getCategoryColor(categoryName) }

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
                // Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(categoryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventRepeat, // Recurring icon
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = expense.note ?: categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$frequencyText • Next: $nextDueDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = decimalFormat.format(expense.amount),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Swipe Background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        DeleteColor
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
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
private fun RecurringEmptyState() {
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
            text = "No Subscriptions",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to track rent, bills, or recurring payments.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// Helper for colors
private fun getCategoryColor(name: String): Color {
    val hash = name.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    return Color.hsl(hue, 0.65f, 0.45f)
}