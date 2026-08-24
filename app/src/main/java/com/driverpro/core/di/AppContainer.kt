package com.driverpro.core.di

import android.content.Context
import androidx.room.Room
import com.driverpro.data.backup.ExportBackupUseCase
import com.driverpro.data.backup.ImportBackupUseCase
import com.driverpro.core.database.DriverProDatabase
import com.driverpro.core.database.Migrations
import com.driverpro.data.repository.OfflineExpenseRepository
import com.driverpro.data.repository.OfflineMaintenanceScheduleRepository
import com.driverpro.data.repository.OfflinePersonalUsageRepository
import com.driverpro.data.repository.OfflineReconciliationDismissalRepository
import com.driverpro.vehicle.data.OfflineVehicleRepository
import com.driverpro.data.repository.OfflineWorkSessionRepository
import com.driverpro.domain.repository.ExpenseRepository
import com.driverpro.domain.repository.MaintenanceScheduleRepository
import com.driverpro.domain.repository.PersonalUsageRepository
import com.driverpro.domain.repository.ReconciliationDismissalRepository
import com.driverpro.vehicle.domain.VehicleRepository
import com.driverpro.domain.repository.WorkSessionRepository
import com.driverpro.domain.usecase.DeleteExpenseUseCase
import com.driverpro.vehicle.domain.DeleteVehicleUseCase
import com.driverpro.domain.usecase.DeleteWorkSessionUseCase
import com.driverpro.domain.usecase.ExpenseValidator
import com.driverpro.domain.usecase.GetExpenseUseCase
import com.driverpro.vehicle.domain.GetVehicleUseCase
import com.driverpro.domain.usecase.GetWorkSessionUseCase
import com.driverpro.domain.usecase.ObserveAccruedExpensesUseCase
import com.driverpro.domain.usecase.ObserveDashboardUseCase
import com.driverpro.domain.usecase.ObserveMaintenanceUseCase
import com.driverpro.domain.usecase.ResetMaintenanceIntervalUseCase
import com.driverpro.domain.usecase.SaveMaintenanceIntervalUseCase
import com.driverpro.domain.usecase.ObserveExpensesBetweenUseCase
import com.driverpro.domain.usecase.ObserveExpensesUseCase
import com.driverpro.domain.usecase.DeletePersonalUsageUseCase
import com.driverpro.domain.usecase.GetPersonalUsageUseCase
import com.driverpro.domain.usecase.ObservePersonalUsageInPeriodUseCase
import com.driverpro.domain.usecase.ObservePersonalUsageUseCase
import com.driverpro.domain.usecase.PersonalUsageValidator
import com.driverpro.domain.usecase.DismissReconciliationUseCase
import com.driverpro.domain.usecase.ObserveOdometerReconciliationUseCase
import com.driverpro.domain.usecase.SavePersonalUsageUseCase
import com.driverpro.domain.usecase.SaveReconciledPersonalUsageUseCase
import com.driverpro.domain.usecase.ObserveVehicleOdometerUseCase
import com.driverpro.domain.usecase.ObserveVehicleOdometersUseCase
import com.driverpro.vehicle.domain.ObserveVehiclesUseCase
import com.driverpro.domain.usecase.ObserveWorkSessionsBetweenUseCase
import com.driverpro.domain.usecase.ObserveWorkSessionsUseCase
import com.driverpro.domain.usecase.SaveExpenseUseCase
import com.driverpro.vehicle.domain.SaveVehicleUseCase
import com.driverpro.domain.usecase.SaveWorkSessionUseCase
import com.driverpro.vehicle.domain.SetCurrentVehicleUseCase
import com.driverpro.vehicle.domain.VehicleValidator
import com.driverpro.domain.usecase.WorkSessionValidator

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
    val setCurrentVehicle: SetCurrentVehicleUseCase

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

    val savePersonalUsage: SavePersonalUsageUseCase
    val observePersonalUsage: ObservePersonalUsageUseCase
    val getPersonalUsage: GetPersonalUsageUseCase
    val deletePersonalUsage: DeletePersonalUsageUseCase
    val observeOdometerReconciliation: ObserveOdometerReconciliationUseCase
    val saveReconciledPersonalUsage: SaveReconciledPersonalUsageUseCase
    val dismissReconciliation: DismissReconciliationUseCase

    val observeMaintenance: ObserveMaintenanceUseCase
    val saveMaintenanceInterval: SaveMaintenanceIntervalUseCase
    val resetMaintenanceInterval: ResetMaintenanceIntervalUseCase

    val exportBackup: ExportBackupUseCase
    val importBackup: ImportBackupUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DriverProDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            DriverProDatabase::class.java,
            DriverProDatabase.NAME,
        )
            .addMigrations(*Migrations.ALL)
            // Sem fallbackToDestructiveMigration: ver DriverProDatabase.
            .build()
    }

    override val exportBackup: ExportBackupUseCase by lazy {
        ExportBackupUseCase(context.applicationContext, database)
    }

    override val importBackup: ImportBackupUseCase by lazy {
        ImportBackupUseCase(context.applicationContext, database)
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

    override val setCurrentVehicle: SetCurrentVehicleUseCase by lazy {
        SetCurrentVehicleUseCase(vehicleRepository)
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

    private val personalUsageRepository: PersonalUsageRepository by lazy {
        OfflinePersonalUsageRepository(database.personalUsageDao())
    }

    private val personalUsageValidator = PersonalUsageValidator()

    override val savePersonalUsage: SavePersonalUsageUseCase by lazy {
        SavePersonalUsageUseCase(personalUsageRepository, personalUsageValidator)
    }

    override val observePersonalUsage: ObservePersonalUsageUseCase by lazy {
        ObservePersonalUsageUseCase(personalUsageRepository)
    }

    override val getPersonalUsage: GetPersonalUsageUseCase by lazy {
        GetPersonalUsageUseCase(personalUsageRepository)
    }

    override val deletePersonalUsage: DeletePersonalUsageUseCase by lazy {
        DeletePersonalUsageUseCase(personalUsageRepository)
    }

    override val observeOdometerReconciliation: ObserveOdometerReconciliationUseCase by lazy {
        ObserveOdometerReconciliationUseCase(
            vehicleRepository = vehicleRepository,
            expenseRepository = expenseRepository,
            workSessionRepository = workSessionRepository,
            personalUsageRepository = personalUsageRepository,
            dismissalRepository = dismissalRepository,
        )
    }

    private val dismissalRepository: ReconciliationDismissalRepository by lazy {
        OfflineReconciliationDismissalRepository(database.reconciliationDismissalDao())
    }

    override val dismissReconciliation: DismissReconciliationUseCase by lazy {
        DismissReconciliationUseCase(dismissalRepository)
    }

    override val saveReconciledPersonalUsage: SaveReconciledPersonalUsageUseCase by lazy {
        SaveReconciledPersonalUsageUseCase(personalUsageRepository)
    }

    private val maintenanceScheduleRepository: MaintenanceScheduleRepository by lazy {
        OfflineMaintenanceScheduleRepository(database.maintenanceScheduleDao())
    }

    override val observeMaintenance: ObserveMaintenanceUseCase by lazy {
        ObserveMaintenanceUseCase(
            vehicleRepository = vehicleRepository,
            expenseRepository = expenseRepository,
            scheduleRepository = maintenanceScheduleRepository,
        )
    }

    override val saveMaintenanceInterval: SaveMaintenanceIntervalUseCase by lazy {
        SaveMaintenanceIntervalUseCase(maintenanceScheduleRepository)
    }

    override val resetMaintenanceInterval: ResetMaintenanceIntervalUseCase by lazy {
        ResetMaintenanceIntervalUseCase(maintenanceScheduleRepository)
    }

    override val observeDashboard: ObserveDashboardUseCase by lazy {
        ObserveDashboardUseCase(
            observeWorkSessionsBetween = ObserveWorkSessionsBetweenUseCase(workSessionRepository),
            observeExpensesBetween = ObserveExpensesBetweenUseCase(expenseRepository),
            observePersonalUsageInPeriod =
                ObservePersonalUsageInPeriodUseCase(personalUsageRepository),
            observeAccruedInPeriod = ObserveAccruedExpensesUseCase(expenseRepository),
        )
    }
}
