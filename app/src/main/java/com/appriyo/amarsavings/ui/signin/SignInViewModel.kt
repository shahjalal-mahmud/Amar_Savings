package com.appriyo.amarsavings.ui.signin

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appriyo.amarsavings.data.auth.AuthDebug
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.data.backup.RestoreOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Google (Firebase) sign-in + Drive authorization + restore flow.
 *
 * Steps:
 *   1. [beginSignIn] launches the Credential Manager Google picker, signs
 *      into Firebase, then requests the `drive.appdata` scope.
 *   2. If Drive consent UI is needed, [driveConsentIntent] emits a
 *      PendingIntent the UI must launch; its result is forwarded into
 *      [handleDriveConsentResult].
 *   3. On success, state becomes [AuthState.Restoring] and the UI navigates
 *      to the restore loader (driven by [AuthRepository.state]).
 */
class SignInViewModel(
    private val auth: AuthRepository,
    private val scheduler: BackupScheduler
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state
    val restoreOutcome: StateFlow<RestoreOutcome> = scheduler.restoreOutcome

    private val _inFlight = MutableStateFlow(false)
    val inFlight: StateFlow<Boolean> = _inFlight.asStateFlow()

    val driveConsentIntent: StateFlow<android.app.PendingIntent?> = auth.driveConsentIntent

    /** [activityContext] must be an Activity context (Credential Manager needs it for the picker UI). */
    fun beginSignIn(activityContext: Context) {
        if (_inFlight.value) return
        Log.d(AuthDebug.TAG, "SignInViewModel.beginSignIn()")
        viewModelScope.launch {
            _inFlight.value = true
            auth.signIn(activityContext)
            _inFlight.value = false
        }
    }

    /** Drive consent UI (launched from [driveConsentIntent]) returned a result. */
    fun handleDriveConsentResult(data: Intent?) {
        viewModelScope.launch {
            auth.handleDriveConsentResult(data)
        }
    }

    fun clearDriveConsentIntent() = auth.clearDriveConsentIntent()

    fun dismissError() = auth.clearError()
}