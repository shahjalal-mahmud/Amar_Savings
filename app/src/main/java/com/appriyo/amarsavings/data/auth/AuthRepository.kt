package com.appriyo.amarsavings.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.appriyo.amarsavings.data.db.AppPreferences
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for the user's sign-in status.
 *
 * Identity (email / display name / photo, session persistence across app
 * restarts) is owned by Firebase Auth via [FirebaseAuthClient]. This class
 * layers on top of that:
 *  - the [AuthState] UI state machine (SignedOut / Restoring / SignedIn / Error)
 *  - the separate Drive `drive.appdata` authorization via [DriveAuthClient]
 */
class AuthRepository(
    private val firebaseAuthClient: FirebaseAuthClient,
    private val driveAuthClient: DriveAuthClient,
    private val prefs: AppPreferences
) {
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val _driveConsentIntent = MutableStateFlow<android.app.PendingIntent?>(null)
    val driveConsentIntent: StateFlow<android.app.PendingIntent?> = _driveConsentIntent.asStateFlow()

    init {
        // Firebase persists the session itself, so on process start we trust
        // FirebaseAuth.currentUser directly instead of our own profile cache.
        val profile = firebaseAuthClient.currentProfile()
        _state.value = if (profile != null) {
            AuthState.SignedIn(profile.uid, profile.email, profile.displayName, profile.photoUrl)
        } else {
            AuthState.SignedOut
        }
    }

    /**
     * Step 1: sign in with Google via Firebase (Credential Manager picker),
     * then kick off Drive `drive.appdata` authorization. On success, state
     * becomes [AuthState.Restoring], which
     * [com.appriyo.amarsavings.data.backup.BackupScheduler] observes to
     * trigger a restore.
     */
    suspend fun signIn(activityContext: Context) {
        Log.d(AuthDebug.TAG, "AuthRepository.signIn() start")
        runCatching { firebaseAuthClient.signIn(activityContext) }
            .onSuccess { profile ->
                prefs.setUserProfile(profile.email, profile.displayName, profile.photoUrl)
                requestDriveAccess(profile)
            }
            .onFailure { t ->
                AuthDebug.logFailure("AuthRepository.signIn", t)
                _state.value = AuthState.Error(humanReadable(t))
            }
    }

    private suspend fun requestDriveAccess(profile: FirebaseAuthClient.Profile) {
        runCatching { driveAuthClient.authorizeDriveAccess() }
            .onSuccess { outcome ->
                when (outcome) {
                    is DriveAuthClient.DriveAuthOutcome.NeedsResolution -> {
                        Log.d(AuthDebug.TAG, "Drive auth needs resolution UI, launching consent intent")
                        _driveConsentIntent.value = outcome.pendingIntent
                    }
                    is DriveAuthClient.DriveAuthOutcome.Authorized -> {
                        _state.value = AuthState.Restoring(profile.uid, profile.email, profile.displayName)
                    }
                }
            }
            .onFailure { t ->
                AuthDebug.logFailure("requestDriveAccess", t)
                _state.value = AuthState.Error(humanReadable(t))
            }
    }

    /** Step 2 (only if Drive auth needed resolution UI): consent result. */
    fun handleDriveConsentResult(data: Intent?) {
        Log.d(AuthDebug.TAG, "AuthRepository.handleDriveConsentResult() start")
        _driveConsentIntent.value = null
        val profile = firebaseAuthClient.currentProfile()
        if (profile == null) {
            _state.value = AuthState.Error("Signed out before Drive consent completed.")
            return
        }
        runCatching { driveAuthClient.completeDriveAuthorization(data) }
            .onSuccess {
                _state.value = AuthState.Restoring(profile.uid, profile.email, profile.displayName)
            }
            .onFailure { t ->
                AuthDebug.logFailure("handleDriveConsentResult", t)
                _state.value = AuthState.Error(humanReadable(t))
            }
    }

    /**
     * Ensures we hold a Drive access token before an upload/download. Firebase
     * sessions persist across restarts, but the Drive access token is only
     * cached in memory — so on a fresh process we may need to (silently,
     * usually) re-request it. Called by
     * [com.appriyo.amarsavings.data.backup.BackupRepository]. If consent UI
     * turns out to be needed, it's surfaced via [driveConsentIntent] and this
     * returns false so the caller can bail out for now.
     */
    suspend fun ensureDriveAccess(): Boolean {
        if (driveAuthClient.getAccessToken() != null) return true
        val outcome = runCatching { driveAuthClient.authorizeDriveAccess() }
            .onFailure { t -> AuthDebug.logFailure("ensureDriveAccess", t) }
            .getOrNull() ?: return false

        return when (outcome) {
            is DriveAuthClient.DriveAuthOutcome.Authorized -> true
            is DriveAuthClient.DriveAuthOutcome.NeedsResolution -> {
                _driveConsentIntent.value = outcome.pendingIntent
                false
            }
        }
    }

    fun clearDriveConsentIntent() { _driveConsentIntent.value = null }

    /** Called by [com.appriyo.amarsavings.data.backup.BackupRepository] after restore completes. */
    fun onRestoreComplete() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(current.uid, current.email, current.displayName, null)
        }
    }

    /** Called if restore fails — we still allow the user to use the app locally. */
    fun onRestoreFailed() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(current.uid, current.email, current.displayName, null)
        }
    }

    /** Signs out of Firebase, clears the Drive token, and wipes stored profile/backup prefs. */
    suspend fun signOut() {
        firebaseAuthClient.signOut()
        driveAuthClient.clearToken()
        prefs.clearAuthAndBackup()
        _state.value = AuthState.SignedOut
    }

    fun clearError() {
        if (_state.value is AuthState.Error) _state.value = AuthState.SignedOut
    }

    private fun humanReadable(t: Throwable): String = humanReadableAuthError(t)
}

/**
 * Maps sign-in / Drive-authorization failures into a short, user-readable
 * message. Full diagnostic detail is logged via [AuthDebug.logFailure] at the
 * point of failure — this only produces the string shown in the snackbar.
 */
internal fun humanReadableAuthError(t: Throwable): String = when (t) {
    is ApiException -> when (t.statusCode) {
        7 -> "Network error. Check your connection and try again."
        10 -> "Developer error — check OAuth client SHA-1 fingerprint / client ID configuration (code 10)."
        12501 -> "Sign-in cancelled."
        12502 -> "Sign-in already in progress."
        16 -> "Authorization failed (code 16). Check Logcat tag 'AmarAuth' for details."
        else -> "Google Sign-In failed (code ${t.statusCode})."
    }
    is GetCredentialCancellationException -> "Sign-in cancelled."
    is NoCredentialException -> "No Google account found on this device."
    is GetCredentialException -> "Sign-in failed: ${t.message ?: t.type}"
    else -> t.message ?: "Unknown sign-in error."
}