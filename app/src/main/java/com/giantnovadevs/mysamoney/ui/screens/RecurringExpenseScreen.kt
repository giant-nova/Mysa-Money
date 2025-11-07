package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.RecurringExpense
import com.giantnovadevs.mysamoney.ui.theme.Expense
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.RecurringExpenseViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val recurringVm: RecurringExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()

    val recurringExpenses by recurringVm.recurringExpenses.collectAsState()
    val categories by catVm.categories.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Bills") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // We'll create this screen next
                    navController.navigate("add_recurring_expense")
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring Expense")
            }
        }
    ) { padding ->
        Crossfade(
            targetState = recurringExpenses.isEmpty(),
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "Empty/List"
        ) { isEmpty ->
            if (isEmpty) {
                // Show the empty state message
                RecurringEmptyState()
            } else {
                // Show the list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recurringExpenses, key = { it.id }) { expense ->
                        val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
                        RecurringExpenseRow(
                            expense = expense,
                            categoryName = categoryName,
                            onDelete = { recurringVm.deleteRecurringExpense(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecurringEmptyState() {
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
                contentDescription = "No Subscriptions",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No subscriptions found",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Tap the '+' button to add your rent, bills, or other recurring payments.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecurringExpenseRow(
    expense: RecurringExpense,
    categoryName: String,
    onDelete: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val nextDueDate = dateFormatter.format(Date(expense.nextDueDate))
    val frequencyText = expense.frequency.name.lowercase().replaceFirstChar { it.titlecase() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon and Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Recurring")
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = expense.note ?: categoryName,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Amount and Frequency
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "₹${expense.amount}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Expense,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = frequencyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // Next Due Date
            Text(
                text = "Next payment: $nextDueDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}