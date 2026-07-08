package com.appriyo.amarsavings

import android.app.Application
import com.appriyo.amarsavings.data.auth.AuthDebug
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.di.appModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AmarSavingsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AmarSavingsApp)
            modules(appModule)
        }
        // Logs the OAuth client id and signing cert SHA-1 baked into this
        // exact build, so you can diff a working config against a broken one.
        AuthDebug.logEnvironment(this)
        // Start observing the data layer so the auto-backup pipeline is alive.
        get<BackupScheduler>().start()
    }
}