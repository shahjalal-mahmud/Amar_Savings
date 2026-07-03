package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.ui.theme.Gray25
import com.appriyo.amarsavings.ui.theme.Gray800
import com.appriyo.amarsavings.ui.theme.Gray850
import com.appriyo.amarsavings.ui.theme.White

/**
 * Surface is considered "dark themed" when its surface color carries a YIQ luminance
 * lower than 0.5. Used by [GlassCard] and other theme-aware components that need
 * to pick between dark/light variants without threading an explicit `isDark` flag.
 */
@Composable
@ReadOnlyComposable
fun isDarkTheme(): Boolean {
    val bg = MaterialTheme.colorScheme.background
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    return luminance < 0.5f
}

/**
 * A premium "glass" surface used throughout the app for cards.
 *
 * - Subtle gradient backdrop
 * - Soft top-edge highlight stroke
 * - Adaptive to light + dark themes (inferred via [isDarkTheme])
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    val isDark = isDarkTheme()

    val containerBrush = remember(isDark) {
        if (isDark) Brush.linearGradient(
            colors = listOf(Gray800, Gray850)
        ) else Brush.linearGradient(
            colors = listOf(White, Gray25)
        )
    }

    val borderBrush = remember(isDark) {
        if (isDark) Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.02f)
            )
        ) else Brush.verticalGradient(
            colors = listOf(
                Color.Black.copy(alpha = 0.06f),
                Color.Black.copy(alpha = 0.01f)
            )
        )
    }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(containerBrush)
            .border(
                width = 1.dp,
                brush = borderBrush,
                shape = shape
            )
    ) {
        content()
    }
}