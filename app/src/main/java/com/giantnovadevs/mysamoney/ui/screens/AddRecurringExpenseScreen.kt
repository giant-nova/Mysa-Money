package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.Frequency
import com.giantnovadevs.mysamoney.data.RecurringExpense
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.RecurringExpenseViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    navController: NavController,
    expenseId: String?
) {
    val recurringVm: RecurringExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()

    // Form State
    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var categoriesExpanded by remember { mutableStateOf(false) }

    // --- New State for this screen ---
    var frequencyExpanded by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(Frequency.MONTHLY) } // Default to Monthly
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Form validation
    val isFormValid by remember(amount, selectedCat) {
        mutableStateOf(
            (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedCat != null
        )
    }

    val isEditMode = expenseId != null
    var expenseToEdit by remember { mutableStateOf<RecurringExpense?>(null) }

    // This LaunchedEffect will run ONCE if we are in edit mode
    LaunchedEffect(expenseId, categories) {
        if (isEditMode && categories.isNotEmpty()) {
            val id = expenseId?.toIntOrNull() ?: -1
            recurringVm.getRecurringExpenseById(id).collect { expense ->
                if (expense != null) {
                    expenseToEdit = expense
                    amount = String.format("%.2f", expense.amount).removeSuffix(".00")
                    note = expense.note ?: ""
                    selectedFrequency = expense.frequency
                    // Find and set the category
                    selectedCat = categories.find { it.id == expense.categoryId }
                    // Find and set the date
                    selectedDate = LocalDate.ofEpochDay(expense.startDate / (1000 * 60 * 60 * 24))
                }
            }
        }
    }

    // Auto-select first category
    LaunchedEffect(categories) {
        if (selectedCat == null && categories.isNotEmpty()) {
            selectedCat = categories.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Subscription" else "Add Subscription") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. Amount Field ---
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.matches(Regex("^\\d*\\.?\\d*\$"))) {
                            amount = it
                        }
                    },
                    label = { Text("Amount") },
                    leadingIcon = { Icon(Icons.Filled.MonetizationOn, "Amount") },
                    prefix = { Text("₹ ") },
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- 2. Category Dropdown ---
                ExposedDropdownMenuBox(
                    expanded = categoriesExpanded,
                    onExpandedChange = { categoriesExpanded = !categoriesExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCat?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Filled.Category, "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoriesExpanded,
                        onDismissRequest = { categoriesExpanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCat = category
                                    categoriesExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- 3. Frequency Dropdown ---
                ExposedDropdownMenuBox(
                    expanded = frequencyExpanded,
                    onExpandedChange = { frequencyExpanded = !frequencyExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedFrequency.name.lowercase()
                            .replaceFirstChar { it.titlecase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        leadingIcon = { Icon(Icons.Filled.Refresh, "Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyExpanded,
                        onDismissRequest = { frequencyExpanded = false }
                    ) {
                        Frequency.values().forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                onClick = {
                                    selectedFrequency = freq
                                    frequencyExpanded = false
                                }
                            )
                        }
                    }
                }

                // --- 4. Start Date Picker ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true } // <-- The click is on the Box
                ) {
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("First Payment Date") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, "Start Date") },
                        modifier = Modifier.fillMaxWidth(),

                        // 1. This stops the autofill service from triggering
                        enabled = false,

                        // 2. This makes the disabled field look like it's enabled
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // --- 5. Note Field ---
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g., 'Netflix', 'Rent')") },
                    leadingIcon = { Icon(Icons.Filled.Notes, "Note") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- Save Button (Bottom Aligned) ---
            Button(
                onClick = {
                    val date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // THIS IS THE NEW LOGIC
                    if (isEditMode) {
                        // We are UPDATING
                        val updatedExpense = expenseToEdit!!.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            categoryId = selectedCat!!.id,
                            note = if (note.isBlank()) null else note,
                            frequency = selectedFrequency,
                            startDate = date,
                            // Re-calculate next due date based on edited start date
                            nextDueDate = recurringVm.calculateFirstDueDate(selectedDate, selectedFrequency)
                                .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        )
                        recurringVm.updateRecurringExpense(updatedExpense)
                    } else {
                        // We are ADDING
                        recurringVm.addRecurringExpense(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            categoryId = selectedCat!!.id,
                            note = if (note.isBlank()) null else note,
                            frequency = selectedFrequency,
                            startDate = selectedDate
                        )
                    }
                    navController.popBackStack()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save Subscription",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}