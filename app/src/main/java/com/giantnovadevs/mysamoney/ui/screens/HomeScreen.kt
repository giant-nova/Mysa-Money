package com.giantnovadevs.mysamoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.data.RecurringExpense
import com.giantnovadevs.mysamoney.ui.components.ExpenseItem
import com.giantnovadevs.mysamoney.ui.theme.Expense as ExpenseColor
import com.giantnovadevs.mysamoney.ui.theme.Success as SuccessColor
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
import com.giantnovadevs.mysamoney.viewmodel.FinancialCoachViewModel
import com.giantnovadevs.mysamoney.viewmodel.IncomeViewModel
import com.giantnovadevs.mysamoney.viewmodel.RecurringExpenseViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit

// --- Custom Colors for the Grey-White Theme ---
private val AppBackground = Color(0xFFF6F7F9) // Cool light grey
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB) // Very subtle border
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    financialCoachViewModel: FinancialCoachViewModel
) {
    // --- ViewModels & Data ---
    val expenseVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val incomeVm: IncomeViewModel = viewModel()
    val recurringVm: RecurringExpenseViewModel = viewModel()

    val upcomingBills by recurringVm.recurringExpenses.collectAsState()
    val expenses by expenseVm.expenses.collectAsState()
    val categories by catVm.categories.collectAsState()
    val dailySpending by expenseVm.dailySpendingLast7Days.collectAsState()

    val monthlyIncome by incomeVm.monthlyTotalIncome.collectAsState()
    val monthlyExpense by expenseVm.monthlyTotal.collectAsState()
    val monthlySurplus = monthlyIncome - monthlyExpense

    val insight by financialCoachViewModel.dashboardInsight.collectAsState()
    val isInsightLoading by financialCoachViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { financialCoachViewModel.getDashboardInsight() }

    // Colors & Context
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    ).map { it.toArgb() }

    val context = LocalContext.current

    // Permission Logic
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        containerColor = AppBackground, // Set the clean grey background
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mysa Money",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground, // Blend with background
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("expense_entry?categoryId=null&expenseId=null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape, // Modern circle FAB
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp), // Increased horizontal padding
            verticalArrangement = Arrangement.spacedBy(20.dp) // Airy spacing between cards
        ) {

            // 1. AI Insight
            item {
                InsightCard(
                    insight = insight,
                    isLoading = isInsightLoading,
                    onClick = { financialCoachViewModel.getDashboardInsight() }
                )
            }

            // 2. Main Monthly Stats
            item {
                MonthlySurplusCard(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    surplus = monthlySurplus
                )
            }

            // 3. Upcoming Bills
            if (upcomingBills.isNotEmpty()) {
                item {
                    UpcomingBillsCard(
                        upcomingBills = upcomingBills.take(3),
                        categories = categories
                    )
                }
            }

            // 4. Charts (Horizontal Scroll)
            item {
                ChartLazyRow(
                    expenses = expenses,
                    categories = categories,
                    dailySpending = dailySpending,
                    chartColors = chartColors
                )
            }

            // 5. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(expenses.take(5), key = { it.id }) { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
                ExpenseItem(
                    expense = expense,
                    categoryName = categoryName,
                    onClick = { navController.navigate("expense_entry?expenseId=${expense.id}") },
                    onDelete = { expenseVm.deleteExpense(expense) }
                )
            }

            // Bottom spacer to avoid FAB overlap
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// --- REUSABLE CLEAN CARD COMPONENT ---
@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), // Softer corners
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder), // Subtle border definition
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Flat look
    ) {
        Column(
            modifier = Modifier.padding(24.dp), // Generous internal padding
            content = content
        )
    }
}

@Composable
private fun InsightCard(
    insight: String?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = !isLoading),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) // Very light tint
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = "Financial Insight",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                if (isLoading) {
                    Text("Analyzing your finances...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                } else {
                    Text(
                        text = insight ?: "Tap to generate insights.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlySurplusCard(income: Double, expense: Double, surplus: Double) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") } // Removed decimals for cleaner look

    DashboardCard {
        Text(
            text = "Total Balance",
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = decimalFormat.format(surplus),
            style = MaterialTheme.typography.displaySmall, // Big Hero Text
            color = if (surplus >= 0) TextPrimary else ExpenseColor,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(SuccessColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Income", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = decimalFormat.format(income),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Vertical Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(CardBorder)
            )

            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Expense", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.size(8.dp).background(ExpenseColor, CircleShape))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = decimalFormat.format(expense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun UpcomingBillsCard(
    upcomingBills: List<RecurringExpense>,
    categories: List<Category>
) {
    DashboardCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Upcoming Bills",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            upcomingBills.forEach { bill ->
                val categoryName = categories.find { it.id == bill.categoryId }?.name ?: "General"
                UpcomingBillRow(bill = bill, categoryName = categoryName)
            }
        }
    }
}

@Composable
private fun UpcomingBillRow(bill: RecurringExpense, categoryName: String) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val todayMillis = System.currentTimeMillis()
    val diffMillis = bill.nextDueDate - todayMillis
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(diffMillis)

    val (timeText, timeColor) = when {
        daysRemaining < 0 -> "Overdue" to ExpenseColor
        daysRemaining == 0L -> "Today" to ExpenseColor
        daysRemaining == 1L -> "Tomorrow" to MaterialTheme.colorScheme.primary
        else -> "in $daysRemaining days" to TextSecondary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Category Dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = categoryName.take(1),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = bill.note ?: categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = timeColor
                )
            }
        }
        Text(
            text = decimalFormat.format(bill.amount),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

@Composable
private fun ChartLazyRow(
    expenses: List<Expense>,
    categories: List<Category>,
    dailySpending: Map<LocalDate, Double>,
    chartColors: List<Int>
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        item {
            ChartCardContainer(title = "Spending Share") {
                SpendingPieChart(expenses, categories, chartColors)
            }
        }
        item {
            ChartCardContainer(title = "Last 7 Days") {
                DailySpendingBarChart(dailySpending, chartColors)
            }
        }
        item {
            ChartCardContainer(title = "Intensity Map") {
                DailySpendingHeatmap(dailySpending)
            }
        }
    }
}

@Composable
fun ChartCardContainer(
    title: String,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .width(300.dp)
            .height(280.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

// --- CHARTS (Visual Clean Up) ---

@Composable
private fun SpendingPieChart(
    expenses: List<Expense>,
    categories: List<Category>,
    chartColors: List<Int>
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.WHITE) // Match card background
                setTransparentCircleAlpha(0)
                holeRadius = 65f // Thinner ring looks more modern
                setUsePercentValues(false)
                setDrawEntryLabels(false)
                legend.isEnabled = false
            }
        },
        update = { view ->
            // Logic remains same, just ensuring colors match
            val sums = expenses.groupBy { it.categoryId }
                .mapValues { it.value.sumOf { e -> e.amount } }

            val entries = sums.map { (catId, sum) ->
                PieEntry(sum.toFloat(), categories.find { it.id == catId }?.name ?: "")
            }

            val ds = PieDataSet(entries, "").apply {
                colors = chartColors
                sliceSpace = 3f
                setDrawValues(false) // Clean look: no numbers on chart
            }
            view.data = PieData(ds)
            view.invalidate()
        }
    )
}

@Composable
private fun DailySpendingBarChart(
    data: Map<LocalDate, Double>,
    chartColors: List<Int>
) {
    val primaryColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)

                // Clean X Axis
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.setDrawAxisLine(false)
                xAxis.textColor = android.graphics.Color.parseColor("#72777F")
                xAxis.textSize = 10f
                xAxis.granularity = 1f

                // Clean Y Axis
                axisLeft.setDrawAxisLine(false)
                axisLeft.setDrawGridLines(true) // Keep horizontal grid for readability
                axisLeft.gridColor = android.graphics.Color.parseColor("#F0F0F0")
                axisLeft.textColor = android.graphics.Color.parseColor("#72777F")
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            if (data.isNotEmpty()) {
                val dayFormatter = DateTimeFormatter.ofPattern("E")
                val labels = data.keys.map { it.format(dayFormatter) }
                chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                val entries = data.values.mapIndexed { i, v -> BarEntry(i.toFloat(), v.toFloat()) }
                val ds = BarDataSet(entries, "").apply {
                    color = primaryColor
                    setDrawValues(false) // Clean look
                }

                chart.data = BarData(ds).apply { barWidth = 0.4f }
                chart.invalidate()
            }
        }
    )
}

@Composable
private fun DailySpendingHeatmap(data: Map<LocalDate, Double>) {
    val maxAmount = data.values.maxOrNull() ?: 1.0
    val hotColor = MaterialTheme.colorScheme.primary
    val coldColor = MaterialTheme.colorScheme.surfaceVariant
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (data.isEmpty()) {
            Text("No recent data", color = TextSecondary)
        } else {
            data.entries.forEach { (date, amount) ->
                val fraction = (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                val color = lerp(coldColor, hotColor, fraction)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pill shape for heatmap
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(60.dp) // Taller, thinner bars
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.2f)) // Track
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(fraction) // Fill based on amount
                                .align(Alignment.BottomCenter)
                                .background(color)
                        )
                    }
                    Text(
                        text = date.format(dayFormatter).take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}