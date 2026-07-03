package com.appriyo.amarsavings.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appriyo.amarsavings.ui.dashboard.DashboardScreen
import com.appriyo.amarsavings.ui.history.HistoryScreen

sealed class Route(val path: String) {
    object Dashboard : Route("dashboard")
    object History : Route("history")
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onToggleTheme: () -> Unit
) {
    NavHost(navController = navController, startDestination = Route.Dashboard.path) {
        composable(Route.Dashboard.path) {
            DashboardScreen(
                onViewAll = { navController.navigate(Route.History.path) },
                onToggleTheme = onToggleTheme
            )
        }
        composable(Route.History.path) {
            HistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}