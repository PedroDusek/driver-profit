package com.driverpro.domain.model

import com.driverpro.core.domain.DateRange

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DashboardPeriodTest {

    // Uma quarta-feira, propositalmente no meio da semana e no meio do mes.
    private val wednesday = LocalDate.of(2026, 8, 12)

    @Test
    fun `hoje cobre um unico dia`() {
        val range = DashboardPeriod.Today.rangeAt(wednesday)

        assertEquals(DateRange(wednesday, wednesday), range)
        assertTrue(range.isSingleDay)
        assertEquals(1L, range.days)
    }

    @Test
    fun `ontem cobre o dia anterior`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 11), LocalDate.of(2026, 8, 11)),
            DashboardPeriod.Yesterday.rangeAt(wednesday),
        )
    }

    @Test
    fun `ontem atravessa a virada do mes`() {
        // Se "ontem" fosse dia menos um sem cuidado com o calendario, o
        // primeiro dia do mes devolveria dia zero.
        assertEquals(
            DateRange(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 31)),
            DashboardPeriod.Yesterday.rangeAt(LocalDate.of(2026, 8, 1)),
        )
    }

    @Test
    fun `a semana vai de segunda a domingo`() {
        val range = DashboardPeriod.ThisWeek.rangeAt(wednesday)

        assertEquals(LocalDate.of(2026, 8, 10), range.start)
        assertEquals(LocalDate.of(2026, 8, 16), range.end)
        assertEquals(7L, range.days)
    }

    @Test
    fun `no domingo a semana ainda e a que comecou na segunda`() {
        // O domingo fecha a semana ISO; se ele abrisse uma nova, o motorista
        // veria o dashboard zerar no fim de semana.
        val sunday = LocalDate.of(2026, 8, 16)

        assertEquals(
            DateRange(LocalDate.of(2026, 8, 10), sunday),
            DashboardPeriod.ThisWeek.rangeAt(sunday),
        )
    }

    @Test
    fun `na segunda a semana comeca no proprio dia`() {
        val monday = LocalDate.of(2026, 8, 10)

        assertEquals(
            DateRange(monday, LocalDate.of(2026, 8, 16)),
            DashboardPeriod.ThisWeek.rangeAt(monday),
        )
    }

    @Test
    fun `o mes vai do primeiro ao ultimo dia`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            DashboardPeriod.ThisMonth.rangeAt(wednesday),
        )
    }

    @Test
    fun `o mes anterior respeita meses curtos`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            DashboardPeriod.LastMonth.rangeAt(LocalDate.of(2026, 3, 15)),
        )
    }

    @Test
    fun `o mes anterior a partir do dia 31 nao escorrega`() {
        // 31 de marco menos um mes cai em 28 de fevereiro; o periodo tem que
        // ser fevereiro inteiro, e nao apenas o fim dele.
        assertEquals(
            DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            DashboardPeriod.LastMonth.rangeAt(LocalDate.of(2026, 3, 31)),
        )
    }

    @Test
    fun `o mes anterior atravessa a virada do ano`() {
        assertEquals(
            DateRange(LocalDate.of(2025, 12, 1), LocalDate.of(2025, 12, 31)),
            DashboardPeriod.LastMonth.rangeAt(LocalDate.of(2026, 1, 10)),
        )
    }

    @Test
    fun `periodo personalizado ignora o dia de referencia`() {
        val range = DateRange(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20))

        assertEquals(range, DashboardPeriod.Custom(range).rangeAt(wednesday))
    }

    @Test
    fun `os presets aparecem na ordem esperada`() {
        assertEquals(
            listOf(
                DashboardPeriod.Today,
                DashboardPeriod.Yesterday,
                DashboardPeriod.ThisWeek,
                DashboardPeriod.ThisMonth,
                DashboardPeriod.LastMonth,
            ),
            DashboardPeriod.PRESETS,
        )
    }

    @Test
    fun `intervalo conta as duas pontas`() {
        val range = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))

        assertEquals(31L, range.days)
        assertTrue(LocalDate.of(2026, 8, 1) in range)
        assertTrue(LocalDate.of(2026, 8, 31) in range)
        assertFalse(LocalDate.of(2026, 9, 1) in range)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `intervalo invertido e recusado`() {
        DateRange(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 8, 1))
    }
}
