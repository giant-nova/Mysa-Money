package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.viewmodel.BudgetViewModel
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val catVm: CategoryViewModel = viewModel()
    val budgetVm: BudgetViewModel = viewModel()

    val categories by catVm.categories.collectAsState()
    val budgets by budgetVm.budgetsForSelectedMonth.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Budgets") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                // This uses your dynamic theme colors
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                // Find the existing budget for this category, if any
                val budget = budgets.find { it.categoryId == category.id }

                BudgetRow(
                    category = category,
                    currentBudgetAmount = budget?.amount ?: 0.0,
                    onBudgetSet = { newAmount ->
                        budgetVm.setBudget(category.id, newAmount)
                    }
                )
            }
        }
    }
}

@Composable
private fun BudgetRow(
    category: Category,
    currentBudgetAmount: Double,
    onBudgetSet: (Double) -> Unit
) {
    val focusManager = LocalFocusManager.current

    // This text state is local to the row.
    // We format the initial amount to avoid "0.0"
    val initialAmount = if (currentBudgetAmount == 0.0) "" else currentBudgetAmount.toString()
    var textValue by remember(currentBudgetAmount) {
        mutableStateOf(initialAmount)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category Name
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Budget Amount Text Field
            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    // Allow only valid numbers
                    if (it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                        textValue = it
                    }
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done // Show "Done" button on keyboard
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        // When "Done" is pressed, save the budget
                        val newAmount = textValue.toDoubleOrNull() ?: 0.0
                        onBudgetSet(newAmount)
                        focusManager.clearFocus() // Hide the keyboard
                    }
                )
            )
        }
    }
}