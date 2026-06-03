package com.appriyo.amarsavings.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.db.DenominationInput
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.ui.theme.*
import com.appriyo.amarsavings.util.formatTaka

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashInputBottomSheet(
    type: TransactionType,
    denominations: List<DenominationInput>,
    totalAmount: Long,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onQuantityChange: (Int, Int) -> Unit,
    onSave: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val isAdd = type == TransactionType.ADD
    val accentColor = if (isAdd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val title = if (isAdd) "Add Cash" else "Withdraw Cash"
    val actionLabel = if (isAdd) "Add to Savings" else "Withdraw"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enter note counts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Denomination list
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                denominations.forEach { denom ->
                    DenominationRow(
                        denomination = denom,
                        accentColor = accentColor,
                        onIncrement = { onIncrement(denom.denomination) },
                        onDecrement = { onDecrement(denom.denomination) },
                        onQuantityChange = { onQuantityChange(denom.denomination, it) }
                    )
                }
            }

            // Sticky total + action
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        AnimatedContent(
                            targetState = totalAmount.formatTaka(),
                            transitionSpec = {
                                slideInVertically { it } + fadeIn() togetherWith
                                        slideOutVertically { -it } + fadeOut()
                            }
                        ) { amount ->
                            Text(
                                text = amount,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    Button(
                        onClick = onSave,
                        enabled = totalAmount > 0,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isAdd) Slate900 else White
                        )
                    ) {
                        Icon(
                            imageVector = if (isAdd) Icons.Rounded.Add else Icons.Rounded.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DenominationRow(
    denomination: DenominationInput,
    accentColor: androidx.compose.ui.graphics.Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val isActive = denomination.quantity > 0
    val animatedAlpha by animateFloatAsState(if (isActive) 1f else 0.6f)
    var textValue by remember(denomination.quantity) {
        mutableStateOf(if (denomination.quantity == 0) "" else denomination.quantity.toString())
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) accentColor.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant,
        border = if (isActive) BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isActive) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "৳${denomination.denomination}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            // Subtotal
            AnimatedVisibility(visible = isActive) {
                Text(
                    text = "= ${denomination.subtotal.formatTaka()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(4.dp))

            // Counter control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallIconButton(onClick = onDecrement, enabled = denomination.quantity > 0) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Decrement",
                        modifier = Modifier.size(16.dp)
                    )
                }

                OutlinedTextField(
                    value = textValue,
                    onValueChange = { v ->
                        val num = v.filter { it.isDigit() }
                        textValue = num
                        onQuantityChange(num.toIntOrNull() ?: 0)
                    },
                    modifier = Modifier.width(56.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                SmallIconButton(onClick = onIncrement) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Increment",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(32.dp),
        shape = RoundedCornerShape(8.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        content()
    }
}