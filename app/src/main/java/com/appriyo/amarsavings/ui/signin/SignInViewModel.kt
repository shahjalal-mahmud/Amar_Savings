package com.appriyo.amarsavings.ui.signin

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.auth.GoogleAuthClient
import com.appriyo.amarsavings.data.auth.humanReadableAuthError
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.data.backup.RestoreOutcome
import com.appriyo.amarsavings.data.db.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the multi-step Google sign-in + Drive authorization + restore flow.
 *
 * Steps:
 *   1. [beginOneTap] starts the Google Sign-In One Tap flow and returns a
 *      [android.app.PendingIntent] the UI must launch via
 *      [androidx.activity.result.ActivityResultLauncher].
 *   2. The launcher result is forwarded into [handleOneTapResult] which
 *      extracts the profile, persists it, and calls Drive authorization.
 *   3. If the user hasn't yet granted the `drive.appdata` scope,
 *      [AuthRepository.driveConsentIntent] emits a PendingIntent the UI must
 *      launch; its result is forwarded into [handleDriveConsentResult].
 *   4. On success, state becomes [AuthState.Restoring] and the UI navigates
 *      to the dedicated restore loader (driven by [AuthRepository.state]).
 */
class SignInViewModel(
    private val auth: AuthRepository,
    private val client: GoogleAuthClient,
    private val prefs: AppPreferences,
    private val scheduler: BackupScheduler
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state
    val restoreOutcome: StateFlow<RestoreOutcome> = scheduler.restoreOutcome

    private val _inFlight = MutableStateFlow(false)
    val inFlight: StateFlow<Boolean> = _inFlight.asStateFlow()

    private val _pendingIntent = MutableStateFlow<android.app.PendingIntent?>(null)
    val pendingIntent: StateFlow<android.app.PendingIntent?> = _pendingIntent.asStateFlow()

    val driveConsentIntent: StateFlow<android.app.PendingIntent?> = auth.driveConsentIntent

    /** Step 1: start One Tap. Caller must observe [pendingIntent] and launch it. */
    fun beginOneTap() {
        if (_inFlight.value) return
        viewModelScope.launch {
            _inFlight.value = true
            runCatching { client.beginSignIn() }
                .onSuccess { _pendingIntent.value = it }
                .onFailure { t -> auth.broadcastError(humanReadableAuthError(t)) }
        }
    }

    /** Step 2: One Tap returned a result Intent. Persist profile + start Drive auth. */
    fun handleOneTapResult(data: Intent?) {
        viewModelScope.launch {
            _pendingIntent.value = null

            // data == null means the user dismissed the system account chooser —
            // a real cancellation. Anything else is a parse failure from the
            // Identity SDK (most commonly DEVELOPER_ERROR when the OAuth client
            // is misconfigured), and we want to surface the real reason rather
            // than the misleading "Sign-in cancelled." string.
            if (data == null) {
                auth.broadcastError("Sign-in cancelled.")
                _inFlight.value = false
                return@launch
            }

            val credential = try {
                client.handleActivityResult(data)
            } catch (t: Throwable) {
                auth.broadcastError(humanReadableAuthError(t))
                _inFlight.value = false
                return@launch
            }
            if (credential == null) {
                auth.broadcastError("Sign-in cancelled.")
                _inFlight.value = false
                return@launch
            }

            val email = client.extractEmailFromIdToken(credential) ?: credential.id
            val displayName = credential.displayName
            val photoUrl = credential.profilePictureUri?.toString()
            prefs.setUserProfile(email, displayName, photoUrl)
            // Now request drive.appdata authorization and flip state
            // (or, if consent is needed, emit driveConsentIntent for the UI).
            auth.signInWithCachedActivity()
            _inFlight.value = false
        }
    }

    /** Step 3b: the Drive consent UI (launched from [driveConsentIntent]) returned a result. */
    fun handleDriveConsentResult(data: Intent?) {
        viewModelScope.launch {
            auth.handleDriveConsentResult(data)
        }
    }

    fun clearDriveConsentIntent() = auth.clearDriveConsentIntent()

    fun clearPendingIntent() { _pendingIntent.value = null }

    fun dismissError() { auth.clearError() }
}