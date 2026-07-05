package com.appriyo.amarsavings.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.amarsavings.data.db.DenominationInput
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.ui.theme.Coral400
import com.appriyo.amarsavings.ui.theme.GradientHeroDark
import com.appriyo.amarsavings.ui.theme.GradientHeroLight
import com.appriyo.amarsavings.ui.theme.Indigo400
import com.appriyo.amarsavings.ui.theme.Indigo500
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
    val title = if (isAdd) "Add Cash" else "Withdraw Cash"
    val subtitle = if (isAdd) "Add cash to your savings" else "Withdraw cash from savings"
    val actionLabel = if (isAdd) "Add to Savings" else "Confirm Withdraw"
    val typeAccent = if (isAdd) Mint400 else Coral400

    val isDark = isDarkTheme()
    val heroBrush = if (isDark) GradientHeroDark else GradientHeroLight

    // ── Anti-accidental-dismissal state ──────────────────────────────────
    // The sheet's drag-to-close gesture is intentionally very forgiving by
    // default, which means a small downward flick or an accidental scrim
    // tap immediately closes it. We add two guards so a stray touch can
    // never dismiss the sheet:
    //
    //  1. `confirmValueChange` refuses to transition to SheetValue.Hidden
    //     from a drag — the sheet physically cannot collapse on its own.
    //  2. `onDismissRequest` (scrim tap / back press) is debounced for the
    //     first ~300 ms after the sheet opens, and between consecutive
    //     dismiss attempts, so accidental taps are ignored.
    //
    // The deliberate close paths (scrim tap after the debounce window, and
    // the back button) still trigger `sheetState.hide()` to animate the
    // sheet out cleanly before the parent removes it from composition.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            // Allow staying open or re-expanding; never allow an implicit
            // collapse via a small swipe. Dismissal is gated to the
            // `onDismissRequest` path below (scrim tap / back press).
            newValue != SheetValue.Hidden
        }
    )
    val sheetScope = rememberCoroutineScope()

    var lastDismissAllowedAtMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        // Allow the first real dismiss ~300 ms after the sheet has opened.
        lastDismissAllowedAtMs = System.currentTimeMillis() + 300L
    }

    ModalBottomSheet(
        onDismissRequest = {
            val now = System.currentTimeMillis()
            if (now >= lastDismissAllowedAtMs) {
                // Prevent a rapid burst of dismiss attempts from registering.
                lastDismissAllowedAtMs = now + 500L
                // Animate the sheet out smoothly before the parent removes it.
                sheetScope.launch { sheetState.hide() }
                onDismiss()
            }
            // else: ignore — accidental scrim tap within the debounce window.
        },
        sheetState = sheetState,
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
            // ── Hero header — matches Dashboard hero exactly ───────────
            HeroHeader(
                title = title,
                subtitle = subtitle,
                totalAmount = totalAmount,
                typeAccent = typeAccent,
                heroBrush = heroBrush,
                onClearAll = onClearAll
            )

            // ── Denomination list ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DENOMINATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Tap notes to add",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(2.dp))
                denominations.forEach { denom ->
                    DenominationRow(
                        denomination = denom,
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
                SaveActionButton(
                    heroBrush = heroBrush,
                    actionLabel = actionLabel,
                    isAdd = isAdd,
                    enabled = totalAmount > 0,
                    onClick = { onSave() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Hero header — exact same construction as DashboardScreen.HeroBalanceCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HeroHeader(
    title: String,
    subtitle: String,
    totalAmount: Long,
    typeAccent: Color,
    heroBrush: Brush,
    onClearAll: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "sheet-shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheet-shimmer-x"
    )

    val blobBrushA = remember {
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
        )
    }
    val blobBrushB = remember {
        Brush.radialGradient(
            colors = listOf(Violet400.copy(alpha = 0.35f), Color.Transparent)
        )
    }
    val blobBrushAccent = remember(typeAccent) {
        Brush.radialGradient(
            colors = listOf(typeAccent.copy(alpha = 0.30f), Color.Transparent)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(heroBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = blobBrushA,
                radius = w * 0.55f,
                center = Offset(w * 0.2f, h * 0.1f)
            )
            drawCircle(
                brush = blobBrushB,
                radius = w * 0.7f,
                center = Offset(w * 0.9f, h * 0.85f)
            )
            drawCircle(
                brush = blobBrushAccent,
                radius = w * 0.4f,
                center = Offset(w * 0.85f, h * 0.15f)
            )
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    start = Offset(shimmerX, 0f),
                    end = Offset(shimmerX + 200f, h)
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, h)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .clickableNoRipple(onClearAll)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.75f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(4.dp))

            AnimatedContent(
                targetState = totalAmount,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                            (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "total-anim"
            ) { value ->
                Text(
                    text = value.formatTaka(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Denomination row — glass surface with subtle Indigo→Violet highlight
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DenominationRow(
    denomination: DenominationInput,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onQuantityChange: (Int) -> Unit
) {
    val isActive = denomination.quantity > 0
    val isDark = isDarkTheme()

    var textValue by remember(denomination.quantity) {
        mutableStateOf(if (denomination.quantity == 0) "" else denomination.quantity.toString())
    }

    val animatedActive by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "row-active"
    )

    val containerBrush = remember(isDark, animatedActive) {
        if (isDark) {
            Brush.horizontalGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.lerp(
                        Color(0xFF1C2030),
                        Indigo400.copy(alpha = 0.16f),
                        animatedActive
                    ),
                    androidx.compose.ui.graphics.lerp(
                        Color(0xFF161A26),
                        Violet400.copy(alpha = 0.12f),
                        animatedActive
                    )
                )
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(
                    androidx.compose.ui.graphics.lerp(
                        Color.White,
                        Indigo400.copy(alpha = 0.10f),
                        animatedActive
                    ),
                    androidx.compose.ui.graphics.lerp(
                        Color(0xFFFBFBFD),
                        Violet400.copy(alpha = 0.08f),
                        animatedActive
                    )
                )
            )
        }
    }

    val borderBrush = remember(isDark, animatedActive) {
        if (isActive) {
            Brush.horizontalGradient(
                colors = listOf(
                    Indigo400.copy(alpha = 0.55f),
                    Violet400.copy(alpha = 0.45f)
                )
            )
        } else if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.06f),
                    Color.White.copy(alpha = 0.02f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.06f),
                    Color.Black.copy(alpha = 0.01f)
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerBrush)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Note badge — pill style like dashboard QuickStat icon
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isActive) Brush.horizontalGradient(
                            colors = listOf(
                                Indigo500.copy(alpha = 0.18f),
                                Violet400.copy(alpha = 0.18f)
                            )
                        ) else Brush.horizontalGradient(
                            colors = listOf(
                                Indigo500.copy(alpha = 0.08f),
                                Violet400.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isActive) Indigo400.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "৳${denomination.denomination}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.weight(1f))

            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 }
            ) {
                Text(
                    text = "= ${denomination.subtotal.formatTaka()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.width(2.dp))

            // Counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallIconButton(
                    onClick = onDecrement,
                    enabled = denomination.quantity > 0
                ) {
                    Icon(
                        Icons.Rounded.Remove,
                        contentDescription = "Decrement",
                        modifier = Modifier.size(16.dp),
                        tint = if (denomination.quantity > 0)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }

                CompactQuantityField(
                    value = textValue,
                    onValueChange = { v ->
                        val num = v.filter { it.isDigit() }
                        textValue = num
                        onQuantityChange(num.toIntOrNull() ?: 0)
                    },
                    isActive = isActive
                )

                SmallIconButton(
                    onClick = onIncrement,
                    enabled = true,
                    primary = true
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "Increment",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Slim, fast quantity field — uses BasicTextField with no Material decoration
 * for snappy, lag-free input on lower-end devices.
 */
@Composable
private fun CompactQuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    isActive: Boolean
) {
    val textColor = if (isActive) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurface
    val cursorColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(10.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                color = textColor
            ),
            cursorBrush = SolidColor(cursorColor),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 6.dp)
        )
    }
}

/**
 * Pill-shaped action button with a glass-tint on top of the brand gradient —
 * matches the Dashboard's GoalCtaButton style.
 */
@Composable
private fun SaveActionButton(
    heroBrush: Brush,
    actionLabel: String,
    isAdd: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.45f,
        animationSpec = tween(durationMillis = 180),
        label = "save-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(heroBrush)
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .clickableNoRipple(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle inner pill highlight, mirrors dashboard's white@0.18 pills
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.10f * animatedAlpha))
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
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

@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    content: @Composable () -> Unit
) {
    val bgBrush = remember(primary) {
        if (primary) Brush.horizontalGradient(colors = listOf(Indigo500, Violet400))
        else null
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (bgBrush != null) Modifier.background(bgBrush)
                else Modifier.background(
                    if (enabled) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            )
            .border(
                1.dp,
                if (primary) Color.White.copy(alpha = 0.25f)
                else if (enabled) MaterialTheme.colorScheme.outlineVariant
                else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}