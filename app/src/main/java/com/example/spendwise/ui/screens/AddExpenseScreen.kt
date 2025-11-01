package com.example.spendwise.ui.screens


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.spendwise.data.Category
import com.example.spendwise.data.Expense
import com.example.spendwise.viewmodel.CategoryViewModel
import com.example.spendwise.viewmodel.ExpenseViewModel
import androidx.compose.material3.TopAppBarDefaults
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import com.example.spendwise.ml.TextRecognitionHelper
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(navController: NavController, categoryId: String?) {
    val expVm: ExpenseViewModel = viewModel()
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val textRecognizer = remember { TextRecognitionHelper(context) }

    var amount by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<Category?>(null) }
    var note by remember { mutableStateOf("") }
    var categoriesExpanded by remember { mutableStateOf(false) }

    // This state is derived from other states. It makes the UI more "intuitive"
    // by enabling/disabling the save button.
    val isFormValid by remember(amount, selectedCat) {
        mutableStateOf(
            (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedCat != null
        )
    }

    // --- 1. State for permission ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    // --- 2. Permission launcher ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            navController.navigate("camera_screen")
        }
    }

    // --- 3. Listen for the camera result ---
    val imageUriResult = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Uri>("image_uri")

    LaunchedEffect(imageUriResult) {
        imageUriResult?.observeForever { uri ->
            if (uri != null) {
                // We got a photo! Analyze it.
                scope.launch {
                    val parsedAmount = textRecognizer.analyze(uri)
                    if (parsedAmount != null) {
                        amount = parsedAmount // Set the amount!
                    }
                    // Clear the result so we don't process it again
                    navController.currentBackStackEntry?.savedStateHandle?.set("image_uri", null)
                }
            }
        }
    }

    // ✅ Whenever categories change, set first as default if none selected
    LaunchedEffect(categories, categoryId) {
        if (categories.isNotEmpty()) {
            // Check if a valid categoryId was passed
            val preSelected = categoryId?.toIntOrNull()?.let { id ->
                categories.find { it.id == id }
            }

            if (preSelected != null) {
                // If we found the pre-selected category, set it
                selectedCat = preSelected
            } else if (selectedCat == null) {
                // Otherwise, just set the first category as default
                selectedCat = categories.first()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Go Back")
                    }
                }
            )
        }
    ) { padding ->
        // This Box provides the animation and the bottom-aligned button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            // Column for all the form fields
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {

                // ✅ Animation: All content fades in and slides up
                AnimatedVisibility(
                    visible = true, // We can set this to a state if we want to delay it
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp) // Principle 1
                    ) {

                        // --- 1. Amount Field (The Hero) ---
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
                            // --- 4. Add the Scan Button ---
                            trailingIcon = {
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
                        )

                        // --- 2. Category Field (M3 Dropdown) ---
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
                                // Principle 5
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Category,
                                        contentDescription = "Category"
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = categoriesExpanded
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor() // This is important
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

                        // --- 3. Note Field ---
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note (optional)") },
                            // Principle 5
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Notes,
                                    contentDescription = "Note"
                                )
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text, // Use .Text for general notes
                                capitalization = KeyboardCapitalization.Sentences, // This is the setting you want
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- 4. Save Button (Bottom Aligned) ---
            Button(
                onClick = {
                    // We know from isFormValid that amount is valid and selectedCat is not null
                    val amt = amount.toDoubleOrNull()!!

                    expVm.addExpense(
                        Expense(
                            amount = amt,
                            categoryId = selectedCat!!.id,
                            note = note.ifBlank { null },
                            date = Date().time
                        )
                    )
                    navController.popBackStack()
                },
                // ✅ Intuitive UI: Button is disabled until form is valid
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .align(Alignment.BottomCenter) // Principle 4: Grouping/Layout
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleMedium // Principle 2
                )
            }
        }
    }
}