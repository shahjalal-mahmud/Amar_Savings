package com.appriyo.amarsavings

import android.app.Application
import com.appriyo.amarsavings.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AmarSavingsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AmarSavingsApp)
            modules(appModule)
        }
    }
}