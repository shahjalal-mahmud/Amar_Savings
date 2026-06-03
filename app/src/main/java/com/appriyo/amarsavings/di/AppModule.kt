package com.appriyo.amarsavings.di

import androidx.room.Room
import com.appriyo.amarsavings.data.db.AppDatabase
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.data.repository.SavingsRepository
import com.appriyo.amarsavings.viewmodel.DashboardViewModel
import com.appriyo.amarsavings.viewmodel.HistoryViewModel
import com.appriyo.amarsavings.viewmodel.TransactionViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }

    single { get<AppDatabase>().transactionDao() }
    single { AppPreferences(androidContext()) }
    single { SavingsRepository(get(), get()) }

    viewModel { DashboardViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { TransactionViewModel(get()) }
}