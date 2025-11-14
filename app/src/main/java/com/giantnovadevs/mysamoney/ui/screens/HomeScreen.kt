package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.ui.components.ExpenseItem
import com.giantnovadevs.mysamoney.ui.theme.Expense as ExpenseColor // Use semantic colors
import com.giantnovadevs.mysamoney.ui.theme.Success as SuccessColor // Use semantic colors
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
import com.giantnovadevs.mysamoney.viewmodel.IncomeViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.DecimalFormat
import java.util.Calendar
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.lerp
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.formatter.ValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    // --- 1. Get All ViewModels & Data ---
    val expenseVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val incomeVm: IncomeViewModel = viewModel() // <-- New

    val expenses by expenseVm.expenses.collectAsState()
    val categories by catVm.categories.collectAsState()
    val dailySpending by expenseVm.dailySpendingLast7Days.collectAsState()

    // --- New Monthly Data ---
    val monthlyIncome by incomeVm.monthlyTotalIncome.collectAsState()
    val monthlyExpense by expenseVm.monthlyTotal.collectAsState()
    val monthlySurplus = monthlyIncome - monthlyExpense

    // Chart colors from theme
    val chartColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer
    ).map { it.toArgb() }
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val context = LocalContext.current

    // Create the permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied. We can show a snackbar/toast here if we want.
        }
    }

    // Check and request permission when the screen first launches
    LaunchedEffect(Unit) {
        // Only run this on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    Scaffold(
        // The Scaffold background is now the main background
        // Cards will "float" on top of this
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Mysa Money") },
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
                onClick = { navController.navigate("expense_entry?categoryId=null&expenseId=null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            // This is the key: Generous spacing between all card items
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {

            // --- Item 1: Monthly Surplus Card ---
            item {
                MonthlySurplusCard(
                    income = monthlyIncome,
                    expense = monthlyExpense,
                    surplus = monthlySurplus
                )
            }

            // --- Item 2: Quick Add Row ---
            item {
                QuickAddCardRow(
                    categories = categories,
                    onQuickAdd = { categoryId ->
                        navController.navigate("add?categoryId=$categoryId")
                    }
                )
            }

            item {
                ChartLazyRow(
                    expenses = expenses, // For the pie chart
                    categories = categories, // For the pie chart
                    dailySpending = dailySpending, // For the bar chart
                    chartColors = chartColors,
                    onSurfaceColor = onSurfaceColor,
                    onSurfaceVariantColor = onSurfaceVariantColor
                )
            }

            // --- Item 4: "Recent" Header ---
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // --- Item 5: List of Recent Expenses ---
            val recent = expenses.take(5)
            items(recent, key = { it.id }) { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown"
                // ExpenseItem is already a Card, so it fits perfectly
                ExpenseItem(
                    expense = expense,
                    categoryName = categoryName,
                    onClick = {
                        navController.navigate("expense_entry?expenseId=${expense.id}")
                    },
                    onDelete = { expenseVm.deleteExpense(expense) }
                )
            }
        }
    }
}

@Composable
private fun ChartLazyRow(
    expenses: List<Expense>,
    categories: List<Category>,
    dailySpending: Map<LocalDate, Double>,
    chartColors: List<Int>,
    onSurfaceColor: Int,
    onSurfaceVariantColor: Int
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 0.dp) // Handled by parent
    ) {
        // --- Chart 1: Pie Chart ---
        item {
            SpendingChartCard(
                expenses = expenses,
                categories = categories,
                chartColors = chartColors,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                modifier = Modifier
                    .width(320.dp)
                    .height(300.dp)
            )
        }

        // --- Chart 2: Bar Chart ---
        item {
            DailySpendingBarChart(
                data = dailySpending,
                chartColors = chartColors,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                modifier = Modifier
                    .width(320.dp)
                    .height(300.dp)
            )
        }

        // --- Chart 3: Heatmap ---
        item {
            DailySpendingHeatmap(
                data = dailySpending,
                modifier = Modifier
                    .width(320.dp)
                    .height(300.dp)
            )
        }
    }
}


/**
 * ✅ This is our NEW Bar Chart composable
 */
@Composable
private fun DailySpendingBarChart(
    data: Map<LocalDate, Double>,
    chartColors: List<Int>,
    onSurfaceColor: Int,
    onSurfaceVariantColor: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Daily Spending (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    BarChart(ctx).apply {
                        description.isEnabled = false
                        legend.isEnabled = false
                        // X-Axis setup (Day labels)
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.textColor = onSurfaceVariantColor
                        xAxis.granularity = 1f
                        // Y-Axis setup
                        axisLeft.textColor = onSurfaceVariantColor
                        axisLeft.setDrawGridLines(true)
                        axisLeft.axisMinimum = 0f
                        axisRight.isEnabled = false
                    }
                },
                update = { barChart ->
                    if (data.isEmpty()) {
                        barChart.clear()
                        return@AndroidView
                    }

                    // Get "Mon", "Tue", "Wed" labels
                    val dayFormatter = DateTimeFormatter.ofPattern("E")
                    val labels = data.keys.map { it.format(dayFormatter) }
                    barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)

                    // Create BarEntry list
                    val entries = data.values.mapIndexed { index, value ->
                        BarEntry(index.toFloat(), value.toFloat())
                    }

                    val dataSet = BarDataSet(entries, "Daily Spending")
                    dataSet.color = chartColors.first() // Use primary color
                    dataSet.valueTextColor = onSurfaceColor
                    dataSet.valueTextSize = 10f

                    val barData = BarData(dataSet)
                    barData.barWidth = 0.5f

                    barChart.data = barData
                    barChart.invalidate() // Refresh the chart
                }
            )
        }
    }
}
/**
 * The new "Monthly Surplus" Card
 */
@Composable
private fun MonthlySurplusCard(income: Double, expense: Double, surplus: Double) {
    val decimalFormat = remember { DecimalFormat("₹#,##0.00") }

    // Determine color for surplus
    val surplusColor = if (surplus >= 0) SuccessColor else ExpenseColor

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Row 1: Title and Main Surplus
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Surplus",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = decimalFormat.format(surplus),
                    style = MaterialTheme.typography.headlineSmall,
                    color = surplusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            // Divider
            Divider()

            // Row 2: Income vs Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Income Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = decimalFormat.format(income),
                        style = MaterialTheme.typography.titleLarge,
                        color = SuccessColor
                    )
                }

                // Expense Column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = decimalFormat.format(expense),
                        style = MaterialTheme.typography.titleLarge,
                        color = ExpenseColor
                    )
                }
            }
        }
    }
}

/**
 * The new "Quick Add" Row with Cards
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddCardRow(
    categories: List<Category>,
    onQuickAdd: (Int) -> Unit
) {
    val quickAddMapping = mapOf(
        "Food" to Icons.Default.Fastfood,
        "Transport" to Icons.Default.Train,
        "Bills" to Icons.Default.Receipt,
        "Rent" to Icons.Default.Home
    )
    val quickAddCategories = categories.filter { quickAddMapping.containsKey(it.name) }

    if (quickAddCategories.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Quick Add",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp) // Space before cards
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quickAddCategories, key = { it.id }) { category ->
                    val icon = quickAddMapping[category.name] ?: Icons.Default.Add

                    // This is the new "small sized card"
                    Card(
                        onClick = { onQuickAdd(category.id) },
                        modifier = Modifier
                            .width(90.dp)
                            .height(90.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, contentDescription = category.name, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(text = category.name, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Spending Chart Card (Unchanged, but now with consistent elevation)
 */
@Composable
private fun SpendingChartCard(
    expenses: List<Expense>,
    categories: List<Category>,
    chartColors: List<Int>,
    onSurfaceColor: Int,
    onSurfaceVariantColor: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Consistent elevation
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Spending (Last 7 Days)", // Added context
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PieChart(ctx).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        setUsePercentValues(true)
                        setDrawEntryLabels(false) // ✅ Hides labels inside slices
                        legend.isEnabled = false
                        legend.textColor = onSurfaceVariantColor
                    }
                },
                update = { view ->
                    val now = Calendar.getInstance()
                    now.set(Calendar.HOUR_OF_DAY, 0); now.set(Calendar.MINUTE, 0); now.set(Calendar.SECOND, 0); now.set(Calendar.MILLISECOND, 0)
                    val start = now.timeInMillis - 6 * 86_400_000L
                    val filtered = expenses.filter { it.date >= start }

                    val sums = mutableMapOf<Int, Double>()
                    filtered.forEach { e ->
                        sums[e.categoryId] = (sums[e.categoryId] ?: 0.0) + e.amount
                    }

                    val entries = sums.mapNotNull { (catId, sum) ->
                        val label = categories.find { it.id == catId }?.name ?: "Other"
                        PieEntry(sum.toFloat(), label)
                    }

                    val ds = PieDataSet(entries, "")
                    ds.sliceSpace = 2f
                    ds.valueTextSize = 12f
                    ds.colors = ColorTemplate.MATERIAL_COLORS.toList() +
                            ColorTemplate.VORDIPLOM_COLORS.toList() +
                            ColorTemplate.JOYFUL_COLORS.toList() +
                            ColorTemplate.COLORFUL_COLORS.toList() +
                            ColorTemplate.LIBERTY_COLORS.toList() +
                            ColorTemplate.PASTEL_COLORS.toList()
                    ds.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                    ds.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                    ds.valueLinePart1Length = 0.4f
                    ds.valueLinePart2Length = 0.8f
                    ds.valueLineColor = onSurfaceVariantColor
                    ds.valueTextColor = onSurfaceColor

                    val data = PieData(ds)
                    data.setValueFormatter(object : ValueFormatter() {
                        override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                            return pieEntry?.label ?: ""
                        }
                    })
                    view.data = data
                    view.invalidate()
                }
            )
        }
    }
}

/**
 * ✅ This is our NEW 7-Day Heatmap composable
 */
@Composable
private fun DailySpendingHeatmap(
    data: Map<LocalDate, Double>,
    modifier: Modifier = Modifier
) {
    // Find the max amount for scaling the color
    val maxAmount = data.values.maxOrNull() ?: 1.0
    // Get the theme's primary color for the "hot" state
    val hotColor = MaterialTheme.colorScheme.primary
    // Get the "cold" color
    val coldColor = MaterialTheme.colorScheme.surfaceVariant

    val dayFormatter = remember { DateTimeFormatter.ofPattern("E") }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Daily Spending Heatmap",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // A Row of 7 blocks for the 7 days
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (data.isEmpty()) {
                    Text("No data for this period.")
                } else {
                    data.entries.forEach { (date, amount) ->
                        // Calculate the "heat" (0.0f to 1.0f)
                        val fraction = (amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        // Find the color by blending cold and hot
                        val color = lerp(coldColor, hotColor, fraction)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = date.format(dayFormatter),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(color)
                            )
                        }
                    }
                }
            }
        }
    }
}