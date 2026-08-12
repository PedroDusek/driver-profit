package com.driverprofit.core.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.driverprofit.DriverProfitApplication
import com.driverprofit.core.di.AppContainer
import com.driverprofit.feature.earnings.form.EarningsFormViewModel
import com.driverprofit.feature.earnings.list.EarningsListViewModel
import com.driverprofit.feature.expenses.form.ExpenseFormViewModel
import com.driverprofit.feature.expenses.list.ExpensesListViewModel
import com.driverprofit.feature.vehicle.form.VehicleFormViewModel
import com.driverprofit.feature.vehicle.list.VehicleListViewModel

/**
 * Fábricas de ViewModel do aplicativo.
 *
 * Com DI manual (ver `core/di/AppContainer`), cada ViewModel precisa de uma
 * fábrica que saiba montar suas dependências. Concentrá-las aqui evita
 * espalhar `viewModelFactory` por dentro dos Composables.
 */
object DriverProfitViewModelFactory {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            VehicleListViewModel(
                observeVehicles = container().observeVehicles,
                deleteVehicle = container().deleteVehicle,
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
                getExpense = container().getExpense,
                saveExpense = container().saveExpense,
            )
        }
    }
}

private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as DriverProfitApplication)
        .container
