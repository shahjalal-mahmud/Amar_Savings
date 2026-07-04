package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Brand-true Google "G" logo, rendered as a Compose vector so we don't need to
 * ship the official bitmap asset. Follows Google's published brand colors.
 *
 * The shape approximates the multi-color "G" used on Sign-In buttons.
 */
@Composable
fun GoogleGIcon(
    size: Dp = 20.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        drawGoogleG()
    }
}

private const val BLUE   = 0xFF4285F4.toInt()
private const val RED    = 0xFFEA4335.toInt()
private const val YELLOW = 0xFFFBBC05.toInt()
private const val GREEN  = 0xFF34A853.toInt()

private fun DrawScope.drawGoogleG() {
    val s = size.minDimension
    val stroke = s * 0.18f
    val r = s * 0.5f - stroke
    val c = Offset(size.width / 2f, size.height / 2f)

    // Blue: top arc
    drawArc(
        color = Color(BLUE),
        startAngle = -90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke)
    )
    // Red: left arc
    drawArc(
        color = Color(RED),
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke)
    )
    // Yellow: bottom arc
    drawArc(
        color = Color(YELLOW),
        startAngle = 90f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke)
    )
    // Green: right arc (smaller, ends at the G's "bar")
    drawArc(
        color = Color(GREEN),
        startAngle = 0f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke)
    )

    // Horizontal bar of the "G"
    val bar = Path().apply {
        moveTo(c.x, c.y)
        lineTo(c.x + r, c.y)
        lineTo(c.x + r, c.y - stroke * 0.5f)
        lineTo(c.x, c.y - stroke * 0.5f)
        close()
    }
    drawPath(bar, color = Color(GREEN))
}