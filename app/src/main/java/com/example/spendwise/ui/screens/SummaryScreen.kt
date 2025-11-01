package com.example.spendwise.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.spendwise.data.Category
import com.example.spendwise.data.CategoryTotal
import com.example.spendwise.ui.theme.Expense
import com.example.spendwise.viewmodel.BudgetViewModel
import com.example.spendwise.viewmodel.CategoryViewModel
import com.example.spendwise.viewmodel.ExpenseViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val catVm: CategoryViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()
    val expenseVm: ExpenseViewModel = viewModel()

    // Collect all the data we need
    val categories by catVm.categories.collectAsState()
    val budgets by budgetVm.budgetsForSelectedMonth.collectAsState()
    val categoryTotals by expenseVm.categoryTotalsForSelectedMonth.collectAsState()

    val monthDisplay by remember(expenseVm.selectedMonthYear.collectAsState().value) {
        derivedStateOf { expenseVm.getMonthYearDisplay() }
    }

    // State for which tab is selected
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Budget Progress", "All Spending")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monthly Report") },
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
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // --- 1. Month Switcher UI ---
            MonthSwitcher(
                monthDisplay = monthDisplay,
                onPrev = {
                    expenseVm.prevMonth()
                    budgetVm.prevMonth()
                },
                onNext = {
                    expenseVm.nextMonth()
                    budgetVm.nextMonth()
                }
            )

            // --- 2. TabRow ---
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 3. Content Area (Budget or All Spending) ---
            when (selectedTabIndex) {
                0 -> BudgetProgressTab(categories, budgets, categoryTotals)
                1 -> AllSpendingTab(categoryTotals, categories)
            }
        }
    }
}

/**
 * Tab 1: Budget Progress (Our existing list)
 */
@Composable
private fun BudgetProgressTab(
    categories: List<Category>,
    budgets: List<com.example.spendwise.data.Budget>,
    categoryTotals: List<CategoryTotal>
) {
    val budgetedCategories = categories.filter { category ->
        budgets.any { it.categoryId == category.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (budgetedCategories.isEmpty()) {
            item {
                Text(
                    text = "You haven't set any budgets for this month. Go to the 'Budgets' screen to get started.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(budgetedCategories, key = { it.id }) { category ->
                val budget = budgets.find { it.categoryId == category.id }!!
                val totalSpent = categoryTotals.find { it.categoryId == category.id }?.total ?: 0.0

                BudgetProgressItem(
                    category = category,
                    totalSpent = totalSpent,
                    budgetAmount = budget.amount
                )
            }
        }
    }
}

/**
 * Tab 2: All Spending (Our new list)
 */
@Composable
private fun AllSpendingTab(
    categoryTotals: List<CategoryTotal>,
    categories: List<Category>
) {
    // Sort the list from highest spending to lowest
    val sortedTotals = categoryTotals.sortedByDescending { it.total }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (sortedTotals.isEmpty()) {
            item {
                Text(
                    text = "No spending for this month.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sortedTotals, key = { it.categoryId }) { total ->
                val category = categories.find { it.id == total.categoryId }
                AllSpendingRow(
                    categoryName = category?.name ?: "Unknown",
                    amount = total.total
                )
            }
        }
    }
}

/**
 * A simple row for the "All Spending" tab
 */
@Composable
private fun AllSpendingRow(categoryName: String, amount: Double) {
    val decimalFormat = remember { DecimalFormat("₹#,##0.00") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = decimalFormat.format(amount),
                style = MaterialTheme.typography.titleMedium,
                color = Expense
            )
        }
    }
}


// --- Composables from before (MonthSwitcher, BudgetProgressItem, etc.) ---
// --- No changes are needed to these ---

@Composable
private fun MonthSwitcher(
    monthDisplay: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Previous Month")
        }
        Text(
            text = monthDisplay,
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ArrowForwardIos, contentDescription = "Next Month")
        }
    }
}

@Composable
private fun BudgetProgressItem(
    category: Category,
    totalSpent: Double,
    budgetAmount: Double
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0.00") }
    val progress = (totalSpent / budgetAmount).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "BudgetProgress"
    )
    val progressColor = if (totalSpent > budgetAmount) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
    val spentColor = if (totalSpent > budgetAmount) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = decimalFormat.format(totalSpent),
                    style = MaterialTheme.typography.titleMedium,
                    color = spentColor
                )
                Text(
                    text = "of ${decimalFormat.format(budgetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// NOTE: The Pie Chart Composable is no longer needed in this file,
// as it's only used on the HomeScreen now.