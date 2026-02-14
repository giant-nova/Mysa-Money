package com.giantnovadevs.mysamoney.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.giantnovadevs.mysamoney.data.Income
import com.giantnovadevs.mysamoney.ui.theme.Success // Your custom Success color
import com.giantnovadevs.mysamoney.viewmodel.IncomeViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Theme Colors ---
private val AppBackground = Color(0xFFF6F7F9)
private val CardBackground = Color.White
private val CardBorder = Color(0xFFEBEBEB)
private val TextPrimary = Color(0xFF1A1C1E)
private val TextSecondary = Color(0xFF72777F)
private val DeleteColor = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeListScreen(
    navController: NavController,
    onMenuClick: () -> Unit
) {
    val incomeVm: IncomeViewModel = viewModel()
    val incomes by incomeVm.allIncomes.collectAsState()

    // State for delete confirmation
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Incomes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = AppBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("income_entry?id=null") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Income")
            }
        }
    ) { padding ->
        Crossfade(
            targetState = incomes.isEmpty(),
            modifier = Modifier.padding(padding).fillMaxSize(),
            label = "Empty/List"
        ) { isEmpty ->
            if (isEmpty) {
                IncomeEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(incomes, key = { it.id }) { income ->

                        // --- SWIPE TO DELETE ---
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    incomeToDelete = income
                                    false // Snap back, show dialog
                                } else {
                                    false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = { IncomeDeleteBackground(dismissState) },
                            content = {
                                IncomeListTile(
                                    income = income,
                                    onClick = { navController.navigate("income_entry?id=${income.id}") }
                                )
                            },
                            enableDismissFromStartToEnd = false
                        )
                    }
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (incomeToDelete != null) {
        AlertDialog(
            onDismissRequest = { incomeToDelete = null },
            title = { Text("Delete Income?") },
            text = { Text("Are you sure you want to delete this income entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        incomeToDelete?.let { incomeVm.deleteIncome(it) }
                        incomeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { incomeToDelete = null }) {
                    Text("Cancel")
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

/**
 * Clean White Tile for Income
 */
@Composable
private fun IncomeListTile(
    income: Income,
    onClick: () -> Unit
) {
    val decimalFormat = remember { DecimalFormat("₹#,##0") }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dateString = dateFormatter.format(Date(income.date))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(1.dp, CardBorder),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Placeholder (Green Theme)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Success.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.TrendingUp,
                        contentDescription = "Income",
                        tint = Success,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = income.note.ifBlank { "Income" },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Text(
                text = "+${decimalFormat.format(income.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = Success, // Green Color for amount
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Red Swipe Background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeDeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        DeleteColor
    } else {
        Color.Transparent
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = Color.White
        )
    }
}

@Composable
private fun IncomeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color.White, CircleShape)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No Incomes Found",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the + button to add your salary or other income sources.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}