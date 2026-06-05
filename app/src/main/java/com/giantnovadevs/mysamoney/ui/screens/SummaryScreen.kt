package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val catVm: CategoryViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()
    val expenseVm: ExpenseViewModel = viewModel()
    val isPro by proViewModel.isProUser.collectAsState()

    // Data collection
    val categories by catVm.categories.collectAsState()
    val budgets by budgetVm.budgetsForSelectedMonth.collectAsState()
    val categoryTotals by expenseVm.categoryTotalsForSelectedMonth.collectAsState()

    // ✅ FIX: specific fix for the type mismatch
    val selectedMonthString by expenseVm.selectedMonthYear.collectAsState()

    // Parse the String ("yyyy-MM") to YearMonth object safely
    val selectedYearMonth = remember(selectedMonthString) {
        try {
            YearMonth.parse(selectedMonthString)
        } catch (e: Exception) {
            YearMonth.now() // Fallback
        }
    }

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
                    0 -> BudgetProgressTab(categories, budgets, categoryTotals, isPro)
                    // ✅ FIX: Now passing the parsed YearMonth object
                    1 -> AllSpendingTab(categoryTotals, categories, selectedYearMonth)
                }
            }
        }
    }
}

// --- TAB 2: SPENDING ANALYSIS ---
@Composable
private fun AllSpendingTab(
    categoryTotals: List<CategoryTotal>,
    categories: List<Category>,
    selectedYearMonth: YearMonth
) {
    // 1. Sort by highest spending
    val sortedTotals = categoryTotals.sortedByDescending { it.total }

    // 2. Calculate Context Data
    val grandTotal = sortedTotals.sumOf { it.total }

    // Guard against division by zero if grandTotal is 0
    val safeGrandTotal = if (grandTotal > 0) grandTotal else 1.0

    // 3. Calculate Daily Average
    val today = LocalDate.now()
    val isCurrentMonth = selectedYearMonth.year == today.year && selectedYearMonth.month == today.month
    val daysPassed = if (isCurrentMonth) today.dayOfMonth else selectedYearMonth.lengthOfMonth()
    val dailyAverage = if (daysPassed > 0) grandTotal / daysPassed else 0.0

    // 4. Find Top Spender
    val topSpender = sortedTotals.firstOrNull()
    val topSpenderName = topSpender?.let { t -> categories.find { it.id == t.categoryId }?.name } ?: "None"

    if (sortedTotals.isEmpty()) {
        EmptySummaryState(message = "No spending recorded this month.")
    } else {
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- SECTION 1: SPENDING INSIGHTS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InsightTile(
                        title = "Daily Avg",
                        value = dailyAverage,
                        icon = Icons.Default.Timeline,
                        color = Color(0xFF5E5CE6),
                        modifier = Modifier.weight(1f)
                    )

                    InsightTile(
                        title = "Top Spend",
                        subtitle = topSpenderName,
                        value = topSpender?.total ?: 0.0,
                        icon = Icons.Default.Star,
                        color = Color(0xFFFF9F0A),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- SECTION 2: SPENDING BREAKDOWN ---
            item {
                Text(
                    text = "Spending Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            items(sortedTotals, key = { it.categoryId }) { total ->
                val category = categories.find { it.id == total.categoryId }
                val categoryName = category?.name ?: "Unknown"

                // ✅ FIX: Percentage is now calculated against the SAFE grand total
                val percentageOfTotal = (total.total / safeGrandTotal).toFloat()

                AestheticSpendingCard(
                    categoryName = categoryName,
                    amount = total.total,
                    percentage = percentageOfTotal,
                    // ✅ FIX: The progress bar now represents the ACTUAL percentage share, not relative scaling
                    progress = percentageOfTotal
                )
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun InsightTile(
    title: String,
    value: Double,
    subtitle: String? = null,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }

    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            }

            Column {
                Text(
                    text = decimalFormat.format(value),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = color,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun AestheticSpendingCard(
    categoryName: String,
    amount: Double,
    percentage: Float,
    progress: Float
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val categoryColor = remember(categoryName) { getCategoryColor(categoryName) }

    // Animate the bar
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
        Column(modifier = Modifier.padding(20.dp)) {
            // Row 1: Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Icon + Name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(categoryColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = categoryName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        // Shows "40% of total"
                        Text(
                            text = "${(percentage * 100).toInt()}% of total",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                // Right: Amount
                Text(
                    text = decimalFormat.format(amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2: Visual Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp) // Slightly thicker for better visibility
                    .clip(RoundedCornerShape(50))
                    .background(AppBackground)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress) // Now matches the percentage text exactly
                        .clip(RoundedCornerShape(50))
                        .background(categoryColor)
                )
            }
        }
    }
}

// --- HELPER: Deterministic Pastel Color Generator ---
private fun getCategoryColor(name: String): Color {
    val hash = name.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    return Color.hsl(hue, 0.65f, 0.45f)
}

// --- TAB 1: BUDGET PROGRESS (Kept Simple) ---
@Composable
private fun BudgetProgressTab(
    categories: List<Category>,
    budgets: List<com.giantnovadevs.mysamoney.data.Budget>,
    categoryTotals: List<CategoryTotal>,
    isPro: Boolean
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
            itemsIndexed(budgetedCategories, key = { _, category -> category.id }) { index, category ->
                val budget = budgets.find { it.categoryId == category.id }!!
                val totalSpent = categoryTotals.find { it.categoryId == category.id }?.total ?: 0.0

                BudgetProgressCard(category, totalSpent, budget.amount)

                if (!isPro && (index + 1) % 3 == 0) {
                    AdMobBanner(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                }
            }
        }
    }
}

// --- COMPONENT: Standard Budget Card ---
@Composable
private fun BudgetProgressCard(
    category: Category,
    totalSpent: Double,
    budgetAmount: Double
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val isOverBudget = totalSpent > budgetAmount
    val progress = if (budgetAmount > 0) (totalSpent / budgetAmount).toFloat().coerceIn(0f, 1f) else 0f

    // Status Logic
    val statusColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val statusText = if (isOverBudget) "Over Limit" else "On Track"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Badge(containerColor = statusColor.copy(alpha = 0.1f), contentColor = statusColor) {
                    Text(statusText)
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = statusColor,
                trackColor = AppBackground
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${(progress*100).toInt()}% Used", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text(
                    "${decimalFormat.format(budgetAmount - totalSpent)} left",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

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
        IconButton(onClick = onPrev) { Icon(Icons.Default.ChevronLeft, contentDescription = "Prev") }
        Spacer(modifier = Modifier.width(16.dp))
        Text(monthDisplay, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.width(16.dp))
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, contentDescription = "Next") }
    }
}

@Composable
private fun EmptySummaryState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}