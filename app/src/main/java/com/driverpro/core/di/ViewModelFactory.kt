package com.driverpro.core.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.driverpro.DriverProApplication
import com.driverpro.core.di.AppContainer
import com.driverpro.feature.backup.BackupViewModel
import com.driverpro.feature.dashboard.DashboardViewModel
import com.driverpro.feature.earnings.form.EarningsFormViewModel
import com.driverpro.feature.earnings.list.EarningsListViewModel
import com.driverpro.expenses.presentation.form.ExpenseFormViewModel
import com.driverpro.expenses.presentation.list.ExpensesListViewModel
import com.driverpro.feature.maintenance.MaintenanceViewModel
import com.driverpro.feature.personal.form.PersonalUsageFormViewModel
import com.driverpro.feature.personal.list.PersonalUsageListViewModel
import com.driverpro.vehicle.presentation.form.VehicleFormViewModel
import com.driverpro.vehicle.presentation.list.VehicleListViewModel

/**
 * Fábricas de ViewModel do aplicativo.
 *
 * Com DI manual (ver `core/di/AppContainer`), cada ViewModel precisa de uma
 * fábrica que saiba montar suas dependências. Concentrá-las aqui evita
 * espalhar `viewModelFactory` por dentro dos Composables.
 */
object DriverProViewModelFactory {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            DashboardViewModel(
                observeDashboard = container().observeDashboard,
                observeMaintenance = container().observeMaintenance,
                observeReconciliation = container().observeOdometerReconciliation,
            )
        }
        initializer {
            MaintenanceViewModel(
                observeMaintenance = container().observeMaintenance,
                saveInterval = container().saveMaintenanceInterval,
                resetInterval = container().resetMaintenanceInterval,
            )
        }
        initializer {
            PersonalUsageListViewModel(
                observePersonalUsage = container().observePersonalUsage,
                observeReconciliation = container().observeOdometerReconciliation,
                deletePersonalUsage = container().deletePersonalUsage,
                saveReconciled = container().saveReconciledPersonalUsage,
                dismissReconciliation = container().dismissReconciliation,
            )
        }
        initializer {
            PersonalUsageFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                observeVehicles = container().observeVehicles,
                getPersonalUsage = container().getPersonalUsage,
                savePersonalUsage = container().savePersonalUsage,
            )
        }
        initializer {
            VehicleListViewModel(
                observeVehicles = container().observeVehicles,
                observeVehicleOdometers = container().observeVehicleOdometers,
                deleteVehicle = container().deleteVehicle,
                setCurrentVehicle = container().setCurrentVehicle,
            )
        }
        initializer {
            VehicleFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                getVehicle = container().getVehicle,
                saveVehicle = container().saveVehicle,
            )
        }
        initializer {
            EarningsListViewModel(
                observeWorkSessions = container().observeWorkSessions,
                deleteWorkSession = container().deleteWorkSession,
            )
        }
        initializer {
            EarningsFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                getWorkSession = container().getWorkSession,
                saveWorkSession = container().saveWorkSession,
                observeVehicles = container().observeVehicles,
            )
        }
        initializer {
            ExpensesListViewModel(
                observeExpenses = container().observeExpenses,
                deleteExpense = container().deleteExpense,
            )
        }
        initializer {
            ExpenseFormViewModel(
                savedStateHandle = createSavedStateHandle(),
                observeVehicles = container().observeVehicles,
                observeVehicleOdometers = container().observeVehicleOdometers,
                getExpense = container().getExpense,
                saveExpense = container().saveExpense,
            )
        }
        initializer {
            BackupViewModel(
                exportBackup = container().exportBackup,
                importBackup = container().importBackup,
            )
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DriverProApplication)
        .container
