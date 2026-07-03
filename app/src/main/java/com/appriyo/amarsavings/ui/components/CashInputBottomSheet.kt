package com.appriyo.amarsavings.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.amarsavings.data.db.DenominationInput
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.ui.theme.Coral400
import com.appriyo.amarsavings.ui.theme.Indigo400
import com.appriyo.amarsavings.ui.theme.Mint400
import com.appriyo.amarsavings.ui.theme.Violet400
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
    val accent = if (isAdd) Mint400 else Coral400
    val secondaryAccent = if (isAdd) Indigo400 else Violet400
    val title = if (isAdd) "Add Cash" else "Withdraw Cash"
    val actionLabel = if (isAdd) "Add to Savings" else "Confirm Withdraw"

    // Mint→Indigo for Add, Coral→Violet for Withdraw — used by both the hero
    // header and the save button so they stay visually consistent.
    val accentBrush = if (isAdd)
        Brush.linearGradient(colors = listOf(Mint400, Indigo400))
    else
        Brush.linearGradient(colors = listOf(Coral400, Violet400))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Hero header ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(accentBrush)
            ) {
                val heroBlob = remember {
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .background(heroBlob)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAdd) "Add cash to your savings"
                                else "Withdraw cash from savings",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        TextButton(onClick = onClearAll) {
                            Text(
                                text = "Clear",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Total ${if (isAdd) "to add" else "to withdraw"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                    AnimatedContent(
                        targetState = totalAmount.formatTaka(),
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()) togetherWith
                                    (slideOutVertically { -it } + fadeOut())
                        },
                        label = "total-anim"
                    ) { amount ->
                        Text(
                            text = amount,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            // ── Denomination list ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DENOMINATIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                denominations.forEach { denom ->
                    DenominationRow(
                        denomination = denom,
                        accent = accent,
                        secondaryAccent = secondaryAccent,
                        onIncrement = { onIncrement(denom.denomination) },
                        onDecrement = { onDecrement(denom.denomination) },
                        onQuantityChange = { onQuantityChange(denom.denomination, it) }
                    )
                }
            }

            // ── Sticky action ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.0f),
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accentBrush)
                        .clickableNoRipple(enabled = totalAmount > 0, onClick = { onSave() })
                        .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(18.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isAdd) Icons.Rounded.Add else Icons.Rounded.ArrowOutward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DenominationRow(
    denomination: DenominationInput,
    accent: Color,
    secondaryAccent: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val isActive = denomination.quantity > 0
    var textValue by remember(denomination.quantity) {
        mutableStateOf(if (denomination.quantity == 0) "" else denomination.quantity.toString())
    }

    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val containerBrush = remember(isActive, accent, secondaryAccent, surfaceVariant) {
        if (isActive) Brush.horizontalGradient(
            colors = listOf(
                accent.copy(alpha = 0.10f),
                secondaryAccent.copy(alpha = 0.06f)
            )
        ) else {
            Brush.horizontalGradient(
                colors = listOf(surfaceVariant.copy(alpha = 0.6f), surfaceVariant.copy(alpha = 0.4f))
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(containerBrush)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) accent.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Note badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) accent.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        1.dp,
                        if (isActive) accent.copy(alpha = 0.30f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "৳${denomination.denomination}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(visible = isActive) {
                Text(
                    text = "= ${denomination.subtotal.formatTaka()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(4.dp))

            // Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallIconButton(
                    onClick = onDecrement,
                    enabled = denomination.quantity > 0,
                    backgroundColor = MaterialTheme.colorScheme.surface,
                    contentColor = if (denomination.quantity > 0) accent
                    else MaterialTheme.colorScheme.outline
                ) {
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
                    modifier = Modifier.width(60.dp),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) accent else MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = accent
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                SmallIconButton(
                    onClick = onIncrement,
                    backgroundColor = accent,
                    contentColor = Color.White
                ) {
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
    backgroundColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.4f))
            .border(
                1.dp,
                if (enabled) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}