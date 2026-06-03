package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.util.formatTaka
import com.appriyo.amarsavings.util.smartFormatDate

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isAdd = transaction.type == TransactionType.ADD

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Type icon
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isAdd)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.errorContainer
        ) {
            Icon(
                imageVector = if (isAdd) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                contentDescription = null,
                modifier = Modifier.padding(10.dp).size(20.dp),
                tint = if (isAdd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isAdd) "Added" else "Withdrawn",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = transaction.timestamp.smartFormatDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Denomination breakdown
            val breakdown = transaction.denominationBreakdown()
            if (breakdown.isNotEmpty()) {
                Text(
                    text = breakdown.joinToString(" · ") { "${it.second}×${it.first}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
            }
        }

        // Amount + delete
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (isAdd) "+" else "-"}${transaction.totalAmount.formatTaka()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isAdd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Delete Transaction") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}