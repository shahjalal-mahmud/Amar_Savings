package com.appriyo.amarsavings.data.auth

/**
 * High-level state machine for the user's Google sign-in status.
 *
 * The local-first nature of the app means the user can always use it without
 * signing in. [SignedOut] is therefore a perfectly valid steady state.
 */
sealed class AuthState {
    object SignedOut : AuthState()

    /** Signed in but a restore from Drive is about to happen. */
    data class Restoring(val email: String, val displayName: String?) : AuthState()

    data class SignedIn(
        val email: String,
        val displayName: String?,
        val photoUrl: String?
    ) : AuthState()

    data class Error(val message: String) : AuthState()
}