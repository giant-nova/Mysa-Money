package com.giantnovadevs.mysamoney.ui.screens

import android.app.Activity
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Income
import com.giantnovadevs.mysamoney.ads.RewardedInterstitialAdManager
import com.giantnovadevs.mysamoney.viewmodel.IncomeViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
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
fun AddIncomeScreen(
    navController: NavController,
    incomeId: String?,
    proViewModel: ProViewModel
) {
    val incomeVm: IncomeViewModel = viewModel()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isPro by proViewModel.isProUser.collectAsState()

    val isEditMode = incomeId != null
    var incomeToEdit by remember { mutableStateOf<Income?>(null) }

    // Form State
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Validation
    val isFormValid by remember(amount, note) {
        mutableStateOf((amount.toDoubleOrNull() ?: 0.0) > 0 && note.isNotBlank())
    }

    LaunchedEffect(isPro) {
        if (!isPro) {
            RewardedInterstitialAdManager.load(context)
        }
    }

    // --- Load Data for Edit Mode ---
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

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Income" else "Add Income",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Only show delete in edit mode
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
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

                    // 2. Note
                    CleanOutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Source (e.g. Salary, Freelance)",
                        icon = Icons.Filled.Notes,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )

                    // 3. Date Picker
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        CleanOutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                            onValueChange = {},
                            label = "Date Received",
                            icon = Icons.Filled.DateRange,
                            enabled = false // Click handled by Box
                        )
                    }
                }
            }

            // --- Save Button ---
            Button(
                onClick = {
                    val dateInMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    if (isEditMode && incomeToEdit != null) {
                        val updatedIncome = incomeToEdit!!.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            note = note,
                            date = dateInMillis
                        )
                        incomeVm.updateIncome(updatedIncome)
                    } else {
                        incomeVm.addIncome(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            note = note,
                            date = selectedDate
                        )
                    }
                    if (!isPro) {
                        (context as? Activity)?.let { activity ->
                            RewardedInterstitialAdManager.showIfAvailable(activity)
                        }
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
                    text = if (isEditMode) "Save Changes" else "Save Income",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (!isPro) {
                Spacer(modifier = Modifier.height(16.dp))
                AdMobNative(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // --- Delete Dialog ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Income?") },
            text = { Text("Are you sure you want to remove this income entry? This affects your balance.") },
            confirmButton = {
                Button(
                    onClick = {
                        incomeToEdit?.let { incomeVm.deleteIncome(it) }
                        showDeleteDialog = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp)
        )
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

/**
 * Reusable Clean Text Field Style
 */
@Composable
private fun CleanOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    prefix: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        prefix = prefix,
        readOnly = readOnly,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = CardBorder,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = TextSecondary,
            disabledBorderColor = CardBorder,
            disabledTextColor = TextPrimary,
            disabledLabelColor = TextSecondary
        ),
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}