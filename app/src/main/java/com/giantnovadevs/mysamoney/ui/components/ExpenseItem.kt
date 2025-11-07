package com.giantnovadevs.mysamoney.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong // ✅ Let's use the same icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.giantnovadevs.mysamoney.data.Expense
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseItem(expense: Expense, categoryName: String, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    // We can remember the formatter
    val df = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    // ✅ Using the same layout as HomeScreen's row for consistency
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f) // Let this section grow
            ) {
                Icon(
                    imageVector = Icons.Filled.ReceiptLong,
                    contentDescription = "Expense",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.padding(end = 8.dp)) { // Padding to avoid text clash
                    Text(
                        text = "₹${expense.amount}",
                        style = MaterialTheme.typography.titleMedium,
                        color = com.giantnovadevs.mysamoney.ui.theme.Expense // ✅ Use semantic color
                    )
                    Text(
                        text = categoryName, // ✅ Using Category as the subtitle
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = df.format(Date(expense.date)), // Moved date here
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✅ Delete button is now the main action on the right
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error // Use error color for delete
                )
            }
        }
    }
}