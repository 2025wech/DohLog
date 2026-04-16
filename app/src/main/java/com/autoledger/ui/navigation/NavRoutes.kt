package com.autoledger.ui.navigation

sealed class NavRoutes(val route: String) {
    object Login        : NavRoutes("login")
    object Debug : NavRoutes("debug")
    object Profile : NavRoutes("profile")
    object Permission   : NavRoutes("permission")
    object Dashboard    : NavRoutes("dashboard")
    object Transactions : NavRoutes("transactions")
    object Analytics    : NavRoutes("analytics")
    object Detail       : NavRoutes("detail/{transactionId}") {
        fun createRoute(transactionId: String) = "detail/$transactionId"
    }
}