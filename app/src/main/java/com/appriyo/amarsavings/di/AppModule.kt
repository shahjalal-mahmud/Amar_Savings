package com.appriyo.amarsavings.di

import androidx.room.Room
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.DriveAuthClient
import com.appriyo.amarsavings.data.auth.FirebaseAuthClient
import com.appriyo.amarsavings.data.backup.BackupRepository
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.data.backup.DriveBackupClient
import com.appriyo.amarsavings.data.db.AppDatabase
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.repository.SavingsRepository
import com.appriyo.amarsavings.ui.settings.SettingsViewModel
import com.appriyo.amarsavings.ui.signin.SignInViewModel
import com.appriyo.amarsavings.viewmodel.DashboardViewModel
import com.appriyo.amarsavings.viewmodel.HistoryViewModel
import com.appriyo.amarsavings.viewmodel.TransactionViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // ── Database / prefs ───────────────────────────────────────────────────
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    single { get<AppDatabase>().transactionDao() }
    single { AppPreferences(androidContext()) }

    // ── Domain repository ──────────────────────────────────────────────────
    single { SavingsRepository(get(), get()) }

    // ── Auth ───────────────────────────────────────────────────────────────
    single { FirebaseAuthClient(androidContext()) }
    single { DriveAuthClient() }
    single { AuthRepository(get(), get(), get()) }

    // ── Backup ─────────────────────────────────────────────────────────────
    single { OkHttpClientProvider.client }
    single { JsonProvider.json }
    single { DriveBackupClient(get(), get(), get()) }
    single { BackupRepository(get(), get(), get(), get(), get()) }
    single { BackupScheduler(androidContext(), get(), get(), get(), get()) }

    // ── ViewModels ─────────────────────────────────────────────────────────
    // Option A: Using the clean Constructor DSL (Recommended)
    viewModelOf(::DashboardViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::TransactionViewModel)
    viewModelOf(::SignInViewModel)
    viewModelOf(::SettingsViewModel)

}

/** OkHttp client configured for Drive REST calls. */
object OkHttpClientProvider {
    val client: OkHttpClient by lazy { DriveBackupClient.defaultHttpClient() }
}

/** Shared kotlinx.serialization Json instance. */
object JsonProvider {
    val json: Json by lazy { DriveBackupClient.defaultJson() }
}