package com.appriyo.amarsavings.data.auth

import android.content.Intent
import com.appriyo.amarsavings.data.db.AppPreferences
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Single source of truth for the user's Google sign-in status.
 *
 * Combines the in-memory [GoogleAuthClient] token cache with the persistent
 * user profile in [AppPreferences] so the UI can react to sign-in / sign-out
 * transitions consistently.
 */
class AuthRepository(
    private val client: GoogleAuthClient,
    private val prefs: AppPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()
    private val _driveConsentIntent = MutableStateFlow<android.app.PendingIntent?>(null)
    val driveConsentIntent: StateFlow<android.app.PendingIntent?> = _driveConsentIntent.asStateFlow()

    init {
        // Re-hydrate from persistent storage on construction.
        scope.launch {
            val email = prefs.userEmail.first()
            val name = prefs.userDisplayName.first()
            val photo = prefs.userPhotoUrl.first()
            _state.value = if (email != null) {
                AuthState.SignedIn(email, name, photo)
            } else {
                AuthState.SignedOut
            }
        }
    }

    /**
     * Request Drive authorization using the Activity that was previously set
     * via [ActivityHolder.currentActivity]. Called by [SignInViewModel] after
     * the One Tap credential has been parsed and the user profile saved.
     */
    suspend fun signInWithCachedActivity() {
        runCatching {
            when (val outcome = client.authorizeDriveAccess()) {
                is GoogleAuthClient.DriveAuthOutcome.NeedsResolution -> {
                    _driveConsentIntent.value = outcome.pendingIntent
                }
                is GoogleAuthClient.DriveAuthOutcome.Authorized -> completeSignIn()
            }
        }.onFailure { t -> _state.value = AuthState.Error(humanReadable(t)) }
    }

    suspend fun handleDriveConsentResult(data: Intent?) {
        _driveConsentIntent.value = null
        runCatching {
            client.completeDriveAuthorization(data)
            completeSignIn()
        }.onFailure { t -> _state.value = AuthState.Error(humanReadable(t)) }
    }

    private suspend fun completeSignIn() {
        val email = prefs.userEmail.first()
            ?: throw IllegalStateException("Sign-in completed but no profile stored")
        val displayName = prefs.userDisplayName.first()
        _state.value = AuthState.Restoring(email, displayName)
    }

    fun clearDriveConsentIntent() { _driveConsentIntent.value = null }

    /** Emit an error without touching the Drive auth path (used by VM). */
    fun broadcastError(message: String) {
        _state.value = AuthState.Error(message)
    }

    /** Called by [BackupRepository] after the restore step completes. */
    fun onRestoreComplete() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(current.email, current.displayName, null)
        }
    }

    /** Called if restore fails — we still allow the user to use the app locally. */
    fun onRestoreFailed() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(current.email, current.displayName, null)
        }
    }

    /** Clears tokens and stored profile. Local Room data is untouched. */
    suspend fun signOut() {
        client.signOut()
        prefs.clearAuthAndBackup()
        _state.value = AuthState.SignedOut
    }

    fun clearError() {
        if (_state.value is AuthState.Error) _state.value = AuthState.SignedOut
    }

    private fun humanReadable(t: Throwable): String = humanReadableAuthError(t)
}

/**
 * Maps any [Throwable] thrown by the Google Identity Services APIs into a
 * short, user-readable message. Used by [AuthRepository] for state transitions
 * and by [SignInViewModel] to surface errors that would otherwise be swallowed
 * (e.g. a DEVELOPER_ERROR from a misconfigured OAuth client previously showed
 * up as the misleading "Sign-in cancelled").
 */
internal fun humanReadableAuthError(t: Throwable): String = when (t) {
    is ApiException -> when (t.statusCode) {
        7 -> "Network error. Check your connection and try again."
        10 -> "Developer error — your Google Cloud OAuth client may be missing the SHA-1 fingerprint for this app (code 10)."
        12501 -> "Sign-in cancelled."
        12502 -> "Sign-in already in progress."
        16 -> "Account has too few scopes. Please sign in again."
        else -> "Google Sign-In failed (code ${t.statusCode})."
    }
    else -> t.message ?: t::class.simpleName?.let { "Unknown sign-in error ($it)." }
        ?: "Unknown sign-in error."
}