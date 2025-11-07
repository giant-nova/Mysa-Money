package com.giantnovadevs.mysamoney.ui.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Category
import com.giantnovadevs.mysamoney.viewmodel.CategoryViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBarDefaults



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    navController: NavController,
    onMenuClick: () -> Unit // <-- Add this parameter
) {
    val catVm: CategoryViewModel = viewModel()
    val categories by catVm.categories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                // ✅ Add the navigationIcon
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                // ✅ Style the FAB to match the theme
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            LazyColumn { items(categories) { c -> CategoryRow(c) { catVm.deleteCategory(c) } } }
        }


        if (showAdd) {
            AlertDialog(
                onDismissRequest = { showAdd = false },
                confirmButton = {
                    TextButton(onClick = {
                        if (newName.isNotBlank()) {
                            catVm.addCategory(newName)
                            newName = ""
                            showAdd = false
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAdd = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("New Category", style = MaterialTheme.typography.titleMedium) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") }
                    )
                }
            )
        }

    }
}


@Composable
fun CategoryRow(category: Category, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
        }
    }
}