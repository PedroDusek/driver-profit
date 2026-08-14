package com.driverprofit.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.feature.dashboard.DashboardScreen
import com.driverprofit.feature.earnings.form.EarningsFormScreen
import com.driverprofit.feature.earnings.list.EarningsListScreen
import com.driverprofit.feature.expenses.form.ExpenseFormScreen
import com.driverprofit.feature.expenses.list.ExpensesListScreen
import com.driverprofit.feature.personal.form.PersonalUsageFormScreen
import com.driverprofit.feature.personal.list.PersonalUsageListScreen
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
                onOpenEarnings = { navController.navigate(DriverProfitDestination.EARNINGS_LIST) },
                onOpenExpenses = { navController.navigate(DriverProfitDestination.EXPENSES_LIST) },
                onOpenPersonalUsage = {
                    navController.navigate(DriverProfitDestination.PERSONAL_USAGE_LIST)
                },
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
                onSaved = { navController.popBackStack() },
            )
        }

        composable(route = DriverProfitDestination.EARNINGS_LIST) {
            EarningsListScreen(
                onBack = navController::popBackStack,
                onAddSession = {
                    navController.navigate(DriverProfitDestination.earningsForm())
                },
                onEditSession = { sessionId ->
                    navController.navigate(DriverProfitDestination.earningsForm(sessionId))
                },
            )
        }

        composable(
            route = DriverProfitDestination.EARNINGS_FORM,
            arguments = listOf(
                navArgument(DriverProfitDestination.ARG_SESSION_ID) {
                    type = NavType.LongType
                    defaultValue = WorkSession.UNSAVED_ID
                },
            ),
        ) {
            EarningsFormScreen(
                onBack = navController::popBackStack,
                // Após salvar, volta para o histórico já atualizado pelo Flow.
                onSaved = { navController.popBackStack() },
            )
        }

        composable(route = DriverProfitDestination.EXPENSES_LIST) {
            ExpensesListScreen(
                onBack = navController::popBackStack,
                onAddExpense = {
                    navController.navigate(DriverProfitDestination.expenseForm())
                },
                onEditExpense = { expenseId ->
                    navController.navigate(DriverProfitDestination.expenseForm(expenseId))
                },
            )
        }

        composable(route = DriverProfitDestination.PERSONAL_USAGE_LIST) {
            PersonalUsageListScreen(
                onBack = navController::popBackStack,
                onAddUsage = {
                    navController.navigate(DriverProfitDestination.personalUsageForm())
                },
                onEditUsage = { usageId ->
                    navController.navigate(DriverProfitDestination.personalUsageForm(usageId))
                },
            )
        }

        composable(
            route = DriverProfitDestination.PERSONAL_USAGE_FORM,
            arguments = listOf(
                navArgument(DriverProfitDestination.ARG_PERSONAL_USAGE_ID) {
                    type = NavType.LongType
                    defaultValue = PersonalUsage.UNSAVED_ID
                },
            ),
        ) {
            PersonalUsageFormScreen(
                onBack = navController::popBackStack,
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = DriverProfitDestination.EXPENSE_FORM,
            arguments = listOf(
                navArgument(DriverProfitDestination.ARG_EXPENSE_ID) {
                    type = NavType.LongType
                    defaultValue = Expense.UNSAVED_ID
                },
            ),
        ) {
            ExpenseFormScreen(
                onBack = navController::popBackStack,
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
