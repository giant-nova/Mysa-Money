package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.viewmodel.BudgetViewModel
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlin.math.absoluteValue

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)
private val SaveColor = Color(0xFF34C759) // Nice Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    onMenuClick: () -> Unit,
    proViewModel: ProViewModel
) {
    val catVm: CategoryViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()

    val categories by catVm.categories.collectAsState()
    val budgets by budgetVm.budgetsForSelectedMonth.collectAsState()
    val isPro by proViewModel.isProUser.collectAsState()

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Set Monthly Budgets",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
                val existingBudgetAmount = budgets.find { it.categoryId == category.id }?.amount ?: 0.0

                BudgetRow(
                    category = category,
                    initialAmount = existingBudgetAmount,
                    onSave = { newAmount ->
                        budgetVm.setBudget(category.id, newAmount)
                    }
                )

                if (!isPro && (index + 1) % 2 == 0) {
                    AdMobNative(modifier = Modifier.fillMaxWidth())
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BudgetRow(
    category: Category,
    initialAmount: Double,
    onSave: (Double) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val categoryColor = remember(category.name) { getCategoryColor(category.name) }

    val formattedInitial = remember(initialAmount) {
        if (initialAmount == 0.0) ""
        else if (initialAmount % 1.0 == 0.0) "%.0f".format(initialAmount)
        else "%.2f".format(initialAmount)
    }

    var textValue by remember { mutableStateOf(formattedInitial) }

    // Sync state only if initial load
    LaunchedEffect(formattedInitial) {
        if (textValue.isEmpty() && formattedInitial.isNotEmpty()) {
            textValue = formattedInitial
        }
    }

    val isChanged = textValue != formattedInitial

    val saveAction = {
        val newAmount = textValue.toDoubleOrNull() ?: 0.0
        onSave(newAmount)
        focusManager.clearFocus()
    }

    // --- Dynamic Width Calculation ---
    // This heuristic estimates the width needed based on character count
    // Base width 130dp + ~12dp per character over 4 chars
    val charCount = textValue.length
    val extraWidth = (charCount - 4).coerceAtLeast(0) * 12
    val dynamicWidth = (130 + extraWidth).dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 1. Fixed Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = categoryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 2. Flow Layout for Title and Input
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.Center
            ) {
                // Category Name
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .padding(end = 16.dp)
                        .align(Alignment.CenterVertically)
                )

                // Input Field
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        // ✅ LIMIT: Max 9 digits (before decimal)
                        // This prevents ambiguous/huge numbers that break UI logic
                        if (input.length <= 12) { // Rough total length limit
                            val parts = input.split(".")
                            val integerPart = parts.getOrNull(0) ?: ""
                            val decimalPart = parts.getOrNull(1)

                            // Check integer length limit (9 digits)
                            if (integerPart.length <= 9) {
                                // Validate generic number format
                                if (input.matches(Regex("^\\d*(\\.\\d{0,2})?\$"))) {
                                    textValue = input
                                }
                            }
                        }
                    },
                    placeholder = { Text("0") },
                    prefix = { Text("₹ ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AppBackground,
                        unfocusedContainerColor = AppBackground,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { saveAction() }
                    ),
                    // ✅ STRETCH LOGIC: Dynamic width based on text length
                    modifier = Modifier.width(dynamicWidth),
                    trailingIcon = {
                        if (isChanged) {
                            IconButton(
                                onClick = { saveAction() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save", tint = SaveColor)
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun getCategoryColor(name: String): Color {
    val hash = name.hashCode().absoluteValue
    val hue = (hash % 360).toFloat()
    return Color.hsl(hue, 0.65f, 0.45f)
}