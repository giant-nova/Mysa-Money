package com.giantnovadevs.mysamoney.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur // ✅ Add blur import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
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
    proViewModel: ProViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textRecognizer = remember { TextRecognitionHelper(context) }

    val expVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()

    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var categoriesExpanded by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // --- Get Pro Status & Scan Count ---
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

    // --- ✅ BUG FIX: "Decrement by 2" ---
    // We now collect the URI as state. This is cleaner and avoids re-observing.
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    // Get the flow (it will be null if savedStateHandle is null)
    val imageUriFlow = savedStateHandle?.getStateFlow<Uri?>("image_uri", null)

    // Collect the state. If the flow is null, create a remembered state that is just null.
    val imageUri by imageUriFlow?.collectAsState() ?: remember { mutableStateOf<Uri?>(null) }

    // This LaunchedEffect will ONLY run when imageUri changes from null to a non-null value
    LaunchedEffect(imageUri) {
        val uri = imageUri
        if (uri != null) {
            // We got a photo!
            if (!isPro) {
                proViewModel.useFreeScan() // This will now run only ONCE
            }
            // Analyze the image
            scope.launch {
                val parsedAmount = textRecognizer.analyze(uri)
                if (parsedAmount != null) {
                    amount = parsedAmount
                }
                // Clear the value from the handle to "re-arm" the trigger
                savedStateHandle?.set("image_uri", null)
            }
        }
    }
    // --- END OF BUG FIX ---

    // (Category selection LaunchedEffect is unchanged)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
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
                    .align(Alignment.TopCenter)
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // --- 1. Amount Field (Modified) ---
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

                            // --- ✅ NEW UI: Locked Icon & Tooltip ---
                            trailingIcon = {
                                val isLocked = !isPro && freeScans == 0

                                if (isLocked) {
                                    // User is Free and OUT of scans
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
                                        // Show a blurred lock icon
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = "Scan locked, upgrade to Pro",
                                            modifier = Modifier.blur(4.dp), // Apply blur
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    // User is Pro OR has free scans left
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
                            // --- END OF NEW UI ---
                        )

                        // Show remaining scans text
                        if (!isPro) {
                            val scanText = if (freeScans > 0) {
                                "You have $freeScans free scans remaining."
                            } else {
                                "Upgrade to Pro for unlimited scans."
                            }
                            Text(
                                text = scanText,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (freeScans > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 0.dp)
                            )
                        }

                        // ... (Category Field is unchanged)
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
                                modifier = Modifier.fillMaxWidth().menuAnchor()
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

                        // ... (Date Picker is unchanged, uses the autofill-proof Box)
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

                        // ... (Note Field is unchanged)
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            leadingIcon = { Icon(Icons.Filled.Notes, "Note") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- Save Button (Bottom Aligned) ---
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()!!
                    expVm.addExpense(
                        Expense(
                            amount = amt,
                            categoryId = selectedCat!!.id,
                            note = if (note.isBlank()) null else note,
                            date = selectedDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                        )
                    )
                    navController.popBackStack()
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }

    // --- Date Picker Dialog (Unchanged) ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
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