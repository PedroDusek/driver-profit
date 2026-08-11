package com.driverprofit.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações do banco. Toda alteração de schema entra aqui — nunca
 * `fallbackToDestructiveMigration` (PRD §45).
 */
object Migrations {

    /**
     * 1 → 2: simplificação do cadastro de veículo.
     *
     * Remove `brand`, `model`, `year`, `initial_odometer_km`, `powertrain` e
     * `charging_capability`; introduz `name` e `fuel`.
     *
     * SQLite não suporta `DROP COLUMN` em versões antigas do Android, então o
     * caminho seguro é o padrão tabela-nova + cópia + troca.
     *
     * Os dados existentes **não** são descartados:
     *  - `name` recebe "marca modelo", que é como o motorista já reconhecia o
     *    carro na lista;
     *  - `fuel` deriva da propulsão antiga, caindo em `combustion_fuel` quando
     *    o veículo era puramente a combustão.
     *
     * `FLEX` é o padrão de último recurso porque é a configuração mais comum
     * na frota brasileira de aplicativo — e porque um valor não nulo é
     * obrigatório na coluna.
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `vehicles_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `fuel` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                INSERT INTO `vehicles_new` (`id`, `name`, `fuel`, `created_at`)
                SELECT
                    `id`,
                    TRIM(`brand` || ' ' || `model`),
                    CASE
                        WHEN `powertrain` = 'ELECTRIC' THEN 'ELECTRIC'
                        WHEN `powertrain` = 'HYBRID' THEN 'HYBRID'
                        WHEN `combustion_fuel` IS NOT NULL THEN `combustion_fuel`
                        ELSE 'FLEX'
                    END,
                    `created_at`
                FROM `vehicles`
                """.trimIndent(),
            )

            db.execSQL("DROP TABLE `vehicles`")
            db.execSQL("ALTER TABLE `vehicles_new` RENAME TO `vehicles`")
        }
    }

    /** Todas as migrações conhecidas, na ordem. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
