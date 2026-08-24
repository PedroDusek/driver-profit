package com.driverpro.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.driverpro.data.local.dao.ExpenseDao
import com.driverpro.data.local.dao.MaintenanceScheduleDao
import com.driverpro.data.local.dao.PersonalUsageDao
import com.driverpro.data.local.dao.ReconciliationDismissalDao
import com.driverpro.vehicle.data.VehicleDao
import com.driverpro.data.local.dao.WorkSessionDao
import com.driverpro.data.local.entity.ExpenseEntity
import com.driverpro.data.local.entity.MaintenanceScheduleEntity
import com.driverpro.data.local.entity.PersonalUsageEntity
import com.driverpro.data.local.entity.ReconciliationDismissalEntity
import com.driverpro.vehicle.data.VehicleEntity
import com.driverpro.data.local.entity.WorkSessionEntity

/**
 * Banco local do aplicativo (offline-first, PRD §1).
 *
 * `exportSchema = true`: o JSON do schema é versionado em `app/schemas/` e é o
 * que permite escrever testes de migração de verdade (PRD §45).
 *
 * **Nunca** adicionar `fallbackToDestructiveMigration()`: perder os dados do
 * motorista para resolver mudança de schema não é aceitável. Toda alteração de
 * schema exige incrementar [VERSION], escrever a `Migration` em [Migrations],
 * testá-la e atualizar `docs/DATABASE.md`.
 */
@Database(
    entities = [
        VehicleEntity::class,
        WorkSessionEntity::class,
        ExpenseEntity::class,
        PersonalUsageEntity::class,
        MaintenanceScheduleEntity::class,
        ReconciliationDismissalEntity::class,
    ],
    version = DriverProDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DriverProDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    abstract fun workSessionDao(): WorkSessionDao

    abstract fun expenseDao(): ExpenseDao

    abstract fun personalUsageDao(): PersonalUsageDao

    abstract fun maintenanceScheduleDao(): MaintenanceScheduleDao

    abstract fun reconciliationDismissalDao(): ReconciliationDismissalDao

    companion object {
        /**
         * Versão 10 — adiciona `vehicles.is_current` e `work_sessions.vehicle_id` (veículo atual).
         * Versão 9 — adiciona `reconciliation_dismissals` (sobras aceitas fora da conta).
         * Versão 8 adicionou competência (`accrual_start`, `accrual_end`) em `expenses`.
         * Versão 7 adicionou `maintenance_schedules` (intervalos de manutenção).
         * Versão 6 adicionou `personal_usage` (quilometragem fora do trabalho).
         * Versão 5 adicionou `odometer_km` em `expenses` (odômetro por lançamento).
         * Versão 4 adicionou `expenses` (registro de despesas).
         * Versão 3 adicionou `work_sessions` (registro de ganhos).
         * Versão 2 simplificou o veículo para nome + combustível.
         * Versão 1 tinha marca, modelo, ano, odômetro e três eixos de propulsão.
         */
        const val VERSION = 10

        const val NAME = "driverpro.db"
    }
}
