package com.driverpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.model.WorkSession
import com.driverpro.feature.backup.BackupScreen
import com.driverpro.feature.dashboard.DashboardScreen
import com.driverpro.feature.earnings.form.EarningsFormScreen
import com.driverpro.feature.earnings.list.EarningsListScreen
import com.driverpro.feature.expenses.form.ExpenseFormScreen
import com.driverpro.feature.expenses.list.ExpensesListScreen
import com.driverpro.feature.maintenance.MaintenanceScreen
import com.driverpro.feature.more.MoreScreen
import com.driverpro.feature.personal.form.PersonalUsageFormScreen
import com.driverpro.feature.personal.list.PersonalUsageListScreen
import com.driverpro.feature.vehicle.form.VehicleFormScreen
import com.driverpro.feature.vehicle.list.VehicleListScreen

/**
 * Grafo de navegação único do aplicativo (Single Activity + Navigation
 * Compose, PRD §3).
 */
@Composable
fun DriverProNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    /**
     * Troca de aba, com a semântica que separa aba de tela empilhada.
     *
     * `popUpTo(start) { saveState = true }` impede a pilha de crescer a cada
     * toque: sem ele, ir Ganhos → Gastos → Veículos deixaria três telas
     * empilhadas, e o botão voltar do Android faria o motorista desfazer o
     * caminho tela a tela em vez de sair do app. Com ele, voltar de qualquer
     * aba leva ao dashboard, que é o destino inicial.
     *
     * `saveState`/`restoreState` preservam o que cada aba tinha: a rolagem do
     * histórico de despesas e o filtro escolhido continuam lá ao voltar para
     * ela. `launchSingleTop` evita duas cópias da mesma aba no topo.
     */
    val onSelectTab: (DriverProTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = DriverProDestination.START,
        modifier = modifier,
    ) {
        composable(route = DriverProDestination.DASHBOARD) {
            DashboardScreen(onSelectTab = onSelectTab)
        }

        composable(route = DriverProDestination.MORE) {
            MoreScreen(
                onSelectTab = onSelectTab,
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
                onSelectTab = onSelectTab,
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
                onSelectTab = onSelectTab,
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
                onSelectTab = onSelectTab,
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
