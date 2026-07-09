package com.appriyo.amarsavings.data.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.appriyo.amarsavings.data.db.AppPreferences
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    // Repo-owned scope for the cold-start rehydration work. AuthRepository
    // is a Koin singleton tied to the process lifetime, so a SupervisorJob
    // is safe. signOut() cancels the scope for hygiene.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Re-entrancy guard for rehydrateSession(). Koin singleton construction
    // should only fire init() once, but be defensive.
    @Volatile private var rehydrationStarted = false

    // The most recent Firebase profile photoUrl we've seen. Preserved across
    // Restoring → SignedIn transitions (which would otherwise drop it) so the
    // rehydration path and the fresh-sign-in path both keep the avatar.
    @Volatile private var lastKnownPhotoUrl: String? = null

    init {
        // On cold start, the Firebase session may already be valid (the user
        // signed in on a prior launch). Previously this set state directly
        // to SignedIn, which skipped the restore-on-Drive step entirely. We
        // now start as Restoring so AppNavGraph routes to the Restore loader
        // from the first frame (no jarring flash) and BackupScheduler runs
        // its normal restoreIfAny path — see rehydrateSession().
        val profile = firebaseAuthClient.currentProfile()
        if (profile != null) {
            _state.value = AuthState.Restoring(profile.uid, profile.email, profile.displayName)
            rehydrateSession(profile)
        } else {
            _state.value = AuthState.SignedOut
        }
    }

    /**
     * On cold start with a persisted Firebase session, attempt to silently
     * re-acquire the Drive access token (cached if Play Services still has
     * it) and let the normal Restoring → SignedIn flow play out. If Drive
     * auth needs explicit consent (user revoked, or never granted), surface
     * the intent AND fall back to SignedIn so the user isn't blocked from
     // local use. If Drive auth throws (offline, etc.), fall back to
     // SignedIn and log via AuthDebug so it's diagnosable.
     */
    private fun rehydrateSession(profile: FirebaseAuthClient.Profile) {
        if (rehydrationStarted) return
        rehydrationStarted = true
        lastKnownPhotoUrl = profile.photoUrl

        scope.launch {
            // Identity Authorization API needs an Activity. MainActivity sets
            // ActivityHolder in its DisposableEffect, which runs AFTER
            // AmarSavingsApp.onCreate returns. Wait briefly for it.
            val activity = waitForActivity() ?: run {
                AuthDebug.logFailure(
                    "rehydrateSession",
                    IllegalStateException("No Activity within timeout; falling back to SignedIn")
                )
                transitionToSignedInIfRestoring(profile)
                return@launch
            }

            runCatching { driveAuthClient.authorizeDriveAccess() }
                .onSuccess { outcome ->
                    when (outcome) {
                        is DriveAuthClient.DriveAuthOutcome.Authorized -> {
                            // Token refreshed (or still cached). Stay in
                            // Restoring — BackupScheduler will run
                            // restoreIfAny and flip to SignedIn.
                        }
                        is DriveAuthClient.DriveAuthOutcome.NeedsResolution -> {
                            // User revoked Drive access, or never granted it.
                            // Surface the consent intent (in case any
                            // composable is ready to launch it) but don't
                            // block local use of the app.
                            _driveConsentIntent.value = outcome.pendingIntent
                            transitionToSignedInIfRestoring(profile)
                        }
                    }
                }
                .onFailure { t ->
                    AuthDebug.logFailure("rehydrateSession", t)
                    transitionToSignedInIfRestoring(profile)
                }
        }
    }

    /**
     * Flip Restoring → SignedIn only if state is still Restoring for the
     * SAME uid. The uid guard prevents a stale rehydration completion
     * (e.g. user signed out and back in with a different account during
     // the ~few-hundred-ms rehydration window) from clobbering a fresher
     // state.
     */
    private fun transitionToSignedInIfRestoring(profile: FirebaseAuthClient.Profile) {
        val current = _state.value
        if (current is AuthState.Restoring && current.uid == profile.uid) {
            _state.value = AuthState.SignedIn(
                profile.uid, profile.email, profile.displayName, lastKnownPhotoUrl
            )
        }
    }

    /**
     * The Identity Authorization API's authorize() needs an Activity to
     // attach its consent UI to. MainActivity sets ActivityHolder in its
     // DisposableEffect, which composes after Application.onCreate returns.
     // Wait up to ~2s (40 × 50ms) for it; if it never appears, the caller
     // falls back to SignedIn so the user isn't blocked.
     */
    private suspend fun waitForActivity(
        maxAttempts: Int = 40,
        delayMs: Long = 50
    ): Activity? {
        repeat(maxAttempts) {
            ActivityHolder.currentActivity?.let { return it }
            delay(delayMs)
        }
        return ActivityHolder.currentActivity
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
                lastKnownPhotoUrl = profile.photoUrl
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
        lastKnownPhotoUrl = profile.photoUrl
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

    /**
     * Drops the in-memory cached Drive access token so the next
     * [ensureDriveAccess] call is forced to (silently, in the common case)
     * re-acquire it. Used by the Drive 401 retry path in
     * [com.appriyo.amarsavings.data.backup.BackupRepository].
     */
    fun clearDriveToken() { driveAuthClient.clearToken() }

    /** Called by [com.appriyo.amarsavings.data.backup.BackupRepository] after restore completes. */
    fun onRestoreComplete() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(
                current.uid, current.email, current.displayName, lastKnownPhotoUrl
            )
        }
    }

    /** Called if restore fails — we still allow the user to use the app locally. */
    fun onRestoreFailed() {
        val current = _state.value
        if (current is AuthState.Restoring) {
            _state.value = AuthState.SignedIn(
                current.uid, current.email, current.displayName, lastKnownPhotoUrl
            )
        }
    }

    /** Signs out of Firebase, clears the Drive token, and wipes stored profile/backup prefs. */
    suspend fun signOut() {
        // Stop any in-flight rehydration so it can't re-flip state to
        // SignedIn after we set it to SignedOut.
        scope.cancel()
        firebaseAuthClient.signOut()
        driveAuthClient.clearToken()
        prefs.clearAuthAndBackup()
        lastKnownPhotoUrl = null
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