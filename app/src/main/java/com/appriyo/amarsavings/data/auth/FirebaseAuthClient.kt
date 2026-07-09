package com.appriyo.amarsavings.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.appriyo.amarsavings.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Handles Google identity via Credential Manager + Firebase Auth.
 *
 * This replaces the old One Tap (GoogleSignInClient) identity flow. Drive's
 * `drive.appdata` OAuth access token is a SEPARATE concern, still handled by
 * [DriveAuthClient] via the Identity Authorization API — Firebase Auth only
 * proves who the user is; it does not grant Drive API scopes.
 */
class FirebaseAuthClient(private val context: Context) {

    private val credentialManager by lazy { CredentialManager.create(context) }
    private val firebaseAuth: FirebaseAuth get() = FirebaseAuth.getInstance()

    data class Profile(
        val uid: String,
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    )

    /** The currently signed-in Firebase user, or null. Checked once at process start. */
    fun currentProfile(): Profile? = firebaseAuth.currentUser?.toProfile()

    /**
     * Launches the Google account picker via Credential Manager, then
     * exchanges the returned Google ID token for a Firebase session.
     *
     * [activityContext] MUST be an Activity context — Credential Manager
     * needs it to host the picker UI. Throws on cancellation/failure; the
     * caller (AuthRepository) maps that into [AuthState.Error].
     */
    suspend fun signIn(activityContext: Context): Profile {
        Log.d(AuthDebug.TAG, "FirebaseAuthClient.signIn() requesting Google credential")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_OAUTH_CLIENT_ID)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = credentialManager.getCredential(activityContext, request)
        return signInWithGoogleCredential(response)
    }

    private suspend fun signInWithGoogleCredential(response: GetCredentialResponse): Profile {
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type: ${credential::class.java.name}")
        }

        val googleIdTokenCredential = try {
            GoogleIdTokenCredential.createFrom(credential.data)
        } catch (e: GoogleIdTokenParsingException) {
            AuthDebug.logFailure("GoogleIdTokenCredential.createFrom", e)
            throw e
        }

        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        val user = signInToFirebase(firebaseCredential)

        Log.d(AuthDebug.TAG, "Firebase sign-in SUCCESS, uid=${user.uid}, email=${user.email}")

        // Prefer the FirebaseUser's own fields; fall back to the Google ID
        // token's claims for the very first frame before they've propagated.
        return user.toProfile() ?: Profile(
            uid = user.uid,
            email = googleIdTokenCredential.id,
            displayName = googleIdTokenCredential.displayName,
            photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
        )
    }

    private suspend fun signInToFirebase(credential: AuthCredential): FirebaseUser =
        suspendCancellableCoroutine { cont ->
            firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user == null) {
                        cont.resumeWithException(IllegalStateException("Firebase sign-in succeeded but user is null"))
                    } else {
                        cont.resume(user)
                    }
                }
                .addOnFailureListener { t ->
                    AuthDebug.logFailure("firebaseAuth.signInWithCredential", t)
                    cont.resumeWithException(t)
                }
        }

    /** Signs out of Firebase and clears Credential Manager's saved picker state. */
    suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { t ->
            AuthDebug.logFailure("credentialManager.clearCredentialState", t)
        }
    }

    private fun FirebaseUser.toProfile(): Profile? {
        val mail = email ?: return null
        return Profile(uid = uid, email = mail, displayName = displayName, photoUrl = photoUrl?.toString())
    }
}