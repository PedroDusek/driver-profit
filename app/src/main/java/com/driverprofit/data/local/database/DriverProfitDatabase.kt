package com.driverprofit.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.driverprofit.data.local.dao.ExpenseDao
import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.dao.WorkSessionDao
import com.driverprofit.data.local.entity.ExpenseEntity
import com.driverprofit.data.local.entity.VehicleEntity
import com.driverprofit.data.local.entity.WorkSessionEntity

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
    entities = [VehicleEntity::class, WorkSessionEntity::class, ExpenseEntity::class],
    version = DriverProfitDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DriverProfitDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    abstract fun workSessionDao(): WorkSessionDao

    abstract fun expenseDao(): ExpenseDao

    companion object {
        /**
         * Versão 4 — adiciona `expenses` (registro de despesas).
         * Versão 3 adicionou `work_sessions` (registro de ganhos).
         * Versão 2 simplificou o veículo para nome + combustível.
         * Versão 1 tinha marca, modelo, ano, odômetro e três eixos de propulsão.
         */
        const val VERSION = 4

        const val NAME = "driver_profit.db"
    }
}
