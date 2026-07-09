package com.appriyo.amarsavings.data.auth

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.appriyo.amarsavings.BuildConfig
import com.google.android.gms.common.api.ApiException
import java.security.MessageDigest

/**
 * Centralized, verbose logging for the Google sign-in / Drive-authorization
 * flow.
 *
 * [logEnvironment] / [logSigningCertFingerprints] print config that is
 * sensitive enough not to ship in release builds (OAuth client id, signing
 * certificate SHA-1). They are gated on `BuildConfig.LOG_AUTH_VERBOSE`,
 * which is true for debug builds and false for release.
 *
 * [logFailure] only logs exception details (statusCode, message, qualified
 * class name). It is intentionally NOT gated — exception detail is fine to
 * ship and is genuinely useful for diagnosing release-only bugs.
 *
 * Filter logcat with:
 *   adb logcat -s AmarAuth:V AndroidRuntime:E
 */
object AuthDebug {
    const val TAG = "AmarAuth"

    /**
     * Call once at app startup. Prints everything needed to diagnose a
     * misconfigured OAuth client:
     *  - the client ID actually baked into this build
     *  - the actual signing certificate SHA-1 of this installed APK, which
     *    MUST match the SHA-1 registered on the "Android" OAuth client in
     *    Google Cloud Console for this exact package name.
     *
     * No-op in build types where `BuildConfig.LOG_AUTH_VERBOSE == false`
     * (currently: release).
     */
    fun logEnvironment(context: Context) {
        if (!BuildConfig.LOG_AUTH_VERBOSE) return
        Log.i(TAG, "==================== AUTH ENV ====================")
        Log.i(TAG, "packageName = ${context.packageName}")
        Log.i(TAG, "versionName=${BuildConfig.VERSION_NAME} versionCode=${BuildConfig.VERSION_CODE}")
        Log.i(TAG, "BUILD_TYPE = ${BuildConfig.BUILD_TYPE}, DEBUG=${BuildConfig.DEBUG}")
        Log.i(TAG, "GOOGLE_OAUTH_CLIENT_ID = ${BuildConfig.GOOGLE_OAUTH_CLIENT_ID}")
        if (BuildConfig.GOOGLE_OAUTH_CLIENT_ID.startsWith("REPLACE_WITH")) {
            Log.e(TAG, "!!! GOOGLE_OAUTH_CLIENT_ID is still the PLACEHOLDER — local.properties was not picked up by this build !!!")
        }
        logSigningCertFingerprints(context)
        Log.i(TAG, "===================================================")
    }

    private fun logSigningCertFingerprints(context: Context) {
        if (!BuildConfig.LOG_AUTH_VERBOSE) return
        try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = info.signingInfo
                if (signingInfo?.hasMultipleSigners() == true) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo?.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures
            }

            if (signatures.isNullOrEmpty()) {
                Log.e(TAG, "No signing certificates found — cannot compute SHA-1.")
                return
            }

            signatures.forEachIndexed { index, sig ->
                val digest = MessageDigest.getInstance("SHA-1").digest(sig.toByteArray())
                val sha1 = digest.joinToString(":") { "%02X".format(it) }
                Log.i(TAG, "Signing cert[$index] SHA-1 = $sha1")
                Log.i(TAG, "  -> Compare this against the SHA-1 on the 'Android' OAuth client")
                Log.i(TAG, "     in Google Cloud Console for package ${context.packageName}")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to read signing certificate", t)
        }
    }

    /**
     * Logs full detail for any auth failure, including the [ApiException]
     * fields that a plain `t.message` normally hides.
     */
    fun logFailure(label: String, t: Throwable) {
        Log.e(TAG, "---- FAILURE at: $label ----", t)
        if (t is ApiException) {
            Log.e(TAG, "  statusCode        = ${t.statusCode}")
            Log.e(TAG, "  status.statusMessage = ${t.status.statusMessage}")
            Log.e(TAG, "  status.resolution = ${t.status.resolution}")
            Log.e(TAG, "  status.isCanceled = ${t.status.isCanceled}")
        }
        Log.e(TAG, "  message = ${t.message}")
        Log.e(TAG, "  class   = ${t::class.qualifiedName}")
    }
}