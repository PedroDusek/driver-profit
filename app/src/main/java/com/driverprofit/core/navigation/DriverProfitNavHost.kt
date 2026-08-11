package com.driverprofit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.feature.dashboard.DashboardScreen
import com.driverprofit.feature.vehicle.form.VehicleFormScreen
import com.driverprofit.feature.vehicle.list.VehicleListScreen

/**
 * Grafo de navegação único do aplicativo (Single Activity + Navigation
 * Compose, PRD §3).
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
            DashboardScreen(
                onOpenVehicles = { navController.navigate(DriverProfitDestination.VEHICLE_LIST) },
            )
        }

        composable(route = DriverProfitDestination.VEHICLE_LIST) {
            VehicleListScreen(
                onBack = navController::popBackStack,
                onAddVehicle = {
                    navController.navigate(DriverProfitDestination.vehicleForm())
                },
                onEditVehicle = { vehicleId ->
                    navController.navigate(DriverProfitDestination.vehicleForm(vehicleId))
                },
            )
        }

        composable(
            route = DriverProfitDestination.VEHICLE_FORM,
            arguments = listOf(
                navArgument(DriverProfitDestination.ARG_VEHICLE_ID) {
                    type = NavType.LongType
                    // Ausente significa cadastro novo, não erro de navegação.
                    defaultValue = Vehicle.UNSAVED_ID
                },
            ),
        ) {
            VehicleFormScreen(
                onBack = navController::popBackStack,
                // Após salvar, volta para a lista já atualizada pelo Flow do Room.
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
