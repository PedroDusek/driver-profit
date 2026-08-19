package com.driverpro.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverpro.data.local.dao.MaintenanceScheduleDao
import com.driverpro.data.local.dao.VehicleDao
import com.driverpro.data.local.database.DriverProDatabase
import com.driverpro.data.local.entity.MaintenanceScheduleEntity
import com.driverpro.data.local.entity.VehicleEntity
import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.VehicleFuel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/** Testes de banco dos intervalos de manutenção (PRD §30). */
@RunWith(AndroidJUnit4::class)
class MaintenanceScheduleDaoTest {

    private lateinit var database: DriverProDatabase
    private lateinit var dao: MaintenanceScheduleDao
    private lateinit var vehicleDao: VehicleDao

    private fun schedule(
        vehicleId: Long,
        item: MaintenanceItem,
        intervalKm: Long,
        monitored: Boolean = true,
    ) = MaintenanceScheduleEntity(
        vehicleId = vehicleId,
        item = item,
        intervalKm = intervalKm,
        monitored = monitored,
        createdAt = Instant.ofEpochMilli(1_000),
    )

    private suspend fun insertVehicle(name: String = "Onix branco"): Long =
        vehicleDao.insert(
            VehicleEntity(
                name = name,
                fuel = VehicleFuel.FLEX,
                createdAt = Instant.ofEpochMilli(1_000),
            ),
        )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverProDatabase::class.java,
        ).build()
        dao = database.maintenanceScheduleDao()
        vehicleDao = database.vehicleDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun tabelaComecaVaziaPorqueAusenciaSignificaPadrao() = runTest {
        insertVehicle()

        // Um veículo recém-cadastrado já é acompanhado sem ter linha nenhuma:
        // é o enum que dá o intervalo até alguém discordar dele.
        assertEquals(0, dao.count())
        assertTrue(dao.observeAll().first().isEmpty())
    }

    @Test
    fun upsertSubstituiOIntervaloDoMesmoItem() = runTest {
        val vehicleId = insertVehicle()

        dao.upsert(schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 5_000))
        dao.upsert(schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 8_000))

        val gravados = dao.observeForVehicle(vehicleId).first()
        assertEquals(1, gravados.size)
        assertEquals(8_000L, gravados.single().intervalKm)
    }

    @Test
    fun itensDiferentesConvivemNoMesmoVeiculo() = runTest {
        val vehicleId = insertVehicle()

        dao.upsert(schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 5_000))
        dao.upsert(schedule(vehicleId, MaintenanceItem.TIRES, intervalKm = 50_000))

        assertEquals(2, dao.observeForVehicle(vehicleId).first().size)
    }

    @Test
    fun cadaVeiculoTemOSeuIntervalo() = runTest {
        val onix = insertVehicle("Onix branco")
        val corolla = insertVehicle("Corolla")

        dao.upsert(schedule(onix, MaintenanceItem.OIL, intervalKm = 5_000))
        dao.upsert(schedule(corolla, MaintenanceItem.OIL, intervalKm = 15_000))

        assertEquals(5_000L, dao.observeForVehicle(onix).first().single().intervalKm)
        assertEquals(15_000L, dao.observeForVehicle(corolla).first().single().intervalKm)
    }

    @Test
    fun apagarDevolveOItemAoPadrao() = runTest {
        val vehicleId = insertVehicle()
        dao.upsert(schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 5_000))

        dao.deleteFor(vehicleId, MaintenanceItem.OIL)

        assertTrue(dao.observeForVehicle(vehicleId).first().isEmpty())
    }

    @Test
    fun itemDesligadoContinuaGravado() = runTest {
        val vehicleId = insertVehicle()

        dao.upsert(
            schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 5_000, monitored = false),
        )

        val gravado = dao.observeForVehicle(vehicleId).first().single()
        // Desligar não é apagar: o intervalo escolhido precisa estar lá quando
        // ele voltar atrás.
        assertEquals(false, gravado.monitored)
        assertEquals(5_000L, gravado.intervalKm)
    }

    @Test
    fun excluirOVeiculoLevaOsIntervalosJunto() = runTest {
        val vehicleId = insertVehicle()
        dao.upsert(schedule(vehicleId, MaintenanceItem.OIL, intervalKm = 5_000))

        vehicleDao.deleteById(vehicleId)

        // CASCADE: preferência sobre um carro que não existe mais não é
        // histórico financeiro, e não tem por que sobreviver.
        assertEquals(0, dao.count())
    }

    @Test
    fun oEnumEGravadoPeloNomeENaoPeloOrdinal() = runTest {
        val vehicleId = insertVehicle()
        dao.upsert(schedule(vehicleId, MaintenanceItem.BRAKES, intervalKm = 30_000))

        assertEquals(
            MaintenanceItem.BRAKES,
            dao.observeForVehicle(vehicleId).first().single().item,
        )
    }
}
