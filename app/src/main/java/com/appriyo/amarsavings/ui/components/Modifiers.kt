package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * Adds a no-ripple clickable handler. Useful for FABs, theme toggles, and
 * other places where Material's default ripple effect would clash with
 * custom gradients or animations.
 */
@Composable
fun Modifier.clickableNoRipple(
    onClick: () -> Unit
): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)

/** Same as [clickableNoRipple] but respects an `enabled` flag. */
@Composable
fun Modifier.clickableNoRipple(
    enabled: Boolean,
    onClick: () -> Unit
): Modifier = this.clickable(
    enabled = enabled,
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)