package com.autoledger.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autoledger.ui.AppTheme
import com.autoledger.ui.screens.*
import com.autoledger.ui.viewmodels.AuthViewModel

@Composable
fun AutoledgerNavGraph(
    navController : NavHostController,
    onThemeChange : (AppTheme) -> Unit = {}
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val userPrefs by authViewModel.userPreferences.collectAsState()

    NavHost(
        navController    = navController,
        startDestination = NavRoutes.Login.route
    ) {
        // ── Login ─────────────────────────────────────────────────────
        composable(NavRoutes.Login.route) {
            LoginScreen(
                viewModel       = authViewModel,
                onAuthenticated = {
                    navController.navigate(NavRoutes.Permission.route) {
                        popUpTo(NavRoutes.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Permission ────────────────────────────────────────────────
        composable(NavRoutes.Permission.route) {
            PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(NavRoutes.Dashboard.route) {
                        popUpTo(NavRoutes.Permission.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ─────────────────────────────────────────────────
        composable(NavRoutes.Dashboard.route) {
            DashboardScreen(
                onTransactionClick = { id ->
                    navController.navigate(NavRoutes.Detail.createRoute(id))
                }
            )
        }

        // ── Transactions ──────────────────────────────────────────────
        composable(NavRoutes.Transactions.route) {
            TransactionsScreen(
                onTransactionClick = { id ->
                    navController.navigate(NavRoutes.Detail.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Analytics ─────────────────────────────────────────────────
        composable(NavRoutes.Analytics.route) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // ── Profile ───────────────────────────────────────────────────
        composable(NavRoutes.Profile.route) {
            ProfileScreen(
                onThemeChange = onThemeChange,
                onLogout      = {
                    authViewModel.logout()
                    navController.navigate(NavRoutes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ── Transaction detail ────────────────────────────────────────
        composable(
            route     = NavRoutes.Detail.route,
            arguments = listOf(
                navArgument("transactionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val id = backStackEntry.arguments
                ?.getString("transactionId") ?: return@composable
            TransactionDetailScreen(
                transactionId = id,
                onBack        = { navController.popBackStack() }
            )
        }
    }
}