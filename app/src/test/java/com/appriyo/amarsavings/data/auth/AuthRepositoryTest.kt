package com.appriyo.amarsavings.data.auth

import android.app.Activity
import android.app.PendingIntent
import app.cash.turbine.test
import com.appriyo.amarsavings.data.db.AppPreferences
import com.google.android.gms.auth.api.identity.AuthorizationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the [AuthRepository] state machine.
 *
 * `currentProfile()` is stubbed to return null on every test by default, so
 * `AuthRepository`'s `init` block leaves state at [AuthState.SignedOut] and
 * the rehydration coroutine is never launched. That keeps each test in
 * control of the starting state, instead of racing with a 2-second
 * `waitForActivity()` poll that the rehydration path performs.
 *
 * The repo's `scope` uses `Dispatchers.Main.immediate`; the
 * `UnconfinedTestDispatcher` is installed as the Main dispatcher so child
 * coroutines run inline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

    private lateinit var firebaseAuthClient: FirebaseAuthClient
    private lateinit var driveAuthClient: DriveAuthClient
    private lateinit var prefs: AppPreferences

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        // Plain JVM unit tests use the stub android.jar, whose android.util.Log
        // methods throw ("not mocked") instead of returning. AuthRepository
        // and AuthDebug call Log.d/e/w directly, so stub them out for every
        // test rather than pulling in Robolectric just for logging calls.
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.e(any(), any()) } returns 0
        every { android.util.Log.e(any(), any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0

        Dispatchers.setMain(testDispatcher)
        firebaseAuthClient = mockk(relaxed = false)
        // Drive client is relaxed for token/getter-style accessors that the
        // happy-path tests don't necessarily exercise; tighter stubs in
        // individual tests still take precedence.
        driveAuthClient = mockk(relaxed = true)
        prefs = mockk(relaxed = true)

        // Default: no Firebase session on cold start.
        every { firebaseAuthClient.currentProfile() } returns null
        every { driveAuthClient.getAccessToken() } returns null
        every { driveAuthClient.hasActivityForeground() } returns false
        ActivityHolder.currentActivity = null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        ActivityHolder.currentActivity = null
        unmockkStatic(android.util.Log::class)
    }

    private fun newRepo(): AuthRepository = AuthRepository(
        firebaseAuthClient = firebaseAuthClient,
        driveAuthClient = driveAuthClient,
        prefs = prefs
    )

    // ─────────────────────────────────────────────────────────────────────
    // Happy path: SignedOut → sign-in success → Drive Authorized → Restoring
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `signIn success path puts repo into Restoring when Drive auth is silently authorized`() = runTest {
        val profile = FirebaseAuthClient.Profile(
            uid = "uid-1",
            email = "user@example.com",
            displayName = "User",
            photoUrl = "https://example.com/p.png"
        )
        every { firebaseAuthClient.currentProfile() } returns null
        coEvery { firebaseAuthClient.signIn(any()) } returns profile

        val authResult = mockk<AuthorizationResult>(relaxed = true) {
            every { hasResolution() } returns false
        }
        coEvery { driveAuthClient.authorizeDriveAccess() } returns
                DriveAuthClient.DriveAuthOutcome.Authorized(authResult)

        val repo = newRepo()

        repo.state.test {
            // Initial state from init { } — currentProfile() is null.
            assertEquals(AuthState.SignedOut, awaitItem())

            repo.signIn(mockk(relaxed = true) /* Activity stub */)

            // After signIn (success) and requestDriveAccess (Authorized), the
            // state machine should emit Restoring.
            val restoring = awaitItem()
            assertTrue(
                "expected Restoring, got $restoring",
                restoring is AuthState.Restoring
            )
            val r = restoring as AuthState.Restoring
            assertEquals("uid-1", r.uid)
            assertEquals("user@example.com", r.email)
            assertEquals("User", r.displayName)

            cancelAndIgnoreRemainingEvents()
        }

        // Profile persisted to DataStore-backed prefs.
        coVerify { prefs.setUserProfile("user@example.com", "User", "https://example.com/p.png") }
        coVerify { driveAuthClient.authorizeDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sign-in failure → AuthState.Error
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `signIn failure puts repo into Error state`() = runTest {
        every { firebaseAuthClient.currentProfile() } returns null
        val ex = RuntimeException("boom")
        coEvery { firebaseAuthClient.signIn(any()) } throws ex

        val repo = newRepo()

        repo.state.test {
            assertEquals(AuthState.SignedOut, awaitItem())

            repo.signIn(mockk(relaxed = true))

            val error = awaitItem()
            assertTrue(
                "expected Error, got $error",
                error is AuthState.Error
            )
            // We don't pin the exact text (humanReadableAuthError mapping
            // can change), just that some message is set.
            assertTrue((error as AuthState.Error).message.isNotBlank())

            cancelAndIgnoreRemainingEvents()
        }

        // No Drive auth attempt should have happened.
        coVerify(exactly = 0) { driveAuthClient.authorizeDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Drive NeedsResolution → driveConsentIntent emission → consent success → Restoring
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `drive NeedsResolution surfaces consent intent and handleDriveConsentResult transitions to Restoring`() = runTest {
        val profile = FirebaseAuthClient.Profile(
            uid = "uid-2",
            email = "user2@example.com",
            displayName = "User2",
            photoUrl = null
        )
        every { firebaseAuthClient.currentProfile() } returnsMany listOf(null, profile)
        coEvery { firebaseAuthClient.signIn(any()) } returns profile

        val pendingIntent: PendingIntent = mockk(relaxed = true)
        // Drive authorization requires user consent
        coEvery { driveAuthClient.authorizeDriveAccess() } returns
                DriveAuthClient.DriveAuthOutcome.NeedsResolution(pendingIntent)
        // After consent completes, completeDriveAuthorization succeeds
        every { driveAuthClient.completeDriveAuthorization(any()) } returns mockk(relaxed = true)

        val repo = newRepo()
        val activityStub: android.content.Context = mockk(relaxed = true)

        repo.state.test {
            assertEquals(AuthState.SignedOut, awaitItem())

            // signIn → requestDriveAccess(NeedsResolution). State stays
            // SignedOut (only driveConsentIntent flips).
            repo.signIn(activityStub)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }

        // Consent intent is now published (it's a StateFlow so we read the
        // current value rather than race against the collector).
        assertEquals(pendingIntent, repo.driveConsentIntent.value)

        // Now feed the consent result back in.
        val consentDataIntent: android.content.Intent = mockk(relaxed = true)
        repo.handleDriveConsentResult(consentDataIntent)

        val item = repo.state.value
        assertTrue(
            "expected Restoring after consent, got $item",
            item is AuthState.Restoring && item.uid == "uid-2"
        )
        // After handleDriveConsentResult the repo also clears its own
        // driveConsentIntent (UI consumed the system result).
        assertNull(repo.driveConsentIntent.value)
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sign-out → SignedOut and prefs cleared
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `signOut returns to SignedOut and clears Drive token and prefs`() = runTest {
        val profile = FirebaseAuthClient.Profile(
            uid = "uid-3",
            email = "u3@example.com",
            displayName = "U3",
            photoUrl = null
        )
        every { firebaseAuthClient.currentProfile() } returnsMany listOf(null, profile)
        coEvery { firebaseAuthClient.signIn(any()) } returns profile
        val authResult = mockk<AuthorizationResult>(relaxed = true) {
            every { hasResolution() } returns false
        }
        coEvery { driveAuthClient.authorizeDriveAccess() } returns
                DriveAuthClient.DriveAuthOutcome.Authorized(authResult)
        coEvery { firebaseAuthClient.signOut() } returns Unit
        every { driveAuthClient.getAccessToken() } returns "some-token"

        val repo = newRepo()

        // Drive it to Restoring via the signIn path for realism.
        repo.signIn(mockk(relaxed = true))
        val beforeSignOut = repo.state.value
        assertTrue(
            "expected Restoring pre-signOut, got $beforeSignOut",
            beforeSignOut is AuthState.Restoring
        )

        repo.signOut()

        // prefs cleared, drive token cleared, firebase signed out
        coVerify { prefs.clearAuthAndBackup() }
        verify { driveAuthClient.clearToken() }
        coVerify { firebaseAuthClient.signOut() }

        // State is SignedOut.
        assertEquals(AuthState.SignedOut, repo.state.value)
    }

    // ─────────────────────────────────────────────────────────────────────
    // ensureDriveAccess short-circuits when no Activity in foreground
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `ensureDriveAccess returns false quietly when no foreground Activity`() = runTest {
        every { driveAuthClient.getAccessToken() } returns null
        every { driveAuthClient.hasActivityForeground() } returns false
        val repo = newRepo()

        val ok = repo.ensureDriveAccess()

        assertEquals(false, ok)
        // Critically: authorizeDriveAccess must NOT be called — we'd be
        // throwing out of ActivityHolder.requireActivity otherwise.
        coVerify(exactly = 0) { driveAuthClient.authorizeDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ensureDriveAccess returns true when a token is already cached
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `ensureDriveAccess returns true immediately when a token is cached`() = runTest {
        every { driveAuthClient.getAccessToken() } returns "fresh-token"
        val repo = newRepo()

        val ok = repo.ensureDriveAccess()

        assertEquals(true, ok)
        // No re-authorization needed; none attempted.
        coVerify(exactly = 0) { driveAuthClient.authorizeDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helper assertions that the existing KDoc contracts aren't drifted.
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `initial state is SignedOut when no Firebase profile is cached`() = runTest {
        every { firebaseAuthClient.currentProfile() } returns null
        val repo = newRepo()
        assertEquals(AuthState.SignedOut, repo.state.value)
        assertNull(repo.driveConsentIntent.value)
    }

    // ─────────────────────────────────────────────────────────────────────
    // NEW: cold-start rehydration — cached Firebase session + Activity
    // already attached + silent Drive re-authorization succeeds. Repo
    // should sit in Restoring (BackupScheduler flips it to SignedIn later
    // via onRestoreComplete/onRestoreFailed, not covered here).
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `cold start with cached profile and foreground Activity stays Restoring after silent Drive auth`() = runTest {
        val profile = FirebaseAuthClient.Profile(
            uid = "uid-4",
            email = "u4@example.com",
            displayName = "U4",
            photoUrl = "https://example.com/u4.png"
        )
        // Session already persisted from a prior launch.
        every { firebaseAuthClient.currentProfile() } returns profile
        // Activity already attached (avoids the waitForActivity poll/delay).
        ActivityHolder.currentActivity = mockk<Activity>(relaxed = true)

        val authResult = mockk<AuthorizationResult>(relaxed = true) {
            every { hasResolution() } returns false
        }
        coEvery { driveAuthClient.authorizeDriveAccess() } returns
                DriveAuthClient.DriveAuthOutcome.Authorized(authResult)

        val repo = newRepo()

        val state = repo.state.value
        assertTrue(
            "expected Restoring, got $state",
            state is AuthState.Restoring && state.uid == "uid-4"
        )
        coVerify { driveAuthClient.authorizeDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // NEW: clearError() resets an Error state back to SignedOut, and is a
    // no-op for any other state.
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `clearError resets Error state to SignedOut`() = runTest {
        every { firebaseAuthClient.currentProfile() } returns null
        val ex = RuntimeException("boom")
        coEvery { firebaseAuthClient.signIn(any()) } throws ex

        val repo = newRepo()
        repo.signIn(mockk(relaxed = true))
        assertTrue(repo.state.value is AuthState.Error)

        repo.clearError()

        assertEquals(AuthState.SignedOut, repo.state.value)
    }
}