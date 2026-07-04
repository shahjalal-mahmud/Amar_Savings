package com.appriyo.amarsavings.ui.dashboard

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.amarsavings.data.db.NoteDistribution
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.backup.BackupRepository
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.ui.components.CashInputBottomSheet
import com.appriyo.amarsavings.ui.components.GlassCard
import com.appriyo.amarsavings.ui.components.GoalDialog
import com.appriyo.amarsavings.ui.components.PulsingDot
import com.appriyo.amarsavings.ui.components.SignInBanner
import com.appriyo.amarsavings.ui.components.TransactionItem
import com.appriyo.amarsavings.ui.components.clickableNoRipple
import com.appriyo.amarsavings.ui.components.isDarkTheme
import com.appriyo.amarsavings.ui.theme.Amber400
import com.appriyo.amarsavings.ui.theme.Amber500
import com.appriyo.amarsavings.ui.theme.Coral400
import com.appriyo.amarsavings.ui.theme.Coral500
import com.appriyo.amarsavings.ui.theme.GradientHeroDark
import com.appriyo.amarsavings.ui.theme.GradientHeroLight
import com.appriyo.amarsavings.ui.theme.Indigo400
import com.appriyo.amarsavings.ui.theme.Indigo500
import com.appriyo.amarsavings.ui.theme.Mint400
import com.appriyo.amarsavings.ui.theme.Mint500
import com.appriyo.amarsavings.ui.theme.Violet400
import com.appriyo.amarsavings.ui.theme.White
import com.appriyo.amarsavings.util.formatTaka
import com.appriyo.amarsavings.viewmodel.DashboardViewModel
import com.appriyo.amarsavings.viewmodel.TransactionViewModel
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    onViewAll: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onOpenSignIn: () -> Unit = {}
) {
    val dashVm: DashboardViewModel = koinViewModel()
    val txVm: TransactionViewModel = koinViewModel()
    val authRepo: AuthRepository = koinInject()
    val backupRepo: BackupRepository = koinInject()
    val scheduler: BackupScheduler = koinInject()

    val state by dashVm.uiState.collectAsState()
    val txState by txVm.state.collectAsState()
    val authState by authRepo.state.collectAsState()
    val backupState by backupRepo.state.collectAsState()
    val isOnline by scheduler.online.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf(TransactionType.ADD) }

    LaunchedEffect(txState.isSaved) {
        if (txState.isSaved) {
            showSheet = false
            txVm.resetSaved()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            AddWithdrawFab(
                onAdd = {
                    txVm.initForAdd(TransactionType.ADD)
                    sheetType = TransactionType.ADD
                    showSheet = true
                },
                onWithdraw = {
                    txVm.initForAdd(TransactionType.WITHDRAW)
                    sheetType = TransactionType.WITHDRAW
                    showSheet = true
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            TopHeader(
                onToggleTheme = onToggleTheme,
                onOpenSettings = onOpenSettings,
                onOpenSignIn = onOpenSignIn,
                authState = authState,
                backupState = backupState,
                online = isOnline
            )

            HeroBalanceCard(
                totalSaved = state.totalSaved,
                goal = state.goal,
                remaining = state.remaining,
                progressFraction = state.progressFraction,
                progressPercent = state.progressPercent,
                onEditGoal = { showGoalDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            QuickStatsRow(
                totalSaved = state.totalSaved,
                goal = state.goal,
                totalNotes = state.noteDistribution.totalNotes(),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (state.noteDistribution.totalNotes() > 0) {
                CashAnalyticsCard(
                    distribution = state.noteDistribution,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Show the SignIn banner only when the user is signed out.
            if (authState !is com.appriyo.amarsavings.data.auth.AuthState.SignedIn) {
                SignInBanner(
                    onSignInClick = onOpenSignIn,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            RecentActivityCard(
                transactions = state.recentTransactions,
                onViewAll = onViewAll,
                onDelete = { dashVm.deleteTransaction(it) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            currentGoal = state.goal,
            onConfirm = {
                dashVm.setGoal(it)
                showGoalDialog = false
            },
            onDismiss = { showGoalDialog = false }
        )
    }

    if (showSheet) {
        CashInputBottomSheet(
            type = sheetType,
            denominations = txState.denominations,
            totalAmount = txState.totalAmount,
            onIncrement = { txVm.incrementQuantity(it) },
            onDecrement = { txVm.decrementQuantity(it) },
            onQuantityChange = { denom, qty -> txVm.updateQuantity(denom, qty) },
            onSave = { txVm.save() },
            onClearAll = { txVm.clearAll() },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun TopHeader(
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSignIn: () -> Unit,
    authState: com.appriyo.amarsavings.data.auth.AuthState,
    backupState: com.appriyo.amarsavings.data.backup.BackupState,
    online: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Amar Savings",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "My Savings",
                style = MaterialTheme.typography.headlineMedium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackupStatusIcon(
                authState = authState,
                backupState = backupState,
                online = online,
                onClick = {
                    if (authState is com.appriyo.amarsavings.data.auth.AuthState.SignedIn) {
                        onOpenSettings()
                    } else {
                        onOpenSignIn()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            ThemeToggleButton(onClick = onToggleTheme)
            Spacer(Modifier.width(8.dp))
            SettingsButton(onClick = onOpenSettings)
        }
    }
}

@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Rounded.Settings,
            contentDescription = "Settings"
        )
    }
}

/**
 * Compact icon-only backup indicator for the dashboard top bar.
 *
 * Visually identical to [ThemeToggleButton] (a 42 dp circular pill) so the
 * three right-aligned icons sit on a consistent baseline, but the icon and
 * tint change with the user's Google Drive backup state:
 *   - Signed-out / failed / not-enabled → red [Icons.Rounded.CloudOff]
 *   - Signed-in & synced / ready        → green [Icons.Rounded.CloudDone]
 *   - Syncing                            → amber [Icons.Rounded.CloudSync] w/ pulse
 *   - Offline (signed-in)                → neutral grey [Icons.Rounded.CloudOff]
 */
@Composable
private fun BackupStatusIcon(
    authState: com.appriyo.amarsavings.data.auth.AuthState,
    backupState: com.appriyo.amarsavings.data.backup.BackupState,
    online: Boolean,
    onClick: () -> Unit
) {
    val isDark = isDarkTheme()
    val (icon, tint, bgAlpha, contentDescription) = when {
        // Backup is off (no auth or auth failed) → red cross.
        authState !is com.appriyo.amarsavings.data.auth.AuthState.SignedIn ->
            BackupIconSpec(Icons.Rounded.CloudOff, Coral500, 0.18f, "Backup off")
        backupState is com.appriyo.amarsavings.data.backup.BackupState.Failed ->
            BackupIconSpec(Icons.Rounded.CloudOff, Coral500, 0.18f, "Backup failed")
        backupState is com.appriyo.amarsavings.data.backup.BackupState.Syncing ->
            BackupIconSpec(Icons.Rounded.CloudSync, Amber500, 0.18f, "Backing up")
        // Backup is on and good → green.
        backupState is com.appriyo.amarsavings.data.backup.BackupState.SyncedAt ->
            BackupIconSpec(Icons.Rounded.CloudDone, Mint500, 0.18f, "Backed up")
        // Offline (signed-in, no network) → neutral, not an alarm.
        !online ->
            BackupIconSpec(Icons.Rounded.CloudOff, MaterialTheme.colorScheme.onSurfaceVariant, 0.18f, "Offline")
        // Signed-in default / Idle → treat as "ready / protected".
        else ->
            BackupIconSpec(Icons.Rounded.CloudDone, Mint500, 0.18f, "Backup ready")
    }
    val baseTint = if (isDark) {
        tint.copy(alpha = (tint.alpha * 0.55f + 0.45f).coerceAtMost(1f))
    } else tint

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(baseTint.copy(alpha = bgAlpha))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = baseTint,
            modifier = Modifier.size(20.dp)
        )
        // Tiny amber dot in the corner while syncing, reusing the existing
        // PulsingDot component from CloudStatusChip.kt.
        if (backupState is com.appriyo.amarsavings.data.backup.BackupState.Syncing) {
            PulsingDot(
                color = Amber500,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
            )
        }
    }
}

private data class BackupIconSpec(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val bgAlpha: Float,
    val contentDescription: String
)

@Composable
private fun HeroBalanceCard(
    totalSaved: Long,
    goal: Long,
    remaining: Long,
    progressFraction: Float,
    progressPercent: Int,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()
    val heroBrush = if (isDark) GradientHeroDark else GradientHeroLight

    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer-x"
    )

    // Hoisted out of Canvas draw lambda so we don't reallocate every frame
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(heroBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                .padding(24.dp)
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
                        text = "TOTAL SAVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "৳ BDT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(
                targetState = totalSaved,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                },
                label = "balance-anim"
            ) { value ->
                Text(
                    text = value.formatTaka(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
            }

            if (goal > 0) {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${progressPercent}% to goal",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = remaining.formatTaka() + " to go",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HeroStat(
                        label = "GOAL",
                        value = goal.formatTaka(),
                        modifier = Modifier.weight(1f)
                    )
                    HeroStat(
                        label = "REMAINING",
                        value = remaining.formatTaka(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            GoalCtaButton(
                hasGoal = goal > 0,
                onClick = onEditGoal
            )
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoalCtaButton(
    hasGoal: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
            .clickableNoRipple(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (hasGoal) Icons.Rounded.Edit else Icons.Rounded.Flag,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (hasGoal) "Edit Goal" else "Set Savings Goal",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun QuickStatsRow(
    totalSaved: Long,
    goal: Long,
    totalNotes: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStat(
            icon = Icons.Rounded.Savings,
            iconTint = Mint400,
            label = "Saved",
            value = totalSaved.formatTaka(),
            modifier = Modifier.weight(1f)
        )
        QuickStat(
            icon = Icons.Rounded.Flag,
            iconTint = Amber400,
            label = "Goal",
            value = if (goal > 0) goal.formatTaka() else "—",
            modifier = Modifier.weight(1f)
        )
        QuickStat(
            icon = Icons.Rounded.AccountBalanceWallet,
            iconTint = Indigo400,
            label = "Notes",
            value = totalNotes.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()
    val iconBg = remember(iconTint, isDark) {
        Brush.linearGradient(
            colors = listOf(
                iconTint.copy(alpha = if (isDark) 0.18f else 0.14f),
                iconTint.copy(alpha = if (isDark) 0.10f else 0.08f)
            )
        )
    }

    GlassCard(modifier = modifier, cornerRadius = 20.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun CashAnalyticsCard(
    distribution: NoteDistribution,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()
    val sortedNotes = remember(distribution) {
        distribution.asList().sortedByDescending { it.first }
    }
    val maxQty = remember(sortedNotes) { sortedNotes.maxOfOrNull { it.second } ?: 1 }

    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cash Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Denomination distribution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${distribution.totalNotes()} notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sortedNotes.forEach { (denom, qty) ->
                    DenominationBar(
                        denom = denom,
                        qty = qty,
                        maxQty = maxQty,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
private fun DenominationBar(
    denom: Int,
    qty: Int,
    maxQty: Int,
    isDark: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (maxQty == 0) 0f else qty.toFloat() / maxQty.toFloat(),
        animationSpec = tween(700),
        label = "denom-progress"
    )

    val trackBrush = remember(isDark) {
        if (isDark) Brush.horizontalGradient(
            colors = listOf(Indigo400.copy(alpha = 0.30f), Violet400.copy(alpha = 0.30f))
        ) else Brush.horizontalGradient(
            colors = listOf(Indigo500.copy(alpha = 0.10f), Violet400.copy(alpha = 0.18f))
        )
    }
    val fillBrush = remember {
        Brush.horizontalGradient(colors = listOf(Indigo500, Violet400))
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(trackBrush)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "৳$denom",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "×$qty",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(fillBrush)
            )
        }
    }
}

@Composable
private fun RecentActivityCard(
    transactions: List<Transaction>,
    onViewAll: () -> Unit,
    onDelete: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth(), cornerRadius = 24.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Your latest transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onViewAll) {
                    Text(
                        "View all",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                EmptyTransactionsHint()
            } else {
                transactions.forEachIndexed { idx, tx ->
                    TransactionItem(
                        transaction = tx,
                        onDelete = { onDelete(tx) },
                        showDivider = idx != transactions.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsHint() {
    val isDark = isDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val bubbleBrush = remember(isDark, primary, secondary) {
        val alpha = if (isDark) 0.18f else 0.12f
        Brush.linearGradient(
            colors = listOf(primary.copy(alpha = alpha), secondary.copy(alpha = alpha))
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(bubbleBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Savings,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Tap + to add your first savings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddWithdrawFab(
    onAdd: () -> Unit,
    onWithdraw: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = isDarkTheme()

    val mainBrush = remember(isDark) {
        if (isDark) Brush.linearGradient(colors = listOf(Indigo400, Violet400))
        else Brush.linearGradient(colors = listOf(Indigo500, Violet400))
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FabAction(
                    label = "Withdraw",
                    icon = Icons.Rounded.ArrowOutward,
                    onClick = {
                        expanded = false
                        onWithdraw()
                    },
                    containerColor = Coral400,
                    onContainerColor = White
                )
                FabAction(
                    label = "Add Cash",
                    icon = Icons.Rounded.Add,
                    onClick = {
                        expanded = false
                        onAdd()
                    },
                    containerColor = Mint400,
                    onContainerColor = White
                )
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(mainBrush)
                .clickableNoRipple { expanded = !expanded }
                .border(1.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() }
            ) { isExpanded ->
                Icon(
                    if (isExpanded) Icons.Rounded.Close else Icons.Rounded.Add,
                    contentDescription = if (isExpanded) "Close" else "Add or Withdraw",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun FabAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    onContainerColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(containerColor)
                .clickableNoRipple(onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = onContainerColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ThemeToggleButton(onClick: () -> Unit) {
    val isDark = isDarkTheme()
    val containerBrush = remember(isDark) {
        if (isDark) Brush.linearGradient(
            colors = listOf(Indigo400.copy(alpha = 0.25f), Violet400.copy(alpha = 0.25f))
        ) else Brush.linearGradient(
            colors = listOf(Indigo500.copy(alpha = 0.10f), Violet400.copy(alpha = 0.15f))
        )
    }
    val (icon, description) = if (isDark) {
        Icons.Rounded.LightMode to "Switch to light mode"
    } else {
        Icons.Rounded.DarkMode to "Switch to dark mode"
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(containerBrush)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickableNoRipple(onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )
    }
}