package com.driverprofit.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverprofit.data.local.dao.ExpenseDao
import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.database.DriverProfitDatabase
import com.driverprofit.data.local.entity.ExpenseEntity
import com.driverprofit.data.local.entity.VehicleEntity
import com.driverprofit.domain.model.ChargingLocation
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.MaintenanceCategory
import com.driverprofit.domain.model.VehicleFuel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

/** Testes de banco das despesas (PRD §30). */
@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var database: DriverProfitDatabase
    private lateinit var dao: ExpenseDao
    private lateinit var vehicleDao: VehicleDao

    private fun expense(
        date: LocalDate = LocalDate.of(2026, 8, 11),
        category: ExpenseCategory = ExpenseCategory.TOLL,
        amountCents: Long = 1_250,
        vehicleId: Long? = null,
        odometerKm: Long? = null,
    ) = ExpenseEntity(
        vehicleId = vehicleId,
        date = date,
        category = category,
        amountCents = amountCents,
        description = "",
        odometerKm = odometerKm,
        createdAt = Instant.ofEpochMilli(1_000_000),
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverProfitDatabase::class.java,
        ).build()
        dao = database.expenseDao()
        vehicleDao = database.vehicleDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insereEDevolveOIdGerado() = runTest {
        val id = dao.insert(expense())

        assertEquals(1, dao.count())
        assertEquals(1_250L, dao.findById(id)!!.amountCents)
    }

    @Test
    fun persisteODetalheDeAbastecimento() = runTest {
        val id = dao.insert(
            expense(category = ExpenseCategory.FUEL, amountCents = 21_000).copy(
                fuelType = FuelType.ETHANOL,
                quantityThousandths = 35_400,
                place = "Posto Shell",
            ),
        )

        val salva = dao.findById(id)!!

        assertEquals(FuelType.ETHANOL, salva.fuelType)
        assertEquals(35_400L, salva.quantityThousandths)
        assertEquals("Posto Shell", salva.place)
    }

    @Test
    fun persisteODetalheDeRecarga() = runTest {
        val id = dao.insert(
            expense(category = ExpenseCategory.CHARGING, amountCents = 0).copy(
                quantityThousandths = 42_000,
                chargingLocation = ChargingLocation.PUBLIC,
            ),
        )

        val salva = dao.findById(id)!!

        // Recarga gratuita: valor zero com kWh preenchido (PRD §11).
        assertEquals(0L, salva.amountCents)
        assertEquals(42_000L, salva.quantityThousandths)
        assertEquals(ChargingLocation.PUBLIC, salva.chargingLocation)
    }

    @Test
    fun persisteODetalheDeManutencao() = runTest {
        val id = dao.insert(
            expense(category = ExpenseCategory.MAINTENANCE, amountCents = 32_000).copy(
                maintenanceCategory = MaintenanceCategory.OIL,
                place = "Oficina",
            ),
        )

        assertEquals(MaintenanceCategory.OIL, dao.findById(id)!!.maintenanceCategory)
    }

    @Test
    fun persisteTodasAsCategorias() = runTest {
        ExpenseCategory.entries.forEach { category ->
            val id = dao.insert(expense(category = category))
            assertEquals(category, dao.findById(id)!!.category)
        }
    }

    @Test
    fun excluirVeiculoNaoApagaAsDespesas() = runTest {
        val vehicleId = vehicleDao.insert(
            VehicleEntity(
                name = "Onix branco",
                fuel = VehicleFuel.FLEX,
                createdAt = Instant.ofEpochMilli(1_000),
            ),
        )
        val expenseId = dao.insert(expense(vehicleId = vehicleId))

        vehicleDao.deleteById(vehicleId)

        // ON DELETE SET NULL: trocar de carro não pode apagar o histórico
        // financeiro. A despesa fica órfã e continua somando.
        val salva = dao.findById(expenseId)
        assertEquals(1, dao.count())
        assertNull(salva!!.vehicleId)
        assertEquals(1_250L, salva.amountCents)
    }

    @Test
    fun atualizaDespesaExistente() = runTest {
        val id = dao.insert(expense())

        dao.update(dao.findById(id)!!.copy(amountCents = 2_000))

        assertEquals(2_000L, dao.findById(id)!!.amountCents)
    }

    @Test
    fun excluiPorId() = runTest {
        val id = dao.insert(expense())

        dao.deleteById(id)

        assertNull(dao.findById(id))
        assertEquals(0, dao.count())
    }

    @Test
    fun observeBetweenIncluiAsDuasPontasDoPeriodo() = runTest {
        dao.insert(expense(date = LocalDate.of(2026, 8, 1), amountCents = 1))
        dao.insert(expense(date = LocalDate.of(2026, 8, 5), amountCents = 2))
        dao.insert(expense(date = LocalDate.of(2026, 8, 10), amountCents = 3))
        dao.insert(expense(date = LocalDate.of(2026, 8, 15), amountCents = 4))

        val despesas = dao.observeBetween(
            LocalDate.of(2026, 8, 5).toEpochDay(),
            LocalDate.of(2026, 8, 10).toEpochDay(),
        ).first()

        assertEquals(listOf(3L, 2L), despesas.map { it.amountCents })
    }

    @Test
    fun observeAllOrdenaDaDataMaisRecenteParaAMaisAntiga() = runTest {
        dao.insert(expense(date = LocalDate.of(2026, 8, 1), amountCents = 1))
        dao.insert(expense(date = LocalDate.of(2026, 8, 11), amountCents = 2))

        assertEquals(listOf(2L, 1L), dao.observeAll().first().map { it.amountCents })
    }

    @Test
    fun observeAllComecaVazio() = runTest {
        assertEquals(emptyList<ExpenseEntity>(), dao.observeAll().first())
    }

    // --- Odômetro (v0.6.0) ---

    @Test
    fun odometroAtualEOMaiorEnaoODoLancamentoMaisRecente() = runTest {
        val carro = vehicleDao.insert(
            VehicleEntity(
                name = "Onix",
                fuel = VehicleFuel.FLEX,
                createdAt = Instant.ofEpochMilli(1),
            ),
        )
        // Lançar hoje o abastecimento da semana passada é comum: ordenar por
        // data devolveria uma leitura menor que a real.
        dao.insert(
            expense(
                date = LocalDate.of(2026, 8, 11),
                vehicleId = carro,
                odometerKm = 44_000,
            ),
        )
        dao.insert(
            expense(
                date = LocalDate.of(2026, 8, 5),
                vehicleId = carro,
                odometerKm = 45_500,
            ),
        )

        assertEquals(45_500L, dao.observeLatestOdometer(carro).first())
    }

    @Test
    fun odometroAtualENuloSemNenhumaLeitura() = runTest {
        val carro = vehicleDao.insert(
            VehicleEntity(
                name = "Onix",
                fuel = VehicleFuel.FLEX,
                createdAt = Instant.ofEpochMilli(1),
            ),
        )
        // Despesa sem leitura: é o caso de todo o histórico anterior à v0.6.0.
        dao.insert(expense(vehicleId = carro, odometerKm = null))

        assertNull(dao.observeLatestOdometer(carro).first())
    }

    @Test
    fun observeOdometersAgrupaPorVeiculo() = runTest {
        val onix = vehicleDao.insert(
            VehicleEntity(name = "Onix", fuel = VehicleFuel.FLEX, createdAt = Instant.EPOCH),
        )
        val dolphin = vehicleDao.insert(
            VehicleEntity(name = "Dolphin", fuel = VehicleFuel.ELECTRIC, createdAt = Instant.EPOCH),
        )
        dao.insert(expense(vehicleId = onix, odometerKm = 44_000))
        dao.insert(expense(vehicleId = onix, odometerKm = 45_500))
        dao.insert(expense(vehicleId = dolphin, odometerKm = 12_800))
        // Sem veículo e sem leitura: não pode aparecer no agrupamento.
        dao.insert(expense(vehicleId = null, odometerKm = null))

        val odometros = dao.observeOdometers().first().associate { it.vehicleId to it.odometerKm }

        assertEquals(mapOf(onix to 45_500L, dolphin to 12_800L), odometros)
    }
}
