package com.appriyo.amarsavings.ui.signin

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.auth.GoogleAuthClient
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.data.backup.RestoreOutcome
import com.appriyo.amarsavings.data.db.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 *   3. On success, state becomes [AuthState.Restoring] and the UI navigates
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

    /** Step 1: start One Tap. Caller must observe [pendingIntent] and launch it. */
    fun beginOneTap() {
        if (_inFlight.value) return
        viewModelScope.launch {
            _inFlight.value = true
            runCatching { client.beginSignIn() }
                .onSuccess { _pendingIntent.value = it }
                .onFailure { auth.state.let { /* swallow; signIn reports it */ } }
        }
    }

    /** Step 2: One Tap returned a result Intent. Persist profile + start Drive auth. */
    fun handleOneTapResult(data: Intent?) {
        viewModelScope.launch {
            _pendingIntent.value = null
            val credential = runCatching { client.handleActivityResult(data) }
                .onFailure { /* handled below via Error state */ }
                .getOrNull() ?: run {
                    auth.broadcastError("Sign-in cancelled.")
                    _inFlight.value = false
                    return@launch
                }
            val email = client.extractEmailFromIdToken(credential) ?: credential.id
            val displayName = credential.displayName
            val photoUrl = credential.profilePictureUri?.toString()
            if (email == null) {
                auth.broadcastError("Could not read your Google account email.")
                _inFlight.value = false
                return@launch
            }
            prefs.setUserProfile(email, displayName, photoUrl)
            // Now request drive.appdata authorization and flip state.
            auth.signInWithCachedActivity()
            _inFlight.value = false
        }
    }

    fun clearPendingIntent() { _pendingIntent.value = null }

    fun dismissError() { auth.clearError() }
}