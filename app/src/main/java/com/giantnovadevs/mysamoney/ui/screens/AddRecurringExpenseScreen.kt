package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.Frequency
import com.giantnovadevs.mysamoney.data.RecurringExpense
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.RecurringExpenseViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecurringExpenseScreen(
    navController: NavController,
    expenseId: String?
) {
    val recurringVm: RecurringExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val focusManager = LocalFocusManager.current

    // Form State
    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }

    // Dropdown States
    var categoriesExpanded by remember { mutableStateOf(false) }
    var frequencyExpanded by remember { mutableStateOf(false) }

    // Logic State
    var selectedFrequency by remember { mutableStateOf(Frequency.MONTHLY) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val isEditMode = expenseId != null
    var expenseToEdit by remember { mutableStateOf<RecurringExpense?>(null) }

    // Validation
    val isFormValid by remember(amount, selectedCat) {
        mutableStateOf((amount.toDoubleOrNull() ?: 0.0) > 0 && selectedCat != null)
    }

    // --- Load Data for Edit Mode ---
    LaunchedEffect(expenseId, categories) {
        if (isEditMode && categories.isNotEmpty()) {
            val id = expenseId?.toIntOrNull() ?: -1
            recurringVm.getRecurringExpenseById(id).collect { expense ->
                if (expense != null) {
                    expenseToEdit = expense
                    amount = String.format("%.2f", expense.amount).removeSuffix(".00")
                    note = expense.note ?: ""
                    selectedFrequency = expense.frequency
                    selectedCat = categories.find { it.id == expense.categoryId }
                    // Convert Start Date (Long) -> LocalDate
                    selectedDate = Instant.ofEpochMilli(expense.startDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
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
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Subscription" else "New Subscription",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- Form Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                border = BorderStroke(1.dp, CardBorder),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Amount
                    CleanOutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) amount = it },
                        label = "Amount",
                        icon = Icons.Filled.MonetizationOn,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        prefix = { Text("₹ ", style = MaterialTheme.typography.bodyLarge, color = TextPrimary) }
                    )

                    // 2. Category Dropdown
                    ExposedDropdownMenuBox(
                        expanded = categoriesExpanded,
                        onExpandedChange = { categoriesExpanded = !categoriesExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CleanOutlinedTextField(
                            value = selectedCat?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = "Category",
                            icon = Icons.Filled.Category,
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoriesExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = categoriesExpanded,
                            onDismissRequest = { categoriesExpanded = false },
                            modifier = Modifier.background(CardBackground)
                        ) {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name, color = TextPrimary) },
                                    onClick = {
                                        selectedCat = category
                                        categoriesExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 3. Frequency Dropdown
                    ExposedDropdownMenuBox(
                        expanded = frequencyExpanded,
                        onExpandedChange = { frequencyExpanded = !frequencyExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CleanOutlinedTextField(
                            value = selectedFrequency.name.lowercase().replaceFirstChar { it.titlecase() },
                            onValueChange = {},
                            readOnly = true,
                            label = "Frequency",
                            icon = Icons.Filled.Repeat,
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = frequencyExpanded,
                            onDismissRequest = { frequencyExpanded = false },
                            modifier = Modifier.background(CardBackground)
                        ) {
                            Frequency.values().forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq.name.lowercase().replaceFirstChar { it.titlecase() }, color = TextPrimary) },
                                    onClick = {
                                        selectedFrequency = freq
                                        frequencyExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Start Date
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        CleanOutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                            onValueChange = {},
                            label = "First Payment Date",
                            icon = Icons.Filled.DateRange,
                            enabled = false // Click handled by Box
                        )
                    }

                    // 5. Note
                    CleanOutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note (e.g. Netflix, Rent)",
                        icon = Icons.Filled.Notes,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

            // --- Save Button ---
            Button(
                onClick = {
                    val startDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val noteText = if (note.isBlank()) null else note

                    if (isEditMode && expenseToEdit != null) {
                        // Calculate new Next Due Date
                        // Note: In a real app, you might want logic to keep the original due date cycle if only amount changed
                        // but resetting based on start date + frequency is safer for consistency.
                        val nextDue = recurringVm.calculateFirstDueDate(selectedDate, selectedFrequency)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                        val updated = expenseToEdit!!.copy(
                            amount = amt,
                            categoryId = selectedCat!!.id,
                            note = noteText,
                            frequency = selectedFrequency,
                            startDate = startDateMillis,
                            nextDueDate = nextDue
                        )
                        recurringVm.updateRecurringExpense(updated)
                    } else {
                        recurringVm.addRecurringExpense(
                            amount = amt,
                            categoryId = selectedCat!!.id,
                            note = noteText,
                            frequency = selectedFrequency,
                            startDate = selectedDate
                        )
                    }
                    navController.popBackStack()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save Subscription",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
            colors = DatePickerDefaults.colors(containerColor = CardBackground)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}