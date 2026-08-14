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

    /**
     * 2 → 3: registro de ganhos.
     *
     * Cria `work_sessions`. Migração puramente aditiva — nenhuma tabela
     * existente é tocada, então não há risco para os dados do motorista.
     *
     * O índice em `date` acompanha a criação porque toda consulta do
     * dashboard vai filtrar por período (PRD §20).
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `work_sessions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `date` INTEGER NOT NULL,
                    `platform` TEXT NOT NULL,
                    `rides` INTEGER NOT NULL,
                    `revenue_cents` INTEGER NOT NULL,
                    `online_minutes` INTEGER NOT NULL,
                    `distance_km` INTEGER NOT NULL,
                    `note` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_work_sessions_date` " +
                    "ON `work_sessions` (`date`)",
            )
        }
    }

    /**
     * 3 → 4: registro de despesas.
     *
     * Cria `expenses` com chave estrangeira para `vehicles`. Aditiva: nenhuma
     * tabela existente é alterada.
     *
     * `ON DELETE SET NULL` no veículo — excluir um carro não pode apagar o
     * histórico financeiro; a despesa fica órfã e continua somando.
     *
     * Dois índices: `date`, porque toda consulta do dashboard filtra por
     * período, e `vehicle_id`, que o Room exige para a chave estrangeira (sem
     * ele, apagar um veículo faria varredura completa da tabela).
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `expenses` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicle_id` INTEGER,
                    `date` INTEGER NOT NULL,
                    `category` TEXT NOT NULL,
                    `amount_cents` INTEGER NOT NULL,
                    `description` TEXT NOT NULL,
                    `fuel_type` TEXT,
                    `quantity_thousandths` INTEGER,
                    `charging_location` TEXT,
                    `maintenance_category` TEXT,
                    `place` TEXT,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicle_id`) REFERENCES `vehicles`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_date` ON `expenses` (`date`)")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_expenses_vehicle_id` " +
                    "ON `expenses` (`vehicle_id`)",
            )
        }
    }

    /**
     * 4 → 5: odômetro por lançamento.
     *
     * Acrescenta `odometer_km` a `expenses`. Aditiva e sem reescrita de
     * tabela: `ALTER TABLE ADD COLUMN` é barato mesmo com histórico grande.
     *
     * A coluna é **anulável e sem default**. As despesas já gravadas não têm
     * leitura de painel, e inventar uma — zero, ou repetir a anterior —
     * envenenaria justamente o que o odômetro serve para calcular: consumo
     * estimado, quilômetro pessoal e alerta de manutenção. `NULL` diz a
     * verdade, que é "não sei", e o domínio sabe lidar com isso.
     *
     * A obrigatoriedade nas categorias ligadas ao veículo vive em
     * `ExpenseValidator`, não aqui: ela vale para o que se grava de agora em
     * diante, e não pode invalidar retroativamente o histórico do motorista.
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `odometer_km` INTEGER")
        }
    }

    /**
     * 5 → 6: quilometragem de uso pessoal.
     *
     * Cria `personal_usage`. Aditiva — nenhuma tabela existente é tocada.
     *
     * Tabela própria, e não coluna em `work_sessions`: uso pessoal não tem
     * plataforma, corridas nem faturamento, e não é uma jornada.
     *
     * Três índices. `start_date` e `end_date` porque a consulta do dashboard é
     * de **sobreposição** de intervalos — uma viagem de 28/07 a 03/08 precisa
     * aparecer nos dois meses. `vehicle_id` porque o Room o exige para a chave
     * estrangeira.
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `personal_usage` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicle_id` INTEGER,
                    `start_date` INTEGER NOT NULL,
                    `end_date` INTEGER NOT NULL,
                    `distance_km` INTEGER NOT NULL,
                    `source` TEXT NOT NULL,
                    `note` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicle_id`) REFERENCES `vehicles`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_personal_usage_start_date` " +
                    "ON `personal_usage` (`start_date`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_personal_usage_end_date` " +
                    "ON `personal_usage` (`end_date`)",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_personal_usage_vehicle_id` " +
                    "ON `personal_usage` (`vehicle_id`)",
            )
        }
    }

    /** Todas as migrações conhecidas, na ordem. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
    )
}
