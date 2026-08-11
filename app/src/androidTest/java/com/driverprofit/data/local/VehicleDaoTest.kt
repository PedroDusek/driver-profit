package com.driverprofit.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.entity.VehicleEntity
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.VehiclePowertrain
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
        brand = "Chevrolet",
        model = "Onix",
        year = 2020,
        initialOdometerKm = 50_000,
        powertrain = VehiclePowertrain.COMBUSTION,
        combustionFuel = CombustionFuel.FLEX,
        chargingCapability = null,
        createdAt = Instant.ofEpochMilli(1_000_000),
    )

    private val electricCar = VehicleEntity(
        brand = "BYD",
        model = "Dolphin",
        year = 2024,
        initialOdometerKm = 1_200,
        powertrain = VehiclePowertrain.ELECTRIC,
        combustionFuel = null,
        chargingCapability = ChargingCapability.PLUG_IN,
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

        assertEquals("Chevrolet", salvo.brand)
        assertEquals("Onix", salvo.model)
        assertEquals(2020, salvo.year)
        assertEquals(50_000L, salvo.initialOdometerKm)
        assertEquals(VehiclePowertrain.COMBUSTION, salvo.powertrain)
        assertEquals(CombustionFuel.FLEX, salvo.combustionFuel)
        assertEquals(Instant.ofEpochMilli(1_000_000), salvo.createdAt)
    }

    @Test
    fun aceitaCombustivelNuloParaEletrico() = runTest {
        val id = dao.insert(electricCar)

        val salvo = dao.findById(id)!!

        assertNull(salvo.combustionFuel)
        assertEquals(ChargingCapability.PLUG_IN, salvo.chargingCapability)
    }

    @Test
    fun atualizaVeiculoExistente() = runTest {
        val id = dao.insert(flexCar)

        dao.update(flexCar.copy(id = id, initialOdometerKm = 61_500))

        assertEquals(61_500L, dao.findById(id)!!.initialOdometerKm)
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

        assertEquals(listOf("BYD", "Chevrolet"), vehicles.map { it.brand })
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
