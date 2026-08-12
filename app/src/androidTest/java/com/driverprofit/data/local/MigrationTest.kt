package com.driverprofit.data.local

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

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
