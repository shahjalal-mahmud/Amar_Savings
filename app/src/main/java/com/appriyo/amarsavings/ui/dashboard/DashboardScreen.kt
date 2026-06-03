package com.appriyo.amarsavings.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.db.*
import com.appriyo.amarsavings.ui.components.CashInputBottomSheet
import com.appriyo.amarsavings.ui.components.GoalDialog
import com.appriyo.amarsavings.ui.components.TransactionItem
import com.appriyo.amarsavings.util.formatTaka
import com.appriyo.amarsavings.util.smartFormatDate
import com.appriyo.amarsavings.viewmodel.DashboardViewModel
import com.appriyo.amarsavings.viewmodel.TransactionViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onViewAll: () -> Unit) {
    val dashVm: DashboardViewModel = koinViewModel()
    val txVm: TransactionViewModel = koinViewModel()
    val state by dashVm.uiState.collectAsState()
    val txState by txVm.state.collectAsState()

    var showGoalDialog by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var sheetType by remember { mutableStateOf(TransactionType.ADD) }

    // Dismiss sheet when saved
    LaunchedEffect(txState.isSaved) {
        if (txState.isSaved) {
            showSheet = false
            txVm.resetSaved()
        }
    }

    Scaffold(
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
                .padding(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "আমার সেভিংস",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Rounded.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Savings overview card
            SavingsOverviewCard(
                totalSaved = state.totalSaved,
                goal = state.goal,
                remaining = state.remaining,
                progressFraction = state.progressFraction,
                progressPercent = state.progressPercent,
                onEditGoal = { showGoalDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Cash analytics
            if (state.noteDistribution.totalNotes() > 0) {
                CashAnalyticsCard(
                    distribution = state.noteDistribution,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Recent transactions
            Card(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onViewAll) {
                            Text("View All")
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (state.recentTransactions.isEmpty()) {
                        EmptyTransactionsHint()
                    } else {
                        state.recentTransactions.forEach { tx ->
                            TransactionItem(
                                transaction = tx,
                                onDelete = { dashVm.deleteTransaction(tx) }
                            )
                            if (tx != state.recentTransactions.last()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Goal dialog
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

    // Cash input bottom sheet
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
private fun SavingsOverviewCard(
    totalSaved: Long,
    goal: Long,
    remaining: Long,
    progressFraction: Float,
    progressPercent: Int,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Total Saved",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
            Text(
                text = totalSaved.formatTaka(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            if (goal > 0) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                    strokeCap = StrokeCap.Round
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Goal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = goal.formatTaka(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Remaining",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                        Text(
                            text = remaining.formatTaka(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onEditGoal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    if (goal > 0) Icons.Rounded.Edit else Icons.Rounded.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (goal > 0) "Edit Goal" else "Set Savings Goal",
                    fontWeight = FontWeight.SemiBold
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cash Analytics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${distribution.totalNotes()} notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Distribution grid
            val notes = distribution.asList()
            notes.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pair.forEach { (denom, qty) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "৳$denom",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "×$qty",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Fill odd slot
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EmptyTransactionsHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.SavingsOutlined,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Tap + to add your first savings",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "Withdraw",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            expanded = false
                            onWithdraw()
                        },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Withdraw")
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = "Add Cash",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    SmallFloatingActionButton(
                        onClick = {
                            expanded = false
                            onAdd()
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() }
            ) { isExpanded ->
                Icon(
                    if (isExpanded) Icons.Rounded.Close else Icons.Rounded.Add,
                    contentDescription = if (isExpanded) "Close" else "Add or Withdraw"
                )
            }
        }
    }
}