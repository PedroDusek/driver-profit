package com.driverprofit.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.entity.VehicleEntity

/**
 * Banco local do aplicativo (offline-first, PRD §1).
 *
 * `exportSchema = true`: o JSON do schema é versionado em `app/schemas/` e é o
 * que permite escrever testes de migração de verdade (PRD §45).
 *
 * **Nunca** adicionar `fallbackToDestructiveMigration()`: perder os dados do
 * motorista para resolver mudança de schema não é aceitável. Toda alteração de
 * schema exige incrementar [VERSION], escrever a `Migration`, testá-la e
 * atualizar `docs/DATABASE.md`.
 */
@Database(
    entities = [VehicleEntity::class],
    version = DriverProfitDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class DriverProfitDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao

    companion object {
        /** Versão 1 — schema inicial: apenas `vehicles`. */
        const val VERSION = 1

        const val NAME = "driver_profit.db"
    }
}
