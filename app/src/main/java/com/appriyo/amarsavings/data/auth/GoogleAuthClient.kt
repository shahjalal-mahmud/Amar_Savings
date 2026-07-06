@file:Suppress("DEPRECATION")

package com.appriyo.amarsavings.data.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import com.appriyo.amarsavings.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Google Identity Services for Android.
 *
 * The flow is intentionally simple:
 *   1. [beginSignIn] returns a PendingIntent. We launch it via the
 *      ActivityResultLauncher registered in MainActivity.
 *   2. The result is an [Intent] we pass to [handleActivityResult] which
 *      extracts the [SignInCredential] (profile + Google ID token).
 *   3. [authorizeDriveAccess] uses the ID token's email to call
 *      [com.google.android.gms.auth.api.identity.AuthorizationClient.authorize]
 *      with the offline `drive.appdata` scope. If the user has already granted
 *      this scope in the past, the cached token is returned without UI.
 *   4. If they have not yet granted it, an authorization UI dialog appears
 *      once. The returned `accessToken` is cached in memory and used for all
 *      Drive REST calls.
 *
 * No Firebase dependency is required.
 */
class GoogleAuthClient(private val context: Context) {

    @Volatile
    private var currentToken: String? = null

    /**
     * Returns the cached Drive access token, or null if the user is not signed
     * in (or the token has been cleared by sign-out). Read by [DriveBackupClient]
     * on every Drive REST call.
     */
    fun getAccessToken(): String? = currentToken

    /** Returns a PendingIntent to launch the One Tap UI. */
    suspend fun beginSignIn(): android.app.PendingIntent {
        return suspendCancellableCoroutine { cont ->
            Identity.getSignInClient(context)
                .beginSignIn(buildSignInRequest())
                .addOnSuccessListener { result -> cont.resume(result.pendingIntent) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    /** Parses a result Intent returned from the launched One Tap PendingIntent. */
    suspend fun handleActivityResult(data: Intent?): SignInCredential? {
        if (data == null) return null
        return suspendCancellableCoroutine { cont ->
            val signInClient = Identity.getSignInClient(context)
            try {
                cont.resume(signInClient.getSignInCredentialFromIntent(data))
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }
    }

    sealed class DriveAuthOutcome {
        data class Authorized(val result: AuthorizationResult) : DriveAuthOutcome()
        data class NeedsResolution(val pendingIntent: android.app.PendingIntent) : DriveAuthOutcome()
    }

    suspend fun authorizeDriveAccess(): DriveAuthOutcome {
        ActivityHolder.requireActivity()
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
            .requestOfflineAccess(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
            .build()
        val result = suspendCancellableCoroutine<AuthorizationResult> { cont ->
            Identity.getAuthorizationClient(ActivityHolder.currentActivity!!)
                .authorize(request)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
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
        val result = Identity.getAuthorizationClient(activity)
            .getAuthorizationResultFromIntent(data)
        currentToken = result.accessToken
        return result
    }

    /**
     * Extracts the user's email from the JWT-formatted ID token. This is used
     * to persist the profile before the Drive authorization step runs.
     */
    fun extractEmailFromIdToken(credential: SignInCredential): String? {
        val idToken = credential.googleIdToken ?: credential.id
        // The JWT is `<header>.<payload>.<signature>` — we only need the payload.
        return runCatching {
            val parts = idToken.split('.')
            if (parts.size < 2) null else {
                val payload = String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE))
                JSONObject(payload).optString("email").takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    /** Clears all in-memory tokens and revokes access on Google's servers. */
    fun signOut() {
        currentToken = null
        runCatching { Identity.getSignInClient(context).signOut() }
    }

    private fun buildSignInRequest(): BeginSignInRequest =
        BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()

    companion object {
        /** Required scope for the app's hidden appDataFolder. */
        const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }
}

/**
 * Holds a reference to the foreground Activity so the underlying Google APIs
 * (which need an Activity for UI flows) can use it. Set this from the
 * Activity's lifecycle and cleared on `onDispose` to avoid leaks.
 */
@SuppressLint("StaticFieldLeak")
internal object ActivityHolder {
    @Volatile
    var currentActivity: Activity? = null

    fun requireActivity(): Activity = currentActivity
        ?: error("ActivityHolder has no Activity — set it before sign-in")
}
