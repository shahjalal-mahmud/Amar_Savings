package com.appriyo.amarsavings.ui.signin

import android.app.PendingIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.ui.components.GoogleGIcon
import com.appriyo.amarsavings.ui.components.isDarkTheme
import com.appriyo.amarsavings.ui.theme.GradientHeroDark
import com.appriyo.amarsavings.ui.theme.GradientHeroLight
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignInScreen(
    onSkip: () -> Unit,
    onSignedIn: () -> Unit,
    viewModel: SignInViewModel = koinViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val inFlight by viewModel.inFlight.collectAsState()
    val pendingIntent by viewModel.pendingIntent.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // ActivityResultLauncher for Google One Tap PendingIntent.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.handleOneTapResult(result.data)
    }

    // Whenever the VM produces a PendingIntent, launch it.
    LaunchedEffect(pendingIntent) {
        val pi: PendingIntent = pendingIntent ?: return@LaunchedEffect
        launcher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
        viewModel.clearPendingIntent()
    }

    LaunchedEffect(authState) {
        when (val current = authState) {
            is AuthState.Error -> {
                snackbarHostState.showSnackbar(current.message)
                viewModel.dismissError()
            }
            is AuthState.Restoring -> onSignedIn()
            else -> Unit
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        SignInContent(
            inFlight = inFlight,
            onSignIn = { viewModel.beginOneTap() },
            onSkip = onSkip,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }
}

@Composable
private fun SignInContent(
    inFlight: Boolean,
    onSignIn: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = if (isDarkTheme()) GradientHeroDark else GradientHeroLight
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(Modifier.height(40.dp))
            HeroBadge(brush = brush)
            HeroHeadline()
            ValueProps()
            Spacer(Modifier.height(8.dp))
            GoogleSignInButton(
                inFlight = inFlight,
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = onSkip) {
                Text(
                    text = "Maybe later",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FooterLegal()
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HeroBadge(brush: Brush) {
    val transition = rememberInfiniteTransition(label = "hero-badge")
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero-badge-pulse"
    )
    Box(
        modifier = Modifier
            .size((96 * pulse).dp.coerceAtLeast(80.dp))
            .clip(CircleShape)
            .background(brush)
    ) {
        Icon(
            imageVector = Icons.Rounded.Savings,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .size(48.dp)
        )
    }
}

@Composable
private fun HeroHeadline() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Amar Savings",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Your savings. Always with you.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ValueProps() {
    val props = listOf(
        Triple(Icons.Rounded.CloudOff, "Works offline", "Use every feature without internet."),
        Triple(Icons.Rounded.Lock, "Secure on-device", "Your data stays encrypted on your phone."),
        Triple(Icons.Rounded.Cloud, "Optional Drive backup", "Sync to your own Google Drive.")
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            props.forEachIndexed { index, (icon, title, sub) ->
                ValuePropRow(icon, title, sub)
                if (index != props.lastIndex) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ValuePropRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(9.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun GoogleSignInButton(
    inFlight: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dark = isDarkTheme()
    val container = if (dark) MaterialTheme.colorScheme.surfaceVariant else Color.White
    val content = if (dark) MaterialTheme.colorScheme.onSurface else Color(0xFF1F1F1F)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Button(
            onClick = onClick,
            enabled = !inFlight,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = content
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            if (inFlight) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = content
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Connecting…",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                GoogleGIcon(size = 22.dp)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Continue with Google",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FooterLegal() {
    Text(
        text = "By continuing you agree to Google's sign-in terms.\nYour backup is stored privately in your own Drive.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}