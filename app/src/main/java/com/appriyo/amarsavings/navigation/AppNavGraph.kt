package com.appriyo.amarsavings.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appriyo.amarsavings.data.auth.AuthRepository
import com.appriyo.amarsavings.data.auth.AuthState
import com.appriyo.amarsavings.data.backup.BackupScheduler
import com.appriyo.amarsavings.ui.dashboard.DashboardScreen
import com.appriyo.amarsavings.ui.history.HistoryScreen
import com.appriyo.amarsavings.ui.settings.SettingsScreen
import com.appriyo.amarsavings.ui.signin.SignInScreen
import com.appriyo.amarsavings.ui.components.RestoreLoadingScreen
import org.koin.compose.koinInject

sealed class Route(val path: String) {
    object Dashboard : Route("dashboard")
    object History : Route("history")
    object Settings : Route("settings")
    object SignIn : Route("signin")
    object Restore : Route("restore")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onToggleTheme: () -> Unit,
    auth: AuthRepository = koinInject(),
    scheduler: BackupScheduler = koinInject()
) {
    val authState by auth.state.collectAsState()
    val restoreOutcome by scheduler.restoreOutcome.collectAsState()

    val startDestination = when (authState) {
        is AuthState.Restoring -> Route.Restore.path
        else -> Route.Dashboard.path
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Route.Dashboard.path) {
            DashboardScreen(
                onViewAll = { navController.navigate(Route.History.path) },
                onToggleTheme = onToggleTheme,
                onOpenSettings = { navController.navigate(Route.Settings.path) },
                onOpenSignIn = { navController.navigate(Route.SignIn.path) }
            )
        }

        composable(Route.History.path) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onToggleTheme = onToggleTheme,
                onSignIn = {
                    navController.popBackStack()
                    navController.navigate(Route.SignIn.path)
                }
            )
        }

        composable(Route.SignIn.path) {
            SignInScreen(
                onSkip = { navController.popBackStack() },
                onSignedIn = {
                    // The SignInScreen already flips state to Restoring; we
                    // re-route to the dedicated restore loader so the user
                    // sees clear progress before landing on Dashboard.
                    navController.navigate(Route.Restore.path) {
                        popUpTo(Route.SignIn.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Restore.path) {
            // After restore completes (success or failure), transition to Dashboard.
            androidx.compose.runtime.LaunchedEffect(authState, restoreOutcome) {
                if (authState is AuthState.SignedIn) {
                    navController.navigate(Route.Dashboard.path) {
                        popUpTo(Route.Restore.path) { inclusive = true }
                    }
                }
            }
            RestoreLoadingScreen()
        }
    }
}