package com.giantnovadevs.mysamoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

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

    val expVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val isEditMode = expenseId != null

    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var categoriesExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- ✅ NEW STATE for "Add Category" Dialog ---
    var showAddCategoryDialog by remember { mutableStateOf(false) }

    val isPro by proViewModel.isProUser.collectAsState()
    val freeScans by proViewModel.freeScansRemaining.collectAsState()

    val isFormValid by remember(amount, selectedCat) {
        mutableStateOf(
            (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedCat != null
        )
    }

    // --- Permission State ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            navController.navigate("camera_screen")
        }
    }

    // --- Camera Result Listener ---
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val imageUriFlow = savedStateHandle?.getStateFlow<Uri?>("image_uri", null)
    val imageUri by imageUriFlow?.collectAsState() ?: remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(imageUri) {
        val uri = imageUri
        if (uri != null) {
            if (!isPro) {
                proViewModel.useFreeScan()
            }
            scope.launch {
                val parsedAmount = textRecognizer.analyze(uri)
                if (parsedAmount != null) {
                    amount = parsedAmount
                }
                savedStateHandle?.set("image_uri", null)
            }
        }
    }

    // --- Category Selection Logic ---
    LaunchedEffect(categories, categoryId) {
        if (categories.isNotEmpty()) {
            val preSelected = categoryId?.toIntOrNull()?.let { id ->
                categories.find { it.id == id }
            }
            if (preSelected != null) {
                selectedCat = preSelected
            } else if (selectedCat == null) {
                selectedCat = categories.first()
            }
        }
    }


    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }

    // This LaunchedEffect will run ONCE if we are in edit mode
    LaunchedEffect(expenseId, categories) {
        if (isEditMode) {
            val id = expenseId?.toIntOrNull() ?: -1
            expVm.getExpenseById(id).collect { expense ->
                if (expense != null) {
                    expenseToEdit = expense // Save the expense
                    amount = String.format("%.2f", expense.amount).removeSuffix(".00")
                    note = expense.note ?: ""
                    // Find and set the category
                    selectedCat = categories.find { it.id == expense.categoryId }
                    // Find and set the date
                    selectedDate = LocalDate.ofEpochDay(expense.date / (1000 * 60 * 60 * 24))
                }
            }
        }
    }

    val navToHome: () -> Unit = {
        navController.navigate("home") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    // --- Scaffold ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Expense" else "Add Expense") },
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
                    .align(Alignment.TopCenter)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    Column(
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
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                val isLocked = !isPro && freeScans == 0
                                if (isLocked) {
                                    val tooltipState = rememberTooltipState()
                                    TooltipBox(
                                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                                        tooltip = {
                                            PlainTooltip {
                                                Text("Out of scans? Upgrade to Pro for unlimited scans.")
                                            }
                                        },
                                        state = tooltipState
                                    ) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = "Scan locked, upgrade to Pro",
                                            modifier = Modifier.blur(4.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    IconButton(onClick = {
                                        if (hasCameraPermission) {
                                            navController.navigate("camera_screen")
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(Icons.Filled.CameraAlt, "Scan Receipt")
                                    }
                                }
                            }
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
                                // --- ✅ ITEM #1 FIX ---
                                Divider()
                                DropdownMenuItem(
                                    text = { Text("+ Add New Category") },
                                    onClick = {
                                        showAddCategoryDialog = true
                                        categoriesExpanded = false
                                    }
                                )
                                // --- END OF FIX ---
                            }
                        }

                        // --- 3. Date Picker (Item #2) ---
                        // This already exists and works!
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM, yyyy")),
                                onValueChange = {},
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

                        // --- 4. Note Field ---
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            leadingIcon = { Icon(Icons.Filled.Notes, "Note") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- Save Button ---
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()!!
                    val dateInMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    // ✅ THIS IS THE NEW LOGIC
                    if (isEditMode) {
                        // We are UPDATING an existing expense
                        val updatedExpense = expenseToEdit!!.copy(
                            amount = amt,
                            categoryId = selectedCat!!.id,
                            note = if (note.isBlank()) null else note,
                            date = dateInMillis
                        )
                        expVm.updateExpense(updatedExpense)
                    } else {
                        // We are ADDING a new expense
                        expVm.addExpense(
                            Expense(
                                amount = amt,
                                categoryId = selectedCat!!.id,
                                note = if (note.isBlank()) null else note,
                                date = dateInMillis
                            )
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
                    text = if (isEditMode) "Save Changes" else "Save",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // --- ✅ ITEM #1 FIX: Add Category Dialog ---
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            catVm = catVm,
            onDismiss = { showAddCategoryDialog = false }
        )
    }

    // --- Date Picker Dialog (Unchanged) ---
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

// --- ✅ ITEM #1 FIX: New Composable for the Dialog ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCategoryDialog(
    catVm: CategoryViewModel,
    onDismiss: () -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            OutlinedTextField(
                value = newCategoryName,
                onValueChange = { newCategoryName = it },
                label = { Text("Category Name") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
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
                enabled = newCategoryName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}