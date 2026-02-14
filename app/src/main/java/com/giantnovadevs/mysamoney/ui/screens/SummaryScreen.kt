package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.CategoryTotal
import com.giantnovadevs.mysamoney.viewmodel.BudgetViewModel
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
import java.text.DecimalFormat
import kotlin.math.absoluteValue

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val catVm: CategoryViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()
    val expenseVm: ExpenseViewModel = viewModel()

    // Data collection
    val categories by catVm.categories.collectAsState()
    val budgets by budgetVm.budgetsForSelectedMonth.collectAsState()
    val categoryTotals by expenseVm.categoryTotalsForSelectedMonth.collectAsState()

    // Using a derived state for the month display string
    val monthDisplay = expenseVm.getMonthYearDisplay()

    // Tab State
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Budgets", "Spending")

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Monthly Report",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            // 1. Month Switcher
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

            Spacer(Modifier.height(8.dp))

            // 2. Clean Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AppBackground,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                divider = { HorizontalDivider(color = CardBorder) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if(selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // 3. Content
            Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
                when (selectedTabIndex) {
                    0 -> BudgetProgressTab(categories, budgets, categoryTotals)
                    1 -> AllSpendingTab(categoryTotals, categories)
                }
            }
        }
    }
}

// --- TAB 1: BUDGET PROGRESS ---
@Composable
private fun BudgetProgressTab(
    categories: List<Category>,
    budgets: List<com.giantnovadevs.mysamoney.data.Budget>,
    categoryTotals: List<CategoryTotal>
) {
    val budgetedCategories = categories.filter { category ->
        budgets.any { it.categoryId == category.id }
    }

    if (budgetedCategories.isEmpty()) {
        EmptySummaryState(message = "No budgets set for this month.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(budgetedCategories, key = { it.id }) { category ->
                val budget = budgets.find { it.categoryId == category.id }!!
                val totalSpent = categoryTotals.find { it.categoryId == category.id }?.total ?: 0.0

                BudgetProgressCard(
                    category = category,
                    totalSpent = totalSpent,
                    budgetAmount = budget.amount
                )
            }
        }
    }
}

// --- TAB 2: ALL SPENDING ---
@Composable
private fun AllSpendingTab(
    categoryTotals: List<CategoryTotal>,
    categories: List<Category>
) {
    // 1. Sort by highest spending
    val sortedTotals = categoryTotals.sortedByDescending { it.total }

    // 2. Calculate Context Data
    val grandTotal = sortedTotals.sumOf { it.total }
    val maxCategoryTotal = sortedTotals.maxOfOrNull { it.total } ?: 1.0

    if (sortedTotals.isEmpty()) {
        EmptySummaryState(message = "No spending recorded this month.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Grand Total
            item {
                TotalSpendingHeader(grandTotal)
            }

            // List of Big Aesthetic Cards
            items(sortedTotals, key = { it.categoryId }) { total ->
                val category = categories.find { it.id == total.categoryId }
                val categoryName = category?.name ?: "Unknown"

                // Calculate stats
                val percentageOfTotal = (total.total / grandTotal).toFloat()
                val relativeToMax = (total.total / maxCategoryTotal).toFloat()

                AestheticSpendingCard(
                    categoryName = categoryName,
                    amount = total.total,
                    percentage = percentageOfTotal,
                    progress = relativeToMax
                )
            }

            // Bottom spacer
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

// --- NEW COMPONENT: Total Spending Header ---
@Composable
private fun TotalSpendingHeader(total: Double) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Total Spending",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Text(
            text = decimalFormat.format(total),
            style = MaterialTheme.typography.displayMedium, // Very Big Text
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// --- NEW COMPONENT: Aesthetic Spending Card ---
@Composable
private fun AestheticSpendingCard(
    categoryName: String,
    amount: Double,
    percentage: Float,
    progress: Float
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    // Generate a consistent pastel color for this category
    val categoryColor = remember(categoryName) { getCategoryColor(categoryName) }

    // Animation for the bar
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Row 1: Icon, Name, and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Big Colorful Icon Box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(categoryColor.copy(alpha = 0.2f)), // Pastel background
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = categoryName.take(1).uppercase(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = categoryColor, // Darker text of same hue
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Name and Amount
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = decimalFormat.format(amount),
                        style = MaterialTheme.typography.headlineSmall, // Bigger Amount
                        color = TextPrimary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Row 2: Progress Bar & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // The visual bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(AppBackground) // Track color
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress) // Fill based on relative spending
                            .clip(RoundedCornerShape(50))
                            .background(categoryColor)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // The percentage text
                Text(
                    text = "${(percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- HELPER: Deterministic Pastel Color Generator ---
private fun getCategoryColor(name: String): Color {
    val hash = name.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    // Saturation 0.65, Lightness 0.55 gives a nice vibrant but not neon look
    return Color.hsl(hue, 0.65f, 0.45f)
}

// --- COMPONENT: Budget Progress Card ---
@Composable
private fun BudgetProgressCard(
    category: Category,
    totalSpent: Double,
    budgetAmount: Double
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }

    // Logic for colors
    val isOverBudget = totalSpent > budgetAmount

    // ✅ CRASH FIX: Guard against division by zero (NaN)
    val progress = if (budgetAmount > 0) {
        (totalSpent / budgetAmount).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "BudgetProgress"
    )

    val statusColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header: Icon + Name + Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AppBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                // Percentage Text
                val percent = (progress * 100).toInt()
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelLarge,
                    color = if(isOverBudget) statusColor else TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)), // Fully rounded caps
                color = statusColor,
                trackColor = AppBackground,
                strokeCap = StrokeCap.Round
            )

            Spacer(Modifier.height(12.dp))

            // Footer: Spent vs Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = decimalFormat.format(totalSpent),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if(isOverBudget) statusColor else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Budget", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = decimalFormat.format(budgetAmount),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// --- COMPONENT: All Spending Tile ---
@Composable
private fun AllSpendingTile(categoryName: String, amount: Double) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Small dot indicator
                Box(
                    modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = decimalFormat.format(amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

// --- COMPONENT: Month Switcher ---
@Composable
private fun MonthSwitcher(
    monthDisplay: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrev,
            colors = IconButtonDefaults.iconButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = monthDisplay,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.width(16.dp))

        IconButton(
            onClick = onNext,
            colors = IconButtonDefaults.iconButtonColors(contentColor = TextSecondary)
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
private fun EmptySummaryState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}