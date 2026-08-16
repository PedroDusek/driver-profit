package com.driverprofit.domain.usecase

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseCategory
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.model.PersonalUsageSource
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class OdometerWindowTest {

    private var nextId = 1L

    private fun reading(odometerKm: Long, date: LocalDate, vehicleId: Long? = 1) = Expense(
        id = nextId++,
        vehicleId = vehicleId,
        date = date,
        category = ExpenseCategory.FUEL,
        amount = Money.of(200, 0),
        odometerKm = odometerKm,
        createdAt = Instant.EPOCH,
    )

    private fun session(date: LocalDate, km: Long) = WorkSession(
        id = nextId++,
        date = date,
        platform = Platform.UBER,
        rides = 10,
        revenue = Money.of(300, 0),
        onlineTime = WorkDuration.of(8, 0),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    private fun personal(start: LocalDate, end: LocalDate, km: Long) = PersonalUsage(
        id = nextId++,
        vehicleId = 1,
        range = DateRange(start, end),
        distanceKm = km,
        source = PersonalUsageSource.DECLARED,
        createdAt = Instant.EPOCH,
    )

    private fun pending(
        expenses: List<Expense>,
        sessions: List<WorkSession> = emptyList(),
        personalUsage: List<PersonalUsage> = emptyList(),
    ) = OdometerWindow.pending(expenses, sessions, personalUsage, vehicleId = 1)

    /** Ultima janela pendente, que e a que a tela mostra primeiro. */
    private fun latest(
        expenses: List<Expense>,
        sessions: List<WorkSession> = emptyList(),
        personalUsage: List<PersonalUsage> = emptyList(),
    ) = pending(expenses, sessions, personalUsage).lastOrNull()

    @Test
    fun `sem duas leituras nao ha o que conferir`() {
        assertTrue(pending(emptyList()).isEmpty())
        assertTrue(pending(listOf(reading(100_000, DIA_1))).isEmpty())
    }

    @Test
    fun `todas as janelas pendentes sao devolvidas e nao so a ultima`() {
        // Dois abastecimentos lancados antes de abrir o app fecham duas
        // janelas. Conferir so a mais nova abandonaria a anterior para sempre —
        // e quem lanca em lote, semanalmente, cai nisso toda semana.
        val janelas = pending(
            expenses = listOf(
                reading(100_000, DIA_1),
                reading(101_000, DIA_4),
                reading(101_800, DIA_6),
            ),
            sessions = listOf(session(DIA_2, 600), session(DIA_5, 500)),
        )

        assertEquals(2, janelas.size)
        assertEquals(400L, janelas[0].unexplainedKilometers)
        assertEquals(300L, janelas[1].unexplainedKilometers)
    }

    @Test
    fun `janela ja resolvida sai da lista`() {
        // O uso pessoal gravado por uma conciliacao anterior e descontado como
        // declarado, e a sobra daquela janela volta a zero.
        val janelas = pending(
            expenses = listOf(
                reading(100_000, DIA_1),
                reading(101_000, DIA_4),
                reading(101_800, DIA_6),
            ),
            sessions = listOf(session(DIA_2, 600), session(DIA_5, 500)),
            personalUsage = listOf(personal(DIA_2, DIA_4, 400)),
        )

        assertEquals(1, janelas.size)
        assertEquals(300L, janelas.single().unexplainedKilometers)
    }

    @Test
    fun `a sobra e o painel menos o que foi lancado`() {
        // Caso real do primeiro teste em aparelho: o painel andou muito mais do
        // que as jornadas explicam.
        val r = latest(
            expenses = listOf(reading(100_550, DIA_2), reading(105_000, DIA_8)),
            sessions = listOf(session(DIA_4, 102)),
        )!!

        assertEquals(4_450L, r.odometerKilometers)
        assertEquals(102L, r.workKilometers)
        assertEquals(4_348L, r.unexplainedKilometers)
        assertTrue(r.hasUnexplained)
        assertFalse(r.hasDivergence)
    }

    @Test
    fun `uso pessoal ja declarado abate a sobra`() {
        // Sem esse desconto a viagem lancada a mao apareceria de novo dentro da
        // sobra, e seria contada duas vezes. Zerada a sobra, a janela deixa de
        // ser pendente e some da lista — que e como uma conciliacao resolvida
        // para de aparecer na tela.
        val janelas = pending(
            expenses = listOf(reading(100_000, DIA_1), reading(101_000, DIA_4)),
            sessions = listOf(session(DIA_2, 600)),
            personalUsage = listOf(personal(DIA_3, DIA_3, 400)),
        )

        assertTrue(janelas.isEmpty())
    }

    @Test
    fun `lancado a mais que o painel vira divergencia e nao uso pessoal negativo`() {
        // Antes da v0.9.1 isto era zerado em silencio.
        val r = latest(
            expenses = listOf(reading(100_000, DIA_1), reading(100_700, DIA_4)),
            sessions = listOf(session(DIA_2, 800)),
        )!!

        assertEquals(-100L, r.unexplainedKilometers)
        assertTrue(r.hasDivergence)
        assertFalse(r.hasUnexplained)
    }

    @Test
    fun `janelas encadeadas cancelam erro de alocacao`() {
        // A jornada do DIA_5 pertence a segunda janela. Se ela tivesse sido
        // lancada no DIA_3, a primeira sobra subiria e a segunda desceria na
        // mesma medida — e o total continuaria certo. Isso so vale porque a
        // negativa e preservada; com piso em zero, o desequilibrio passageiro
        // viraria quilometragem pessoal gravada para sempre.
        val expenses = listOf(
            reading(100_000, DIA_1),
            reading(101_000, DIA_4),
            reading(101_700, DIA_6),
        )
        val sessions = listOf(session(DIA_2, 900), session(DIA_5, 800))

        val segunda = OdometerWindow.pending(expenses, sessions, emptyList(), 1).last()
        assertEquals(700L, segunda.odometerKilometers)
        assertEquals(800L, segunda.workKilometers)
        assertEquals(-100L, segunda.unexplainedKilometers)

        val primeira = OdometerWindow.pending(expenses.dropLast(1), sessions, emptyList(), 1)
            .single()
        assertEquals(100L, primeira.unexplainedKilometers)

        // Somadas, as duas janelas dizem a verdade: 1.700 km de painel para
        // 1.700 km de jornada, nenhum quilometro pessoal.
        assertEquals(
            0L,
            primeira.unexplainedKilometers!! + segunda.unexplainedKilometers!!,
        )
    }

    @Test
    fun `a janela comeca no dia seguinte a leitura anterior`() {
        // A jornada do dia da leitura anterior pertence a janela que terminou
        // nela. Conta-la de novo aqui seria dupla contagem.
        val r = latest(
            expenses = listOf(reading(100_000, DIA_2), reading(100_500, DIA_4)),
            sessions = listOf(session(DIA_2, 300), session(DIA_3, 200)),
        )!!

        assertEquals(DIA_3, r.period.start)
        assertEquals(DIA_4, r.period.end)
        assertEquals(200L, r.workKilometers)
    }

    @Test
    fun `duas leituras no mesmo dia caem na janela de um dia so`() {
        val r = latest(
            expenses = listOf(reading(100_000, DIA_2), reading(100_300, DIA_2)),
            sessions = listOf(session(DIA_2, 250)),
        )!!

        assertEquals(DIA_2, r.period.start)
        assertEquals(DIA_2, r.period.end)
        // A jornada do dia entra, o que encolhe a sobra — errar para menos
        // devolve o comportamento antigo em vez de inventar km pessoal.
        assertEquals(50L, r.unexplainedKilometers)
    }

    @Test
    fun `a ordem e a do odometro e nao a da data`() {
        // Lancar hoje a nota da semana passada e rotina.
        val r = latest(
            expenses = listOf(reading(101_000, DIA_2), reading(100_000, DIA_8)),
        )!!

        assertEquals(1_000L, r.odometerKilometers)
    }

    @Test
    fun `leitura de outro veiculo nao entra na conta`() {
        val r = latest(
            expenses = listOf(
                reading(100_000, DIA_1),
                reading(101_000, DIA_4),
                reading(500_000, DIA_4, vehicleId = 2),
            ),
        )!!

        assertEquals(1_000L, r.odometerKilometers)
    }

    private companion object {
        val DIA_1: LocalDate = LocalDate.of(2026, 8, 1)
        val DIA_2: LocalDate = LocalDate.of(2026, 8, 2)
        val DIA_3: LocalDate = LocalDate.of(2026, 8, 3)
        val DIA_4: LocalDate = LocalDate.of(2026, 8, 4)
        val DIA_5: LocalDate = LocalDate.of(2026, 8, 5)
        val DIA_6: LocalDate = LocalDate.of(2026, 8, 6)
        val DIA_8: LocalDate = LocalDate.of(2026, 8, 8)
    }
}
