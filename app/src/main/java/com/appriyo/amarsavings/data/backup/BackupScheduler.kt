package com.appriyo.amarsavings.data.backup

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.repository.SavingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Watches the [SavingsRepository] dirty stream + the auth state + connectivity
 * and triggers debounced uploads / sign-in restores. Started once from
 * [com.appriyo.amarsavings.AmarSavingsApp].
 */
class BackupScheduler(
    private val context: Context,
    private val auth: AuthRepository,
    private val prefs: AppPreferences,
    private val backup: BackupRepository,
    private val savings: SavingsRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    private val _online = MutableStateFlow(currentlyOnline())
    val online: StateFlow<Boolean> = _online.asStateFlow()

    private val _restoreOutcome = MutableStateFlow<RestoreOutcome>(RestoreOutcome.Idle)
    val restoreOutcome: StateFlow<RestoreOutcome> = _restoreOutcome.asStateFlow()

    fun start() {
        if (started) return
        started = true

        // 1. Watch network connectivity.
        registerConnectivityCallback()

        // 2. When auth state transitions to Restoring → trigger a restore.
        scope.launch {
            auth.state.collect { s ->
                if (s is AuthState.Restoring) {
                    val outcome = backup.restoreIfAny()
                    _restoreOutcome.value = outcome
                    when (outcome) {
                        is RestoreOutcome.Restored -> auth.onRestoreComplete()
                        is RestoreOutcome.NoBackup -> auth.onRestoreComplete()
                        is RestoreOutcome.Failed -> auth.onRestoreFailed()
                        RestoreOutcome.Idle -> Unit
                    }
                }
            }
        }

        // 3. Debounced auto-backup: any local mutation + signed-in + online → wait 5s → upload.
        scope.launch {
            combine(
                savings.dirty,
                auth.state,
                _online
            ) { _, state, online -> state to online }
                .debounce(DEBOUNCE_MS)
                .collect { (state, online) ->
                    if (state is AuthState.SignedIn && online) {
                        runCatching { backup.uploadNow() }
                    }
                }
        }

        // 4. App-foreground: if local state has diverged from the last-synced hash,
        //    fire one upload. Uses [ProcessLifecycleOwner] so we don't keep the
        //    scheduler awake when the app is fully backgrounded.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                scope.launch { maybeUploadOnForeground() }
            }
        })
    }

    private suspend fun maybeUploadOnForeground() {
        val state = auth.state.first()
        if (state !is AuthState.SignedIn) return
        if (!_online.value) return
        val current = savings.computeLocalStateHash()
        val last = prefs.getLocalStateHashNow()
        if (current != last) {
            runCatching { backup.uploadNow() }
        }
    }

    fun uploadNow() {
        scope.launch { runCatching { backup.uploadNow() } }
    }

    private fun registerConnectivityCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        val req = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(req, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { _online.value = true }
            override fun onLost(network: Network) {
                // re-evaluate in case another network is still up
                _online.value = currentlyOnline()
            }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    _online.value = true
                }
            }
        })
    }

    private fun currentlyOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return true // assume online if we can't tell — let upload fail cleanly
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        const val DEBOUNCE_MS = 5_000L
    }
}