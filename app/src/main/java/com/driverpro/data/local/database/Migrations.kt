package com.driverpro.data.local.database

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

    /**
     * 6 → 7: intervalos de manutenção preventiva.
     *
     * Cria `maintenance_schedules`. Aditiva — nenhuma tabela existente é
     * tocada, e o histórico de manutenção que alimenta os alertas já está em
     * `expenses` desde a v0.4.0.
     *
     * **A tabela nasce vazia, e isso é o comportamento correto.** Linha
     * ausente significa "intervalo padrão do app", então todo veículo já
     * existente passa a ser acompanhado sem que a migração precise inventar
     * cinco linhas por carro — que seria escrever no banco uma decisão que o
     * motorista não tomou.
     *
     * `ON DELETE CASCADE`, diferente de `expenses` e `personal_usage`: aquelas
     * guardam histórico financeiro e sobrevivem à exclusão do veículo; esta
     * guarda uma preferência sobre um carro que deixou de existir.
     *
     * Um índice só, único, sobre `(vehicle_id, item)`: ele impede dois
     * intervalos para o mesmo item e, por começar em `vehicle_id`, também
     * atende a exigência do Room para a chave estrangeira.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `maintenance_schedules` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicle_id` INTEGER NOT NULL,
                    `item` TEXT NOT NULL,
                    `interval_km` INTEGER NOT NULL,
                    `monitored` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicle_id`) REFERENCES `vehicles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_maintenance_schedules_vehicle_id_item` " +
                    "ON `maintenance_schedules` (`vehicle_id`, `item`)",
            )
        }
    }

    /**
     * 7 → 8: competência dos custos fixos.
     *
     * Acrescenta `accrual_start` e `accrual_end` a `expenses`. Aditiva e sem
     * reescrita de tabela.
     *
     * As duas colunas são **anuláveis e sem default**, e `NULL` é o caso comum,
     * não a exceção: significa "a competência é a própria data", que é o
     * comportamento de todo custo variável e de todo lançamento anterior a esta
     * versão. Preencher as despesas existentes com a própria data seria
     * equivalente, porém apagaria a distinção entre "não tem competência" e
     * "tem competência de um dia só" — e o app perderia a chance de oferecer o
     * campo em edição depois.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `accrual_start` INTEGER")
            db.execSQL("ALTER TABLE `expenses` ADD COLUMN `accrual_end` INTEGER")
        }
    }

    /**
     * 8 → 9: sobras de odômetro aceitas fora da conta.
     *
     * Cria `reconciliation_dismissals`. Aditiva — nenhuma tabela existente é
     * tocada, e a ausência de linha significa "nada foi dispensado", que
     * descreve corretamente todo o histórico anterior.
     *
     * `ON DELETE CASCADE`, como `maintenance_schedules`: isto é uma decisão
     * sobre um intervalo de um carro, não histórico financeiro.
     *
     * O índice é único sobre `(vehicle_id, start_date, end_date)` — uma janela
     * tem uma decisão — e é ele que sustenta o `REPLACE` do DAO. Por começar em
     * `vehicle_id`, também atende a exigência do Room para a chave estrangeira.
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `reconciliation_dismissals` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `vehicle_id` INTEGER NOT NULL,
                    `start_date` INTEGER NOT NULL,
                    `end_date` INTEGER NOT NULL,
                    `dismissed_km` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`vehicle_id`) REFERENCES `vehicles`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS " +
                    "`index_reconciliation_dismissals_vehicle_id_start_date_end_date` " +
                    "ON `reconciliation_dismissals` (`vehicle_id`, `start_date`, `end_date`)",
            )
        }
    }

    /**
     * 9 → 10: veículo atual.
     *
     * `vehicles.is_current` — exatamente um veículo é atual quando há pelo
     * menos um cadastrado. Com um só, ele nasce atual; com histórico de
     * vários (só acontece em banco de teste anterior a esta versão), o mais
     * antigo vira atual, e o motorista troca pela tela se quiser outro.
     *
     * `work_sessions.vehicle_id` — ganhos passam a levar veículo, como
     * despesa já leva desde a v0.4.0. Aditiva: `ALTER TABLE ADD COLUMN` com
     * `REFERENCES`, sem precisar do padrão tabela-nova+cópia+troca da
     * migração 1→2 — não há coluna sendo removida, só acrescentada.
     * Lançamentos antigos ficam com `vehicle_id NULL`: não dá para
     * reconstruir qual carro era o atual antes desta versão.
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `vehicles` ADD COLUMN `is_current` INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                """
                UPDATE vehicles SET is_current = 1
                WHERE id = (SELECT id FROM vehicles ORDER BY created_at ASC LIMIT 1)
                """.trimIndent(),
            )
            db.execSQL(
                "ALTER TABLE `work_sessions` ADD COLUMN `vehicle_id` INTEGER " +
                    "REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL",
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_work_sessions_vehicle_id` " +
                    "ON `work_sessions` (`vehicle_id`)",
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
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
    )
}
