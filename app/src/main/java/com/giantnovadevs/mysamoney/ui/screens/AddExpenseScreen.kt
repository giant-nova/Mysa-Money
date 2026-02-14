package com.giantnovadevs.mysamoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.data.Expense
import com.giantnovadevs.mysamoney.ml.TextRecognitionHelper
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import com.giantnovadevs.mysamoney.viewmodel.ExpenseViewModel
import com.giantnovadevs.mysamoney.viewmodel.ProViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF56595F)
private val TextSecondary = Color(0xFF72777F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    navController: NavController,
    categoryId: String?,
    expenseId: String?,
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textRecognizer = remember { TextRecognitionHelper(context) }
    val focusManager = LocalFocusManager.current

    val expVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val isEditMode = expenseId != null

    // Form State
    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // UI State
    var categoriesExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    // Pro Features
    val isPro by proViewModel.isProUser.collectAsState()
    val freeScans by proViewModel.freeScansRemaining.collectAsState()

    val isFormValid by remember(amount, selectedCat) {
        mutableStateOf((amount.toDoubleOrNull() ?: 0.0) > 0 && selectedCat != null)
    }

    // --- Permissions & Camera ---
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) navController.navigate("camera_screen")
    }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val imageUriFlow = savedStateHandle?.getStateFlow<Uri?>("image_uri", null)
    val imageUri by imageUriFlow?.collectAsState() ?: remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(imageUri) {
        val uri = imageUri
        if (uri != null) {
            if (!isPro) proViewModel.useFreeScan()
            scope.launch {
                val parsedAmount = textRecognizer.analyze(uri)
                if (parsedAmount != null) amount = parsedAmount
                savedStateHandle?.set("image_uri", null)
            }
        }
    }

    // --- Data Initialization ---
    LaunchedEffect(categories, categoryId) {
        if (categories.isNotEmpty()) {
            val preSelected = categoryId?.toIntOrNull()?.let { id -> categories.find { it.id == id } }
            if (preSelected != null) selectedCat = preSelected
            else if (selectedCat == null) selectedCat = categories.first()
        }
    }

    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    LaunchedEffect(expenseId, categories) {
        if (isEditMode) {
            val id = expenseId?.toIntOrNull() ?: -1
            expVm.getExpenseById(id).collect { expense ->
                if (expense != null) {
                    expenseToEdit = expense
                    amount = String.format("%.2f", expense.amount).removeSuffix(".00")
                    note = expense.note ?: ""
                    selectedCat = categories.find { it.id == expense.categoryId }
                    selectedDate = LocalDate.ofEpochDay(expense.date / (1000 * 60 * 60 * 24))
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
                        if (isEditMode) "Edit Expense" else "Add Expense",
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
            // --- Form Container (White Card) ---
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
                    // 1. Amount Input
                    CleanOutlinedTextField(
                        value = amount,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*\$"))) amount = it },
                        label = "Amount",
                        icon = Icons.Filled.MonetizationOn,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        trailingIcon = {
                            val isLocked = !isPro && freeScans == 0
                            if (isLocked) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Locked",
                                    tint = TextSecondary.copy(alpha = 0.5f)
                                )
                            } else {
                                IconButton(onClick = {
                                    if (hasCameraPermission) navController.navigate("camera_screen")
                                    else permissionLauncher.launch(Manifest.permission.CAMERA)
                                }) {
                                    Icon(Icons.Filled.CameraAlt, "Scan", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
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
                            HorizontalDivider(color = CardBorder)
                            DropdownMenuItem(
                                text = { Text("+ Add New Category", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showAddCategoryDialog = true
                                    categoriesExpanded = false
                                }
                            )
                        }
                    }

                    // 3. Date Picker
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        CleanOutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                            onValueChange = {},
                            label = "Date",
                            icon = Icons.Filled.DateRange,
                            enabled = false // Click handled by Box
                        )
                    }

                    // 4. Note Input
                    CleanOutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Note (Optional)",
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
                    val amt = amount.toDoubleOrNull()!!
                    val dateInMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    val expenseData = Expense(
                        id = if (isEditMode) expenseToEdit!!.id else 0, // 0 is ignored by Room auto-generate
                        amount = amt,
                        categoryId = selectedCat!!.id,
                        note = if (note.isBlank()) null else note,
                        date = dateInMillis
                    )

                    if (isEditMode) expVm.updateExpense(expenseData)
                    else expVm.addExpense(expenseData)

                    navController.popBackStack()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "Save Expense",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }
        }
    }

    // Dialogs
    if (showAddCategoryDialog) {
        AddCategoryDialog(catVm = catVm, onDismiss = { showAddCategoryDialog = false })
    }

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
 * A Custom Styled OutlinedTextField that fits the "Clean Grey-White" theme.
 */
@Composable
fun CleanOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = TextSecondary) },
        trailingIcon = trailingIcon,
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
            unfocusedBorderColor = CardBorder, // Subtle border
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

@Composable
private fun AddCategoryDialog(
    catVm: CategoryViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = { Text("New Category") },
        text = {
            CleanOutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = "Name",
                icon = Icons.Filled.Label
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        catVm.addCategory(newCategoryName)
                        onDismiss()
                    }
                },
                enabled = newCategoryName.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}