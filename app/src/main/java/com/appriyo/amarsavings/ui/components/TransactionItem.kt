package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.ui.theme.Coral400
import com.appriyo.amarsavings.ui.theme.Indigo400
import com.appriyo.amarsavings.ui.theme.Mint400
import com.appriyo.amarsavings.ui.theme.Violet400
import com.appriyo.amarsavings.ui.theme.White
import com.appriyo.amarsavings.util.formatTaka
import com.appriyo.amarsavings.util.smartFormatDate

@Composable
fun TransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isAdd = transaction.type == TransactionType.ADD

    val accent = if (isAdd) Mint400 else Coral400
    val secondaryAccent = if (isAdd) Indigo400 else Violet400
    val isDark = isDarkTheme()

    val avatarBrush = remember(isAdd, isDark) {
        val alpha = if (isDark) 0.25f else 0.16f
        Brush.linearGradient(
            colors = listOf(accent.copy(alpha = alpha), secondaryAccent.copy(alpha = alpha))
        )
    }
    val avatarBorderColor = remember(isAdd) { accent.copy(alpha = 0.30f) }

    Column {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(avatarBrush)
                    .border(1.dp, avatarBorderColor, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAdd) Icons.Rounded.ArrowDownward
                    else Icons.Rounded.ArrowOutward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAdd) "Added to savings" else "Withdrawn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = transaction.timestamp.smartFormatDate(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val breakdown = transaction.denominationBreakdown()
                if (breakdown.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = breakdown.joinToString(" · ") { "${it.second}×৳${it.first}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${if (isAdd) "+" else "-"}${transaction.totalAmount.formatTaka()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickableNoRipple { showDeleteDialog = true },
                    contentAlignment = Alignment.Center
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
        if (showDivider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Coral400.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Coral400
                    )
                }
            },
            title = {
                Text(
                    text = "Delete Transaction?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This action cannot be undone. The transaction will be removed permanently.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Coral400,
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}