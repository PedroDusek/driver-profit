package com.driverpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.driverpro.expenses.domain.Expense
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.feature.backup.BackupScreen
import com.driverpro.feature.dashboard.DashboardScreen
import com.driverpro.earnings.presentation.form.EarningsFormScreen
import com.driverpro.earnings.presentation.list.EarningsListScreen
import com.driverpro.expenses.presentation.form.ExpenseFormScreen
import com.driverpro.expenses.presentation.list.ExpensesListScreen
import com.driverpro.maintenance.presentation.MaintenanceScreen
import com.driverpro.feature.more.MoreScreen
import com.driverpro.feature.personal.form.PersonalUsageFormScreen
import com.driverpro.feature.personal.list.PersonalUsageListScreen
import com.driverpro.vehicle.presentation.form.VehicleFormScreen
import com.driverpro.vehicle.presentation.list.VehicleListScreen

/**
 * Grafo de navegação único do aplicativo (Single Activity + Navigation
 * Compose, PRD §3).
 */
@Composable
fun DriverProNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = DriverProDestination.START,
        modifier = modifier,
    ) {
        composable(route = DriverProDestination.DASHBOARD) {
            DashboardScreen(
                onOpenVehicles = { navController.navigate(DriverProDestination.VEHICLE_LIST) },
                onOpenEarnings = { navController.navigate(DriverProDestination.EARNINGS_LIST) },
                onOpenExpenses = { navController.navigate(DriverProDestination.EXPENSES_LIST) },
                onOpenMore = { navController.navigate(DriverProDestination.MORE) },
            )
        }

        composable(route = DriverProDestination.MORE) {
            MoreScreen(
                onBack = navController::popBackStack,
                onOpenPersonalUsage = {
                    navController.navigate(DriverProDestination.PERSONAL_USAGE_LIST)
                },
                onOpenMaintenance = {
                    navController.navigate(DriverProDestination.MAINTENANCE)
                },
                onOpenBackup = {
                    navController.navigate(DriverProDestination.BACKUP)
                },
            )
        }

        composable(route = DriverProDestination.MAINTENANCE) {
            MaintenanceScreen(onBack = navController::popBackStack)
        }

        composable(route = DriverProDestination.BACKUP) {
            BackupScreen(onBack = navController::popBackStack)
        }

        composable(route = DriverProDestination.VEHICLE_LIST) {
            VehicleListScreen(
                onBack = navController::popBackStack,
                onAddVehicle = {
                    navController.navigate(DriverProDestination.vehicleForm())
                },
                onEditVehicle = { vehicleId ->
                    navController.navigate(DriverProDestination.vehicleForm(vehicleId))
                },
            )
        }

        composable(
            route = DriverProDestination.VEHICLE_FORM,
            arguments = listOf(
                navArgument(DriverProDestination.ARG_VEHICLE_ID) {
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

        composable(route = DriverProDestination.EARNINGS_LIST) {
            EarningsListScreen(
                onBack = navController::popBackStack,
                onAddSession = {
                    navController.navigate(DriverProDestination.earningsForm())
                },
                onEditSession = { sessionId ->
                    navController.navigate(DriverProDestination.earningsForm(sessionId))
                },
            )
        }

        composable(
            route = DriverProDestination.EARNINGS_FORM,
            arguments = listOf(
                navArgument(DriverProDestination.ARG_SESSION_ID) {
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

        composable(route = DriverProDestination.EXPENSES_LIST) {
            ExpensesListScreen(
                onBack = navController::popBackStack,
                onAddExpense = {
                    navController.navigate(DriverProDestination.expenseForm())
                },
                onEditExpense = { expenseId ->
                    navController.navigate(DriverProDestination.expenseForm(expenseId))
                },
            )
        }

        composable(route = DriverProDestination.PERSONAL_USAGE_LIST) {
            PersonalUsageListScreen(
                onBack = navController::popBackStack,
                onAddUsage = {
                    navController.navigate(DriverProDestination.personalUsageForm())
                },
                onEditUsage = { usageId ->
                    navController.navigate(DriverProDestination.personalUsageForm(usageId))
                },
            )
        }

        composable(
            route = DriverProDestination.PERSONAL_USAGE_FORM,
            arguments = listOf(
                navArgument(DriverProDestination.ARG_PERSONAL_USAGE_ID) {
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
            route = DriverProDestination.EXPENSE_FORM,
            arguments = listOf(
                navArgument(DriverProDestination.ARG_EXPENSE_ID) {
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
