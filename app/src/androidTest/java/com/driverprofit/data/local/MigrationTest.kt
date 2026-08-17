package com.driverprofit.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.database.Migrations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Testes de migração de schema (PRD §45).
 *
 * O motorista não pode perder o histórico financeiro por causa de uma mudança
 * de estrutura, então cada migração precisa provar que os dados atravessam a
 * transição intactos — não basta o banco abrir.
 *
 * Depende dos schemas exportados em `app/schemas/`, expostos como assets do
 * androidTest em `app/build.gradle.kts`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        DriverProfitDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migracao1para2PreservaOsVeiculos() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT id, name, fuel, created_at FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1L, cursor.getLong(0))
            // Marca e modelo viram o nome, que e como o motorista ja
            // reconhecia o carro na lista.
            assertEquals("Chevrolet Onix", cursor.getString(1))
            assertEquals("FLEX", cursor.getString(2))
            assertEquals(1000L, cursor.getLong(3))
            assertFalse(cursor.moveToNext())
        }
    }

    @Test
    fun migracao1para2DerivaCombustivelDaPropulsaoEletrica() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'BYD', 'Dolphin', 2024, 1200, 'ELECTRIC', NULL, 'PLUG_IN', 2000),
                    (2, 'Toyota', 'Corolla', 2023, 30000, 'HYBRID', 'FLEX', 'NONE', 3000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT name, fuel FROM vehicles ORDER BY id").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("BYD Dolphin", cursor.getString(0))
            assertEquals("ELECTRIC", cursor.getString(1))

            assertTrue(cursor.moveToNext())
            assertEquals("Toyota Corolla", cursor.getString(0))
            assertEquals("HYBRID", cursor.getString(1))
        }
    }

    @Test
    fun migracao1para2FuncionaComBancoVazio() {
        helper.createDatabase(TEST_DB, 1).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, Migrations.MIGRATION_1_2)

        db.query("SELECT COUNT(*) FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao2para3CriaWorkSessionsSemTocarEmVehicles() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO vehicles (id, name, fuel, created_at) " +
                    "VALUES (1, 'Onix branco', 'FLEX', 1000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, Migrations.MIGRATION_2_3)

        // A migração é aditiva: o veículo cadastrado tem que continuar lá.
        db.query("SELECT name FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Onix branco", cursor.getString(0))
        }
        db.query("SELECT COUNT(*) FROM work_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao1para3AtravessaAsDuasEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        // Quem instalou a v0.1.0 e pulou a v0.2.1 precisa chegar inteiro na v3.
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            3,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    @Test
    fun migracao3para4CriaExpensesSemTocarNoQueJaExiste() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO vehicles (id, name, fuel, created_at) " +
                    "VALUES (1, 'Onix branco', 'FLEX', 1000)",
            )
            db.execSQL(
                """
                INSERT INTO work_sessions
                    (id, date, platform, rides, revenue_cents, online_minutes,
                     distance_km, note, created_at)
                VALUES (1, 20000, 'UBER', 18, 32050, 500, 210, '', 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true, Migrations.MIGRATION_3_4)

        // Aditiva: veículo e jornada continuam intactos.
        db.query("SELECT COUNT(*) FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM work_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM expenses").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao4para5AcrescentaOdometroSemTocarNasDespesas() {
        helper.createDatabase(TEST_DB, 4).use { db ->
            db.execSQL(
                """
                INSERT INTO expenses
                    (id, vehicle_id, date, category, amount_cents, description, created_at)
                VALUES (1, NULL, 20000, 'FUEL', 21000, 'Posto Shell', 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migrations.MIGRATION_4_5)

        // A despesa continua inteira, e a leitura fica NULL — que é a verdade:
        // ninguém registrou odômetro antes da v0.6.0. Inventar um valor aqui
        // envenenaria consumo estimado e alerta de manutenção.
        db.query("SELECT amount_cents, description, odometer_km FROM expenses").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(21000, cursor.getInt(0))
            assertEquals("Posto Shell", cursor.getString(1))
            assertTrue(cursor.isNull(2))
        }
    }

    @Test
    fun migracao4para5AceitaOdometroDepoisDeMigrar() {
        helper.createDatabase(TEST_DB, 4).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, Migrations.MIGRATION_4_5)

        db.execSQL(
            """
            INSERT INTO expenses
                (id, vehicle_id, date, category, amount_cents, description,
                 odometer_km, created_at)
            VALUES (1, NULL, 20000, 'FUEL', 21000, '', 45200, 1000)
            """.trimIndent(),
        )

        db.query("SELECT odometer_km FROM expenses WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(45200, cursor.getInt(0))
        }
    }

    @Test
    fun migracao5para6CriaUsoPessoalSemTocarNoQueJaExiste() {
        helper.createDatabase(TEST_DB, 5).use { db ->
            db.execSQL(
                """
                INSERT INTO expenses
                    (id, vehicle_id, date, category, amount_cents, description,
                     odometer_km, created_at)
                VALUES (1, NULL, 20000, 'FUEL', 21000, '', 45200, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migrations.MIGRATION_5_6)

        db.query("SELECT odometer_km FROM expenses WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(45200, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM personal_usage").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao5para6AceitaLancamentoDeUsoPessoal() {
        helper.createDatabase(TEST_DB, 5).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, Migrations.MIGRATION_5_6)

        db.execSQL(
            """
            INSERT INTO personal_usage
                (id, vehicle_id, start_date, end_date, distance_km, source, note, created_at)
            VALUES (1, NULL, 20000, 20002, 1200, 'DECLARED', 'Viagem', 1000)
            """.trimIndent(),
        )

        db.query(
            "SELECT start_date, end_date, distance_km, source FROM personal_usage WHERE id = 1",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(20000, cursor.getInt(0))
            assertEquals(20002, cursor.getInt(1))
            assertEquals(1200, cursor.getInt(2))
            assertEquals("DECLARED", cursor.getString(3))
        }
    }

    @Test
    fun migracao6para7CriaIntervalosSemTocarNoQueJaExiste() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL(
                "INSERT INTO vehicles (id, name, fuel, created_at) " +
                    "VALUES (1, 'Onix branco', 'FLEX', 1000)",
            )
            db.execSQL(
                """
                INSERT INTO expenses
                    (id, vehicle_id, date, category, amount_cents, description,
                     maintenance_category, odometer_km, created_at)
                VALUES (1, 1, 20000, 'MAINTENANCE', 30000, '', 'OIL', 100000, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migrations.MIGRATION_6_7)

        // O marco dos alertas vem do histórico que já existe — a migração não
        // precisa criar dado nenhum para a v0.9.0 funcionar.
        db.query("SELECT maintenance_category, odometer_km FROM expenses WHERE id = 1")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("OIL", cursor.getString(0))
                assertEquals(100000, cursor.getInt(1))
            }

        // Nasce vazia: linha ausente significa "intervalo padrão do app", então
        // o veículo já existente passa a ser acompanhado sem a migração gravar
        // uma decisão que o motorista não tomou.
        db.query("SELECT COUNT(*) FROM maintenance_schedules").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao6para7AceitaIntervaloEImpedeItemDuplicado() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL(
                "INSERT INTO vehicles (id, name, fuel, created_at) " +
                    "VALUES (1, 'Onix branco', 'FLEX', 1000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migrations.MIGRATION_6_7)

        db.execSQL(
            """
            INSERT INTO maintenance_schedules
                (id, vehicle_id, item, interval_km, monitored, created_at)
            VALUES (1, 1, 'OIL', 5000, 1, 1000)
            """.trimIndent(),
        )

        db.query("SELECT item, interval_km, monitored FROM maintenance_schedules WHERE id = 1")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("OIL", cursor.getString(0))
                assertEquals(5000, cursor.getInt(1))
                assertEquals(1, cursor.getInt(2))
            }

        // O índice único é o que garante um intervalo por item, e é ele que
        // sustenta o REPLACE do DAO.
        var recusou = false
        try {
            db.execSQL(
                """
                INSERT INTO maintenance_schedules
                    (id, vehicle_id, item, interval_km, monitored, created_at)
                VALUES (2, 1, 'OIL', 8000, 1, 2000)
                """.trimIndent(),
            )
        } catch (expected: SQLiteConstraintException) {
            recusou = true
        }
        assertTrue(recusou)
    }

    @Test
    fun migracao6para7ApagaIntervalosAoExcluirOVeiculo() {
        helper.createDatabase(TEST_DB, 6).use { db ->
            db.execSQL(
                "INSERT INTO vehicles (id, name, fuel, created_at) " +
                    "VALUES (1, 'Onix branco', 'FLEX', 1000)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, Migrations.MIGRATION_6_7)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL(
            """
            INSERT INTO maintenance_schedules
                (id, vehicle_id, item, interval_km, monitored, created_at)
            VALUES (1, 1, 'OIL', 5000, 1, 1000)
            """.trimIndent(),
        )

        db.execSQL("DELETE FROM vehicles WHERE id = 1")

        // CASCADE, e não SET NULL como em expenses: aquelas guardam histórico
        // financeiro e sobrevivem à troca de carro; esta guarda uma preferência
        // sobre um carro que deixou de existir.
        db.query("SELECT COUNT(*) FROM maintenance_schedules").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migracao7para8AcrescentaCompetenciaSemTocarNasDespesas() {
        helper.createDatabase(TEST_DB, 7).use { db ->
            db.execSQL(
                """
                INSERT INTO expenses
                    (id, vehicle_id, date, category, amount_cents, description,
                     odometer_km, created_at)
                VALUES (1, NULL, 20000, 'VEHICLE_TAX', 120000, 'IPVA', NULL, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, Migrations.MIGRATION_7_8)

        // A despesa continua inteira, e a competência fica NULL — que é a
        // verdade: ninguém declarou intervalo antes da v0.10.0. NULL significa
        // "conta no próprio dia", o comportamento que já existia.
        db.query("SELECT amount_cents, accrual_start, accrual_end FROM expenses WHERE id = 1")
            .use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(120000, cursor.getInt(0))
                assertTrue(cursor.isNull(1))
                assertTrue(cursor.isNull(2))
            }
    }

    @Test
    fun migracao7para8AceitaCompetenciaDepoisDeMigrar() {
        helper.createDatabase(TEST_DB, 7).close()

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, Migrations.MIGRATION_7_8)

        db.execSQL(
            """
            INSERT INTO expenses
                (id, vehicle_id, date, category, amount_cents, description,
                 accrual_start, accrual_end, created_at)
            VALUES (1, NULL, 20468, 'VEHICLE_TAX', 120000, 'IPVA', 20454, 20818, 1000)
            """.trimIndent(),
        )

        db.query("SELECT accrual_start, accrual_end FROM expenses WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(20454, cursor.getInt(0))
            assertEquals(20818, cursor.getInt(1))
        }
    }

    @Test
    fun migracao1para8AtravessaTodasAsEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6,
            Migrations.MIGRATION_6_7,
            Migrations.MIGRATION_7_8,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    @Test
    fun migracao1para7AtravessaTodasAsEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6,
            Migrations.MIGRATION_6_7,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    @Test
    fun migracao1para6AtravessaTodasAsEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            6,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
            Migrations.MIGRATION_5_6,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    @Test
    fun migracao1para5AtravessaTodasAsEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        // Quem instalou a v0.1.0 e só agora atualiza precisa chegar inteiro.
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
            Migrations.MIGRATION_4_5,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    @Test
    fun migracao1para4AtravessaTodasAsEtapas() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO vehicles
                    (id, brand, model, year, initial_odometer_km, powertrain,
                     combustion_fuel, charging_capability, created_at)
                VALUES
                    (1, 'Chevrolet', 'Onix', 2020, 50000, 'COMBUSTION', 'FLEX', NULL, 1000)
                """.trimIndent(),
            )
        }

        // Quem instalou a v0.1.0 e só agora atualiza precisa chegar inteiro.
        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            4,
            true,
            Migrations.MIGRATION_1_2,
            Migrations.MIGRATION_2_3,
            Migrations.MIGRATION_3_4,
        )

        db.query("SELECT name, fuel FROM vehicles").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Chevrolet Onix", cursor.getString(0))
            assertEquals("FLEX", cursor.getString(1))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
