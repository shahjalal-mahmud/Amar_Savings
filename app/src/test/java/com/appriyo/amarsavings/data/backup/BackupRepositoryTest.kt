package com.appriyo.amarsavings.data.backup

import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.db.Transaction
import com.appriyo.amarsavings.data.db.TransactionType
import com.appriyo.amarsavings.data.repository.SavingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BackupRepository], focused on the upload + restore
 * orchestration and the Drive 401 retry-once path.
 *
 * `DriveBackupClient` is mocked as a whole — we're testing the repo's
 * orchestration around the Drive SDK, not the SDK itself (those tests live
 * elsewhere if/when they exist). `SavingsRepository` is also mocked; only
 * the snapshot/restore helpers the repo actually calls are stubbed.
 *
 * Note: [BackupRepository] does not take a `TransactionDao` — restore
 * writes go through `SavingsRepository.replaceAllTransactions(...)`, so
 * assertions verify that call rather than a DAO that the repo never sees.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRepositoryTest {

    private lateinit var authRepo: AuthRepository
    private lateinit var prefs: AppPreferences
    private lateinit var drive: DriveBackupClient
    private lateinit var savings: SavingsRepository

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val signedInState = MutableStateFlow<AuthState>(
        AuthState.SignedIn(
            uid = "u1",
            email = "u@example.com",
            displayName = "User",
            photoUrl = null
        )
    )

    @Before
    fun setUp() {
        authRepo = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        drive = mockk(relaxed = true)
        savings = mockk(relaxed = true)

        every { authRepo.state } returns signedInState

        coEvery { savings.getSavingsGoalSnapshot() } returns 100_000L
        coEvery { savings.getAllTransactionsSnapshot() } returns emptyList()
        coEvery { savings.computeLocalStateHash() } returns "hash-after-upload"
    }

    private fun newRepo(): BackupRepository = BackupRepository(
        auth = authRepo,
        prefs = prefs,
        drive = drive,
        savings = savings,
        json = json
    )

    // ─────────────────────────────────────────────────────────────────────
    // uploadNow() — happy path: prefs are updated on success
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `uploadNow success updates file-id, last-backup-at, and local-state-hash prefs`() = runTest {
        coEvery { authRepo.ensureDriveAccess() } returns true
        coEvery { drive.upload(any()) } returns Result.success("file-id-abc")

        val repo = newRepo()
        val result = repo.uploadNow()

        assertTrue("expected success, got $result", result.isSuccess)
        assertEquals(Unit, result.getOrThrow())

        // Prefs side effects in the exact order the repo performs them.
        coVerifyOrder {
            prefs.setDriveBackupFileId("file-id-abc")
            prefs.setLastBackupAt(any())
            prefs.setLocalStateHash("hash-after-upload")
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // uploadNow() — not signed in → immediate failure, no Drive call
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `uploadNow fails immediately when not signed in and does not touch Drive`() = runTest {
        signedInState.value = AuthState.SignedOut

        val repo = newRepo()
        val result = repo.uploadNow()

        assertTrue("expected failure", result.isFailure)
        assertTrue(
            "expected IllegalStateException, got ${result.exceptionOrNull()}",
            result.exceptionOrNull() is IllegalStateException
        )
        // No Drive or ensureDriveAccess attempt.
        coVerify(exactly = 0) { drive.upload(any()) }
        coVerify(exactly = 0) { authRepo.ensureDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // uploadNow() — ensureDriveAccess returned false → fail before Drive call
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `uploadNow fails when ensureDriveAccess returns false and does not call upload`() = runTest {
        coEvery { authRepo.ensureDriveAccess() } returns false

        val repo = newRepo()
        val result = repo.uploadNow()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { drive.upload(any()) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // restoreIfAny() — no backup file on Drive → NoBackup
    //
    // restoreIfAny() only proceeds when auth.state.value is Restoring (it's
    // called by BackupScheduler during that window, before SignedIn), so
    // the state must be flipped from the SignedIn default.
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `restoreIfAny returns NoBackup when Drive has no backup file`() = runTest {
        signedInState.value = AuthState.Restoring("u1", "u@example.com", "User")
        coEvery { authRepo.ensureDriveAccess() } returns true
        coEvery { drive.getMeta() } returns Result.success(null)

        val repo = newRepo()
        val outcome = repo.restoreIfAny()

        assertEquals(RestoreOutcome.NoBackup, outcome)
        // No download or transaction replacement in the no-backup path.
        coVerify(exactly = 0) { drive.download(any<String>()) }
        coVerify(exactly = 0) { savings.replaceAllTransactions(any()) }
    }

    // ─────────────────────────────────────────────────────────────────────
    // restoreIfAny() — backup exists → Restored with right count and
    // SavingsRepository.replaceAllTransactions called with those rows
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `restoreIfAny returns Restored with tx count and replaces transactions via SavingsRepository`() = runTest {
        signedInState.value = AuthState.Restoring("u1", "u@example.com", "User")

        val txs = listOf(
            Transaction(id = 1, type = TransactionType.ADD, timestamp = 1L,
                qty1000 = 2, totalAmount = 2000L, note = "a"),
            Transaction(id = 2, type = TransactionType.WITHDRAW, timestamp = 2L,
                qty500 = 1, totalAmount = 500L, note = "b"),
            Transaction(id = 3, type = TransactionType.ADD, timestamp = 3L,
                qty100 = 5, totalAmount = 500L, note = "c")
        )
        val backupFile = BackupFile(
            version = 1,
            createdAt = 12345L,
            userEmail = "u@example.com",
            savingsGoal = 100_000L,
            transactions = txs.map { it.toDto() }
        )
        coEvery { authRepo.ensureDriveAccess() } returns true
        coEvery { drive.getMeta() } returns Result.success(BackupMeta("file-id-1", null))
        coEvery { drive.download("file-id-1") } returns Result.success(backupFile)
        coEvery { savings.replaceAllTransactions(any()) } just runs

        val repo = newRepo()
        val outcome = repo.restoreIfAny()

        assertTrue(
            "expected Restored, got $outcome",
            outcome is RestoreOutcome.Restored
        )
        assertEquals(3, (outcome as RestoreOutcome.Restored).transactionCount)

        // The actual destructive swap goes through SavingsRepository, not a
        // DAO the repo doesn't hold — verify the entities it was handed.
        coVerify {
            savings.replaceAllTransactions(match { list ->
                list.size == 3 && list[0].id == 1L && list[2].id == 3L
            })
        }
        // Prefs side effects.
        coVerify { prefs.setSavingsGoal(100_000L) }
        coVerify { prefs.setLastBackupAt(12345L) }
        coVerify { prefs.setLocalStateHash(any()) }
        coVerify { prefs.setDriveBackupFileId("file-id-1") }
    }

    // ─────────────────────────────────────────────────────────────────────
    // uploadNow() — first call hits DriveTokenExpiredException, retry succeeds
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `uploadNow catches DriveTokenExpiredException, clears token, re-authorizes, and retries once`() = runTest {
        coEvery { authRepo.ensureDriveAccess() } returns true
        coEvery { authRepo.clearDriveToken() } just runs

        // First upload → 401, second upload → success.
        coEvery { drive.upload(any()) } returnsMany listOf(
            Result.failure(DriveTokenExpiredException(operation = "upload")),
            Result.success("file-id-after-retry")
        )

        val repo = newRepo()
        val result = repo.uploadNow()

        assertTrue("expected success after retry, got $result", result.isSuccess)

        // Order of operations on retry:
        // 1) first upload returns 401
        // 2) clearDriveToken
        // 3) ensureDriveAccess
        // 4) second upload (the retry) — succeeds
        coVerifyOrder {
            drive.upload(any())
            authRepo.clearDriveToken()
            authRepo.ensureDriveAccess()
            drive.upload(any())
        }
        // Exactly two upload calls (the 401 and the retry), not a loop.
        coVerify(exactly = 2) { drive.upload(any()) }
        // Prefs side effects reflect the successful retry's file id.
        coVerify { prefs.setDriveBackupFileId("file-id-after-retry") }
    }

    // ─────────────────────────────────────────────────────────────────────
    // uploadNow() — ensureDriveAccess returns false on retry path → fail cleanly
    //
    // ensureDriveAccess() is called TWICE in this flow: once as uploadNow's
    // initial guard (must succeed to reach Drive at all), and again inside
    // withDriveTokenRetry after the 401. Only the second call should fail
    // here — stubbing it to always return false would trip the initial
    // guard and drive.upload() would never be called.
    // ─────────────────────────────────────────────────────────────────────
    @Test
    fun `uploadNow fails cleanly with re-auth-needed message when ensureDriveAccess returns false on 401 retry`() = runTest {
        coEvery { authRepo.ensureDriveAccess() } returnsMany listOf(true, false)
        coEvery { authRepo.clearDriveToken() } just runs
        coEvery { drive.upload(any()) } returns Result.failure(
            DriveTokenExpiredException(operation = "upload")
        )

        val repo = newRepo()
        val result = repo.uploadNow()

        assertTrue(result.isFailure)
        val ex = result.exceptionOrNull()
        assertTrue("expected DriveTokenExpiredException, got $ex", ex is DriveTokenExpiredException)
        assertTrue(
            "expected re-grant message; got: ${ex?.message}",
            (ex?.message ?: "").contains("re-granted", ignoreCase = true)
        )
        // Initial guard passed (true), 401 happened once, retry guard
        // failed (false) — so exactly one upload call, not a retry.
        coVerify(exactly = 1) { drive.upload(any()) }
        coVerify(exactly = 2) { authRepo.ensureDriveAccess() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Mirror of `private fun Transaction.toDto()` in BackupRepository.kt so
    // the assertion above can refer to the same fields the production code
    // does (without round-tripping through serialization).
    // ─────────────────────────────────────────────────────────────────────
    private fun Transaction.toDto() = TransactionDto(
        id = id,
        type = type.name,
        timestamp = timestamp,
        qty1000 = qty1000, qty500 = qty500, qty200 = qty200,
        qty100 = qty100, qty50 = qty50, qty20 = qty20,
        qty10 = qty10, qty5 = qty5, qty2 = qty2, qty1 = qty1,
        totalAmount = totalAmount,
        note = note
    )
}