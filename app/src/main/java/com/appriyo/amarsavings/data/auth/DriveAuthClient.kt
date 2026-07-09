@file:Suppress("DEPRECATION")

package com.appriyo.amarsavings.data.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.util.Log
import com.appriyo.amarsavings.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles ONLY the `drive.appdata` OAuth authorization used for cloud backup.
 *
 * User identity (email / display name / photo) now comes from Firebase Auth
 * via [FirebaseAuthClient]. This class doesn't know or care who the user is —
 * it just gets an access token scoped to the app's hidden Drive folder, via
 * the Identity Authorization API (com.google.android.gms.auth.api.identity),
 * which is a separate system from Firebase Auth / Credential Manager.
 */
class DriveAuthClient {

    @Volatile
    private var currentToken: String? = null

    /** Returns the cached Drive access token, or null. Read by [com.appriyo.amarsavings.data.backup.DriveBackupClient]. */
    fun getAccessToken(): String? = currentToken

    sealed class DriveAuthOutcome {
        data class Authorized(val result: AuthorizationResult) : DriveAuthOutcome()
        data class NeedsResolution(val pendingIntent: android.app.PendingIntent) : DriveAuthOutcome()
    }

    /**
     * Requests the `drive.appdata` scope. If previously granted, this
     * resolves silently with a cached token. Otherwise it returns
     * [DriveAuthOutcome.NeedsResolution] with a PendingIntent the UI must
     * launch (see [completeDriveAuthorization]).
     */
    suspend fun authorizeDriveAccess(): DriveAuthOutcome {
        Log.d(
            AuthDebug.TAG,
            "authorizeDriveAccess() called, scope=$DRIVE_APP_DATA_SCOPE, clientId=${BuildConfig.GOOGLE_OAUTH_CLIENT_ID}"
        )
        ActivityHolder.requireActivity()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .requestOfflineAccess(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
            .build()
        val result = suspendCancellableCoroutine<AuthorizationResult> { cont ->
            Identity.getAuthorizationClient(ActivityHolder.currentActivity!!)
                .authorize(request)
                .addOnSuccessListener {
                    Log.d(
                        AuthDebug.TAG,
                        "authorize() SUCCESS, hasResolution=${it.hasResolution()}, grantedScopes=${it.grantedScopes}"
                    )
                    cont.resume(it)
                }
                .addOnFailureListener { t ->
                    AuthDebug.logFailure("authorizeDriveAccess", t)
                    cont.resumeWithException(t)
                }
        }
        return if (result.hasResolution()) {
            DriveAuthOutcome.NeedsResolution(result.pendingIntent!!)
        } else {
            currentToken = result.accessToken
            DriveAuthOutcome.Authorized(result)
        }
    }

    /** Called after the consent UI (launched from NeedsResolution) returns a result. */
    fun completeDriveAuthorization(data: Intent?): AuthorizationResult {
        val activity = ActivityHolder.requireActivity()
        return try {
            val result = Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(data)
            Log.d(AuthDebug.TAG, "completeDriveAuthorization() SUCCESS, grantedScopes=${result.grantedScopes}")
            currentToken = result.accessToken
            result
        } catch (t: Throwable) {
            AuthDebug.logFailure("completeDriveAuthorization", t)
            throw t
        }
    }

    /** Clears the in-memory Drive access token only. Does not touch Firebase Auth's session. */
    fun clearToken() {
        currentToken = null
    }

    /**
     * Whether a foreground [Activity] is currently attached to [ActivityHolder].
     *
     * Callers that want to invoke [authorizeDriveAccess] (directly or via
     * `AuthRepository.ensureDriveAccess`) should check this first and treat a
     * `false` result as "app is not in the foreground, can't drive a consent
     * sheet right now" rather than as an error. Without this guard, a
     * backgrounded auto-upload tick or process teardown will throw out of
     * [ActivityHolder.requireActivity] and surface as a confusing
     * "Drive authorization required" failure.
     */
    fun hasActivityForeground(): Boolean = ActivityHolder.currentActivity != null

    companion object {
        /** Required scope for the app's hidden appDataFolder. */
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

/**
 * Holds a reference to the foreground Activity so the Identity Authorization
 * API (which needs an Activity for its consent UI) can use it. Set this from
 * the Activity's lifecycle and cleared on `onDispose` to avoid leaks.
 */
@SuppressLint("StaticFieldLeak")
internal object ActivityHolder {
    @Volatile
    var currentActivity: Activity? = null

    fun requireActivity(): Activity = currentActivity
        ?: error("ActivityHolder has no Activity — set it before requesting Drive access")
}