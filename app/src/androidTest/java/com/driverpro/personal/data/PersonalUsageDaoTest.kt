package com.driverpro.personal.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverpro.personal.data.PersonalUsageDao
import com.driverpro.vehicle.data.VehicleDao
import com.driverpro.core.database.DriverProDatabase
import com.driverpro.personal.data.PersonalUsageEntity
import com.driverpro.vehicle.data.VehicleEntity
import com.driverpro.personal.domain.PersonalUsageSource
import com.driverpro.vehicle.domain.VehicleFuel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Testes de banco do uso pessoal (PRD §30). */
@RunWith(AndroidJUnit4::class)
class PersonalUsageDaoTest {

    private lateinit var database: DriverProDatabase
    private lateinit var dao: PersonalUsageDao
    private lateinit var vehicleDao: VehicleDao

    private fun usage(
        start: LocalDate,
        end: LocalDate = start,
        km: Long = 100,
        vehicleId: Long? = null,
    ) = PersonalUsageEntity(
        vehicleId = vehicleId,
        startDate = start,
        endDate = end,
        distanceKm = km,
        source = PersonalUsageSource.DECLARED,
        note = "",
        createdAt = Instant.ofEpochMilli(1_000),
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverProDatabase::class.java,
        ).build()
        dao = database.personalUsageDao()
        vehicleDao = database.vehicleDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun observeOverlappingPegaViagemQueAtravessaAViradaDoMes() = runTest {
        // Este é o caso que uma consulta "começa dentro do período" perderia,
        // e é justamente o que deixaria o custo/km de agosto inflado.
        dao.insert(usage(LocalDate.of(2026, 7, 30), LocalDate.of(2026, 8, 2), km = 400))

        val julho = dao.observeOverlapping(
            LocalDate.of(2026, 7, 1).toEpochDay(),
            LocalDate.of(2026, 7, 31).toEpochDay(),
        ).first()
        val agosto = dao.observeOverlapping(
            LocalDate.of(2026, 8, 1).toEpochDay(),
            LocalDate.of(2026, 8, 31).toEpochDay(),
        ).first()

        assertEquals(1, julho.size)
        assertEquals(1, agosto.size)
    }

    @Test
    fun observeOverlappingIgnoraViagemForaDoPeriodo() = runTest {
        dao.insert(usage(LocalDate.of(2026, 6, 10)))

        val agosto = dao.observeOverlapping(
            LocalDate.of(2026, 8, 1).toEpochDay(),
            LocalDate.of(2026, 8, 31).toEpochDay(),
        ).first()

        assertTrue(agosto.isEmpty())
    }

    @Test
    fun findOverlappingForVehicleFiltraPeloCarro() = runTest {
        val onix = vehicleDao.insert(
            VehicleEntity(name = "Onix", fuel = VehicleFuel.FLEX, createdAt = Instant.EPOCH),
        )
        val dolphin = vehicleDao.insert(
            VehicleEntity(name = "Dolphin", fuel = VehicleFuel.ELECTRIC, createdAt = Instant.EPOCH),
        )
        dao.insert(usage(LocalDate.of(2026, 8, 10), vehicleId = onix))
        dao.insert(usage(LocalDate.of(2026, 8, 12), vehicleId = dolphin))

        val doOnix = dao.findOverlappingForVehicle(
            onix,
            LocalDate.of(2026, 8, 1).toEpochDay(),
            LocalDate.of(2026, 8, 31).toEpochDay(),
        )

        assertEquals(1, doOnix.size)
        assertEquals(onix, doOnix.single().vehicleId)
    }

    @Test
    fun excluirVeiculoPreservaOUsoPessoal() = runTest {
        val carro = vehicleDao.insert(
            VehicleEntity(name = "Onix", fuel = VehicleFuel.FLEX, createdAt = Instant.EPOCH),
        )
        dao.insert(usage(LocalDate.of(2026, 8, 10), km = 300, vehicleId = carro))

        vehicleDao.deleteById(carro)

        // Trocar de carro não pode apagar histórico: o registro fica órfão e
        // continua contando na quilometragem total.
        val restante = dao.observeAll().first().single()
        assertNull(restante.vehicleId)
        assertEquals(300L, restante.distanceKm)
    }

    @Test
    fun observeAllOrdenaDaMaisRecenteParaAMaisAntiga() = runTest {
        dao.insert(usage(LocalDate.of(2026, 8, 1), km = 1))
        dao.insert(usage(LocalDate.of(2026, 8, 11), km = 2))

        assertEquals(listOf(2L, 1L), dao.observeAll().first().map { it.distanceKm })
    }
}
