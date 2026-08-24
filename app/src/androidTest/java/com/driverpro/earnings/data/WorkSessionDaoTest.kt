package com.driverpro.earnings.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.driverpro.earnings.data.WorkSessionDao
import com.driverpro.core.database.DriverProDatabase
import com.driverpro.earnings.data.WorkSessionEntity
import com.driverpro.earnings.domain.Platform
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

/**
 * Testes de banco das sessões de trabalho (PRD §30): inserção, atualização,
 * exclusão e — o que mais importa para o dashboard — consulta por período.
 */
@RunWith(AndroidJUnit4::class)
class WorkSessionDaoTest {

    private lateinit var database: DriverProDatabase
    private lateinit var dao: WorkSessionDao

    private fun session(
        date: LocalDate,
        platform: Platform = Platform.UBER,
        revenueCents: Long = 32_050,
    ) = WorkSessionEntity(
        date = date,
        platform = platform,
        rides = 18,
        revenueCents = revenueCents,
        onlineMinutes = 500,
        distanceKm = 210,
        note = "",
        createdAt = Instant.ofEpochMilli(1_000_000),
    )

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DriverProDatabase::class.java,
        ).build()
        dao = database.workSessionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insereEDevolveOIdGerado() = runTest {
        val id = dao.insert(session(LocalDate.of(2026, 8, 11)))

        assertEquals(1, dao.count())
        assertEquals(32_050L, dao.findById(id)!!.revenueCents)
    }

    @Test
    fun preservaTodosOsCamposDaSessao() = runTest {
        val id = dao.insert(session(LocalDate.of(2026, 8, 11), Platform.NINETY_NINE))

        val salva = dao.findById(id)!!

        assertEquals(LocalDate.of(2026, 8, 11), salva.date)
        assertEquals(Platform.NINETY_NINE, salva.platform)
        assertEquals(18, salva.rides)
        assertEquals(500L, salva.onlineMinutes)
        assertEquals(210L, salva.distanceKm)
        assertEquals(Instant.ofEpochMilli(1_000_000), salva.createdAt)
    }

    @Test
    fun persisteTodasAsPlataformas() = runTest {
        Platform.entries.forEach { platform ->
            val id = dao.insert(session(LocalDate.of(2026, 8, 11), platform))
            assertEquals(platform, dao.findById(id)!!.platform)
        }
    }

    @Test
    fun atualizaSessaoExistente() = runTest {
        val id = dao.insert(session(LocalDate.of(2026, 8, 11)))

        dao.update(dao.findById(id)!!.copy(revenueCents = 40_000))

        assertEquals(40_000L, dao.findById(id)!!.revenueCents)
        assertEquals(1, dao.count())
    }

    @Test
    fun excluiPorId() = runTest {
        val id = dao.insert(session(LocalDate.of(2026, 8, 11)))

        dao.deleteById(id)

        assertNull(dao.findById(id))
        assertEquals(0, dao.count())
    }

    @Test
    fun observeAllOrdenaDaDataMaisRecenteParaAMaisAntiga() = runTest {
        dao.insert(session(LocalDate.of(2026, 8, 1), revenueCents = 100))
        dao.insert(session(LocalDate.of(2026, 8, 11), revenueCents = 200))
        dao.insert(session(LocalDate.of(2026, 8, 5), revenueCents = 300))

        val sessions = dao.observeAll().first()

        assertEquals(listOf(200L, 300L, 100L), sessions.map { it.revenueCents })
    }

    @Test
    fun observeBetweenIncluiAsDuasPontasDoPeriodo() = runTest {
        dao.insert(session(LocalDate.of(2026, 8, 1), revenueCents = 1))
        dao.insert(session(LocalDate.of(2026, 8, 5), revenueCents = 2))
        dao.insert(session(LocalDate.of(2026, 8, 10), revenueCents = 3))
        dao.insert(session(LocalDate.of(2026, 8, 15), revenueCents = 4))

        val sessions = dao.observeBetween(
            LocalDate.of(2026, 8, 5).toEpochDay(),
            LocalDate.of(2026, 8, 10).toEpochDay(),
        ).first()

        // 5 e 10 entram; 1 e 15 ficam de fora.
        assertEquals(listOf(3L, 2L), sessions.map { it.revenueCents })
    }

    @Test
    fun observeBetweenDeUmDiaSoDevolveAqueleDia() = runTest {
        val dia = LocalDate.of(2026, 8, 11)
        dao.insert(session(dia, revenueCents = 1))
        dao.insert(session(dia.minusDays(1), revenueCents = 2))

        val sessions = dao.observeBetween(dia.toEpochDay(), dia.toEpochDay()).first()

        assertEquals(listOf(1L), sessions.map { it.revenueCents })
    }

    @Test
    fun observeBetweenSemRegistrosNoPeriodoDevolveVazio() = runTest {
        dao.insert(session(LocalDate.of(2026, 8, 11)))

        val sessions = dao.observeBetween(
            LocalDate.of(2026, 7, 1).toEpochDay(),
            LocalDate.of(2026, 7, 31).toEpochDay(),
        ).first()

        assertEquals(emptyList<WorkSessionEntity>(), sessions)
    }

    @Test
    fun observeAllComecaVazio() = runTest {
        assertEquals(emptyList<WorkSessionEntity>(), dao.observeAll().first())
    }
}
