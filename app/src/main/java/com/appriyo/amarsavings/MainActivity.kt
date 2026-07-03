package com.appriyo.amarsavings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
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
            val themeMode by prefs.themeMode.collectAsState(initial = AppPreferences.THEME_LIGHT)
            val isDark = decodeThemeMode(themeMode)
            val scope = rememberCoroutineScope()

            AmarSavingsTheme(darkTheme = isDark) {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    onToggleTheme = {
                        scope.launch {
                            prefs.setThemeMode(
                                if (isDark) AppPreferences.THEME_LIGHT else AppPreferences.THEME_DARK
                            )
                        }
                    }
                )
            }
        }
    }
}