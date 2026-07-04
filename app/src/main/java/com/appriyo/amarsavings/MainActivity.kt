package com.appriyo.amarsavings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import com.appriyo.amarsavings.data.auth.ActivityHolder
import com.appriyo.amarsavings.data.db.AppPreferences
import com.appriyo.amarsavings.navigation.AppNavGraph
import com.appriyo.amarsavings.ui.theme.AmarSavingsTheme
import com.appriyo.amarsavings.ui.theme.decodeThemeMode
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs: AppPreferences = koinInject()
            val scope = rememberCoroutineScope()

            // Google Sign-In needs the current Activity to launch the
            // account chooser. We expose it once we're resumed and clear
            // the reference in onDispose to avoid leaks.
            DisposableEffect(this) {
                ActivityHolder.currentActivity = this@MainActivity
                onDispose {
                    if (ActivityHolder.currentActivity === this@MainActivity) {
                        ActivityHolder.currentActivity = null
                    }
                }
            }

            val themeMode by prefs.themeMode.collectAsState(initial = AppPreferences.THEME_LIGHT)
            val isDark = decodeThemeMode(themeMode)

            AmarSavingsTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    onToggleTheme = {
                        val next =
                            if (isDark) AppPreferences.THEME_LIGHT else AppPreferences.THEME_DARK
                        scope.launch { prefs.setThemeMode(next) }
                    }
                )
            }
        }
    }
}