package com.driverprofit.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.entity.VehicleEntity
import com.driverprofit.domain.model.VehicleFuel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Testes de banco (PRD §30): inserção, atualização, exclusão e consulta.
 *
 * Roda em memória — cada teste começa com o banco vazio e nada toca o
 * armazenamento do aparelho.
 */
@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {

    private lateinit var database: DriverProfitDatabase
    private lateinit var dao: VehicleDao

    private val flexCar = VehicleEntity(
        name = "Onix branco",
        fuel = VehicleFuel.FLEX,
        createdAt = Instant.ofEpochMilli(1_000_000),
    )

    private val electricCar = VehicleEntity(
        name = "Dolphin",
        fuel = VehicleFuel.ELECTRIC,
        createdAt = Instant.ofEpochMilli(2_000_000),
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverProfitDatabase::class.java,
        ).build()
        dao = database.vehicleDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insereEDevolveOIdGerado() = runTest {
        val id = dao.insert(flexCar)

        assertEquals(1, dao.count())
        assertNotNull(dao.findById(id))
    }

    @Test
    fun preservaTodosOsCamposDoVeiculo() = runTest {
        val id = dao.insert(flexCar)

        val salvo = dao.findById(id)!!

        assertEquals("Onix branco", salvo.name)
        assertEquals(VehicleFuel.FLEX, salvo.fuel)
        assertEquals(Instant.ofEpochMilli(1_000_000), salvo.createdAt)
    }

    @Test
    fun persisteTodosOsTiposDeCombustivel() = runTest {
        VehicleFuel.entries.forEach { fuel ->
            val id = dao.insert(flexCar.copy(name = fuel.name, fuel = fuel))
            assertEquals(fuel, dao.findById(id)!!.fuel)
        }
    }

    @Test
    fun atualizaVeiculoExistente() = runTest {
        val id = dao.insert(flexCar)

        dao.update(flexCar.copy(id = id, fuel = VehicleFuel.CNG))

        assertEquals(VehicleFuel.CNG, dao.findById(id)!!.fuel)
        assertEquals(1, dao.count())
    }

    @Test
    fun excluiPorId() = runTest {
        val id = dao.insert(flexCar)

        dao.deleteById(id)

        assertNull(dao.findById(id))
        assertEquals(0, dao.count())
    }

    @Test
    fun observeAllOrdenaDoMaisRecenteParaOMaisAntigo() = runTest {
        dao.insert(flexCar)
        dao.insert(electricCar)

        val vehicles = dao.observeAll().first()

        assertEquals(listOf("Dolphin", "Onix branco"), vehicles.map { it.name })
    }

    @Test
    fun observeAllComecaVazio() = runTest {
        assertEquals(emptyList<VehicleEntity>(), dao.observeAll().first())
    }

    @Test
    fun findByIdInexistenteRetornaNulo() = runTest {
        assertNull(dao.findById(999L))
    }
}
