package com.appriyo.amarsavings.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.BuildConfig
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.backup.BackupState
import com.appriyo.amarsavings.ui.components.clickableNoRipple
import com.appriyo.amarsavings.ui.theme.Indigo500
import com.appriyo.amarsavings.ui.theme.Mint500
import com.appriyo.amarsavings.util.formatRelativeTime
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onToggleTheme: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar by viewModel.snackbar.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbar) {
        val msg = snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.snackShown()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            TopRow(onBack)
            AccountSection(state, onSignIn, viewModel)
            Spacer(Modifier.height(20.dp))
            BackupSection(state, viewModel)
            Spacer(Modifier.height(20.dp))
            AppearanceSection(onToggleTheme)
            Spacer(Modifier.height(20.dp))
            AboutSection()
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Back")
            }
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(48.dp))
        }
    }
}

@Composable
private fun AccountSection(
    state: SettingsUiState,
    onSignIn: () -> Unit,
    viewModel: SettingsViewModel
) {
    SectionLabel("Account")
    when (val auth = state.authState) {
        is AuthState.SignedIn -> {
            Card {
                ProfileRow(email = auth.email, name = auth.displayName, photoUrl = auth.photoUrl)
                Divider()
                ActionRow(
                    icon = Icons.AutoMirrored.Rounded.Logout,
                    title = "Sign out",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { viewModel.signOut() }
                )
            }
        }
        is AuthState.Restoring -> {
            Card {
                TextItem(
                    title = "Signing you in…",
                    subtitle = "Restoring your backup from Google Drive."
                )
            }
        }
        else -> {
            Card {
                ActionRow(
                    icon = Icons.Rounded.AccountCircle,
                    title = "Sign in with Google",
                    subtitle = "Back up your savings to your Google Drive",
                    tint = Indigo500,
                    onClick = onSignIn
                )
            }
        }
    }
}

@Composable
private fun BackupSection(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    SectionLabel("Backup")
    Card {
        val lastLabel = when {
            state.authState !is AuthState.SignedIn -> "Sign in to back up."
            state.lastBackupAt == 0L -> "No backup yet."
            else -> "Last backup ${formatRelativeTime(state.lastBackupAt)}."
        }
        InfoRow(
            icon = Icons.Rounded.Cloud,
            title = "Google Drive",
            subtitle = lastLabel
        )
        Divider()
        ActionRow(
            icon = Icons.Rounded.Backup,
            title = "Back up now",
            enabled = state.authState is AuthState.SignedIn && state.backupState !is BackupState.Syncing,
            onClick = { viewModel.backupNow() }
        )
        Divider()
        ActionRow(
            icon = Icons.Rounded.Download,
            title = "Restore from Drive",
            enabled = state.authState is AuthState.SignedIn,
            onClick = { viewModel.restore() }
        )
    }
}

@Composable
private fun AppearanceSection(onToggleTheme: () -> Unit) {
    SectionLabel("Appearance")
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickableNoRipple(onClick = onToggleTheme)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.DarkMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Toggle light or dark mode.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.LightMode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutSection() {
    SectionLabel("About")
    Card {
        TextItem(
            title = "Amar Savings",
            subtitle = "Version ${BuildConfig.VERSION_NAME}"
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        Column { content() }
    }
}

@Composable
private fun ProfileRow(email: String, name: String?, photoUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple initials-based avatar (fallback for photoUrl - Coil would be
        // ideal but kept minimal so the file compiles without an extra dep).
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Indigo500),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = (name?.firstOrNull()?.uppercaseChar()?.toString()
                    ?: email.firstOrNull()?.uppercaseChar()?.toString() ?: "U"),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name ?: email,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Mint500
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TextItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(0.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}