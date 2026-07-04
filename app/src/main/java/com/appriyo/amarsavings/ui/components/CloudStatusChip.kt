package com.appriyo.amarsavings.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.backup.BackupState
import com.appriyo.amarsavings.ui.theme.Amber400
import com.appriyo.amarsavings.ui.theme.Amber500
import com.appriyo.amarsavings.ui.theme.Coral400
import com.appriyo.amarsavings.ui.theme.Coral500
import com.appriyo.amarsavings.ui.theme.Indigo500
import com.appriyo.amarsavings.ui.theme.Mint400
import com.appriyo.amarsavings.ui.theme.Mint500

/**
 * Tiny pill in the Dashboard header showing the user's Google Drive backup
 * status. Different colours for synced / syncing / failed / offline / signed-out.
 */
@Composable
fun CloudStatusChip(
    authState: AuthState,
    backupState: BackupState,
    online: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (label, icon, tint, bg) = chipData(authState, backupState, online)

    val animatedBg by animateColorAsState(bg, label = "chipBg")
    val animatedTint by animateColorAsState(tint, label = "chipTint")

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(animatedBg)
            .clickableNoRipple(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            AnimatedContent(
                targetState = icon,
                label = "chipIcon",
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) }
            ) { ic ->
                Icon(
                    imageVector = ic,
                    contentDescription = null,
                    tint = animatedTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = animatedTint,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class ChipData(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val bg: Color
)

@Composable
private fun chipData(
    authState: AuthState,
    backupState: BackupState,
    online: Boolean
): ChipData {
    return when {
        authState !is AuthState.SignedIn -> ChipData(
            label = "Sign in to back up",
            icon = Icons.Rounded.CloudQueue,
            tint = Indigo500,
            bg = Indigo500.copy(alpha = 0.10f)
        )
        backupState is BackupState.Syncing -> ChipData(
            label = "Syncing…",
            icon = Icons.Rounded.CloudSync,
            tint = Amber500,
            bg = Amber400.copy(alpha = 0.18f)
        )
        backupState is BackupState.Failed -> ChipData(
            label = "Tap to retry",
            icon = Icons.Rounded.Refresh,
            tint = Coral500,
            bg = Coral400.copy(alpha = 0.18f)
        )
        !online -> ChipData(
            label = "Offline",
            icon = Icons.Rounded.CloudOff,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            bg = MaterialTheme.colorScheme.surfaceVariant
        )
        backupState is BackupState.SyncedAt -> ChipData(
            label = "Synced",
            icon = Icons.Rounded.CloudDone,
            tint = Mint500,
            bg = Mint400.copy(alpha = 0.18f)
        )
        else -> ChipData(
            label = "Ready",
            icon = Icons.Rounded.CloudDone,
            tint = Mint500,
            bg = Mint400.copy(alpha = 0.18f)
        )
    }
}

/**
 * Small pulsing dot used inside chips for "live" activity feedback.
 */
@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse-alpha"
    )
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}