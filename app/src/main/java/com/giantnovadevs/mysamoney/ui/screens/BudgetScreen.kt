package com.giantnovadevs.mysamoney.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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

    val navToHome: () -> Unit = {
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Budgets") },
                navigationIcon = {
                    IconButton(onClick = { navToHome() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories, key = { it.id }) { category ->
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

    // --- ✅ ITEM #8 FIX: Format the initial amount ---
    val initialAmount = if (currentBudgetAmount == 0.0) {
        ""
    } else if (currentBudgetAmount % 1 == 0.0) {
        // It's a whole number, format as "1000"
        "%.0f".format(currentBudgetAmount)
    } else {
        // It's a decimal, format as "1000.50"
        "%.2f".format(currentBudgetAmount)
    }
    // --- END OF FIX ---

    var textValue by remember(currentBudgetAmount) {
        mutableStateOf(initialAmount)
    }

    // --- ✅ Helper function for saving ---
    val saveBudget = {
        val newAmount = textValue.toDoubleOrNull() ?: 0.0
        onBudgetSet(newAmount)
        focusManager.clearFocus()
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
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = textValue,
                onValueChange = {
                    if (it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) { // Allow 2 decimal places
                        textValue = it
                    }
                },
                label = { Text("Amount") },
                prefix = { Text("₹ ") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { saveBudget() } // Save on keyboard "Done"
                ),
                // --- ✅ ITEM #8 FIX: Add a tick button ---
                trailingIcon = {
                    IconButton(onClick = { saveBudget() }) {
                        Icon(Icons.Default.Check, "Save Budget")
                    }
                }
                // --- END OF FIX ---
            )
        }
    }
}