package com.giantnovadevs.mysamoney.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Income // ✅ Import Income
import com.giantnovadevs.mysamoney.viewmodel.IncomeViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeScreen(
    navController: NavController,
    incomeId: String? // ✅ Use incomeId
) {
    val incomeVm: IncomeViewModel = viewModel()

    // --- ✅ ADD THIS "EDIT MODE" LOGIC ---
    val isEditMode = incomeId != null
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }

    // Form State
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // This LaunchedEffect will run ONCE if we are in edit mode
    LaunchedEffect(incomeId) {
        if (isEditMode) {
            val id = incomeId?.toIntOrNull() ?: -1
            incomeVm.getIncomeById(id).collect { income ->
                if (income != null) {
                    incomeToEdit = income
                    amount = String.format("%.2f", income.amount).removeSuffix(".00")
                    note = income.note
                    selectedDate = LocalDate.ofEpochDay(income.date / (1000 * 60 * 60 * 24))
                }
            }
        }
    }
    // --- END OF "EDIT MODE" LOGIC ---

    // Form validation
    val isFormValid by remember(amount, note) {
        mutableStateOf(
            (amount.toDoubleOrNull() ?: 0.0) > 0 && note.isNotBlank()
        )
    }


    val navToHome: () -> Unit = {
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Income" else "Add Income") },
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
                        if (it.matches(Regex("^\\d*\\.?\\d{0,2}\$"))) {
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

                // --- 2. Note Field ---
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (e.g., 'Salary', 'Freelance')") },
                    leadingIcon = { Icon(Icons.Filled.Notes, "Note") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // --- 3. Date Picker ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        leadingIcon = { Icon(Icons.Filled.DateRange, "Date") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // --- Save Button (Bottom Aligned) ---
            Button(
                onClick = {
                    val dateInMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // ✅ THIS IS THE NEW LOGIC
                    if (isEditMode) {
                        // We are UPDATING
                        val updatedIncome = incomeToEdit!!.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            note = note,
                            date = dateInMillis
                        )
                        incomeVm.updateIncome(updatedIncome)
                    } else {
                        // We are ADDING
                        incomeVm.addIncome(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            note = note,
                            date = selectedDate
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
                    text = if (isEditMode) "Save Changes" else "Save Income", // ✅
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
                                .atZone(ZoneId.systemDefault())
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