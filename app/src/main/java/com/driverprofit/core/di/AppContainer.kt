package com.driverprofit.core.di

import android.content.Context
import androidx.room.Room
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.database.Migrations
import com.driverprofit.data.repository.OfflineExpenseRepository
import com.driverprofit.data.repository.OfflineVehicleRepository
import com.driverprofit.data.repository.OfflineWorkSessionRepository
import com.driverprofit.domain.repository.ExpenseRepository
import com.driverprofit.domain.repository.VehicleRepository
import com.driverprofit.domain.repository.WorkSessionRepository
import com.driverprofit.domain.usecase.DeleteExpenseUseCase
import com.driverprofit.domain.usecase.DeleteVehicleUseCase
import com.driverprofit.domain.usecase.DeleteWorkSessionUseCase
import com.driverprofit.domain.usecase.ExpenseValidator
import com.driverprofit.domain.usecase.GetExpenseUseCase
import com.driverprofit.domain.usecase.GetVehicleUseCase
import com.driverprofit.domain.usecase.GetWorkSessionUseCase
import com.driverprofit.domain.usecase.ObserveDashboardUseCase
import com.driverprofit.domain.usecase.ObserveExpensesBetweenUseCase
import com.driverprofit.domain.usecase.ObserveExpensesUseCase
import com.driverprofit.domain.usecase.ObserveVehicleOdometerUseCase
import com.driverprofit.domain.usecase.ObserveVehicleOdometersUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.domain.usecase.ObserveWorkSessionsBetweenUseCase
import com.driverprofit.domain.usecase.ObserveWorkSessionsUseCase
import com.driverprofit.domain.usecase.SaveExpenseUseCase
import com.driverprofit.domain.usecase.SaveVehicleUseCase
import com.driverprofit.domain.usecase.SaveWorkSessionUseCase
import com.driverprofit.domain.usecase.VehicleValidator
import com.driverprofit.domain.usecase.WorkSessionValidator

/**
 * Injeção de dependências manual.
 *
 * Decisão registrada (PRD §55): o projeto **não** usa Hilt/Koin no MVP. Com um
 * único módulo Gradle e um punhado de dependências, um container manual
 * resolve o problema sem custo de build (processamento de anotações) nem de
 * aprendizado. Se o grafo crescer a ponto de este arquivo ficar difícil de
 * ler, reavaliar — e documentar a troca em docs/ARCHITECTURE.md.
 */
interface AppContainer {
    val vehicleRepository: VehicleRepository
    val workSessionRepository: WorkSessionRepository

    val saveVehicle: SaveVehicleUseCase
    val observeVehicles: ObserveVehiclesUseCase
    val getVehicle: GetVehicleUseCase
    val deleteVehicle: DeleteVehicleUseCase

    val saveWorkSession: SaveWorkSessionUseCase
    val observeWorkSessions: ObserveWorkSessionsUseCase
    val getWorkSession: GetWorkSessionUseCase
    val deleteWorkSession: DeleteWorkSessionUseCase

    val saveExpense: SaveExpenseUseCase
    val observeExpenses: ObserveExpensesUseCase
    val getExpense: GetExpenseUseCase
    val deleteExpense: DeleteExpenseUseCase

    val observeDashboard: ObserveDashboardUseCase

    val observeVehicleOdometer: ObserveVehicleOdometerUseCase
    val observeVehicleOdometers: ObserveVehicleOdometersUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DriverProfitDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            DriverProfitDatabase::class.java,
            DriverProfitDatabase.NAME,
        )
            .addMigrations(*Migrations.ALL)
            // Sem fallbackToDestructiveMigration: ver DriverProfitDatabase.
            .build()
    }

    override val vehicleRepository: VehicleRepository by lazy {
        OfflineVehicleRepository(database.vehicleDao())
    }

    private val vehicleValidator = VehicleValidator()

    override val saveVehicle: SaveVehicleUseCase by lazy {
        SaveVehicleUseCase(vehicleRepository, vehicleValidator)
    }

    override val observeVehicles: ObserveVehiclesUseCase by lazy {
        ObserveVehiclesUseCase(vehicleRepository)
    }

    override val getVehicle: GetVehicleUseCase by lazy {
        GetVehicleUseCase(vehicleRepository)
    }

    override val deleteVehicle: DeleteVehicleUseCase by lazy {
        DeleteVehicleUseCase(vehicleRepository)
    }

    override val workSessionRepository: WorkSessionRepository by lazy {
        OfflineWorkSessionRepository(database.workSessionDao())
    }

    private val workSessionValidator = WorkSessionValidator()

    override val saveWorkSession: SaveWorkSessionUseCase by lazy {
        SaveWorkSessionUseCase(workSessionRepository, workSessionValidator)
    }

    override val observeWorkSessions: ObserveWorkSessionsUseCase by lazy {
        ObserveWorkSessionsUseCase(workSessionRepository)
    }

    override val getWorkSession: GetWorkSessionUseCase by lazy {
        GetWorkSessionUseCase(workSessionRepository)
    }

    override val deleteWorkSession: DeleteWorkSessionUseCase by lazy {
        DeleteWorkSessionUseCase(workSessionRepository)
    }

    private val expenseRepository: ExpenseRepository by lazy {
        OfflineExpenseRepository(database.expenseDao())
    }

    private val expenseValidator = ExpenseValidator()

    override val saveExpense: SaveExpenseUseCase by lazy {
        SaveExpenseUseCase(expenseRepository, vehicleRepository, expenseValidator)
    }

    override val observeExpenses: ObserveExpensesUseCase by lazy {
        ObserveExpensesUseCase(expenseRepository)
    }

    override val getExpense: GetExpenseUseCase by lazy {
        GetExpenseUseCase(expenseRepository)
    }

    override val deleteExpense: DeleteExpenseUseCase by lazy {
        DeleteExpenseUseCase(expenseRepository)
    }

    override val observeVehicleOdometer: ObserveVehicleOdometerUseCase by lazy {
        ObserveVehicleOdometerUseCase(expenseRepository)
    }

    override val observeVehicleOdometers: ObserveVehicleOdometersUseCase by lazy {
        ObserveVehicleOdometersUseCase(expenseRepository)
    }

    override val observeDashboard: ObserveDashboardUseCase by lazy {
        ObserveDashboardUseCase(
            observeWorkSessionsBetween = ObserveWorkSessionsBetweenUseCase(workSessionRepository),
            observeExpensesBetween = ObserveExpensesBetweenUseCase(expenseRepository),
        )
    }
}
