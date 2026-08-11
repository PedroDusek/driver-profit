package com.driverprofit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.driverprofit.feature.dashboard.DashboardScreen

/**
 * Grafo de navegação único do aplicativo (Single Activity + Navigation
 * Compose, PRD §3).
 *
 * Em v0.1.0 existe apenas o destino de dashboard; as telas de veículo,
 * ganhos e despesas entram nas versões seguintes do roadmap.
 */
@Composable
fun DriverProfitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DriverProfitDestination.START,
        modifier = modifier,
    ) {
        composable(route = DriverProfitDestination.DASHBOARD) {
            DashboardScreen()
        }
    }
}
