package com.driverpro.dashboard.domain

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

    // --- Periodo anterior, base da comparacao ---

    @Test
    fun `o anterior de hoje e ontem, e o de ontem e anteontem`() {
        assertEquals(
            DateRange.of(LocalDate.of(2026, 8, 11)),
            DashboardPeriod.Today.previousRangeAt(wednesday),
        )
        assertEquals(
            DateRange.of(LocalDate.of(2026, 8, 10)),
            DashboardPeriod.Yesterday.previousRangeAt(wednesday),
        )
    }

    @Test
    fun `o anterior da semana e a semana ISO inteira que veio antes`() {
        // Segunda a domingo, nao "sete dias atras a partir de hoje": a
        // comparacao precisa cobrir a mesma fatia de calendario.
        assertEquals(
            DateRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9)),
            DashboardPeriod.ThisWeek.previousRangeAt(wednesday),
        )
    }

    @Test
    fun `o anterior do mes e o mes inteiro que veio antes`() {
        assertEquals(
            DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31)),
            DashboardPeriod.ThisMonth.previousRangeAt(wednesday),
        )
        assertEquals(
            DateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
            DashboardPeriod.LastMonth.previousRangeAt(wednesday),
        )
    }

    @Test
    fun `mes anterior respeita o tamanho do mes e nao desloca dias`() {
        // 31 de marco: recuar 31 dias cairia em 28 de fevereiro, e a
        // comparacao passaria a incluir tres dias de janeiro e perder tres de
        // fevereiro. O anterior de marco e fevereiro inteiro, seja ele de 28
        // ou de 29 dias.
        val marco31 = LocalDate.of(2026, 3, 31)
        assertEquals(
            DateRange(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)),
            DashboardPeriod.ThisMonth.previousRangeAt(marco31),
        )

        // 2028 e bissexto: o mesmo codigo tem que devolver 29 dias.
        val marco31Bissexto = LocalDate.of(2028, 3, 31)
        assertEquals(
            DateRange(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)),
            DashboardPeriod.ThisMonth.previousRangeAt(marco31Bissexto),
        )
    }

    @Test
    fun `o anterior de um periodo personalizado tem o mesmo numero de dias`() {
        // Aqui o deslocamento por dias e o certo: intervalo escolhido a mao
        // nao tem semantica de calendario a preservar.
        val escolhido = DateRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16))
        val anterior = DashboardPeriod.Custom(escolhido).previousRangeAt(wednesday)

        assertEquals(DateRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 9)), anterior)
        assertEquals(escolhido.days, anterior.days)
    }

    @Test
    fun `periodo anterior nunca se sobrepoe ao atual`() {
        // Um dia de sobreposicao contaria o mesmo lancamento dos dois lados e
        // achataria toda variacao.
        listOf(
            DashboardPeriod.Today,
            DashboardPeriod.Yesterday,
            DashboardPeriod.ThisWeek,
            DashboardPeriod.ThisMonth,
            DashboardPeriod.LastMonth,
            DashboardPeriod.Custom(DateRange(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16))),
        ).forEach { period ->
            val atual = period.rangeAt(wednesday)
            val anterior = period.previousRangeAt(wednesday)

            assertTrue(
                "$period: o anterior precisa terminar antes de o atual comecar",
                anterior.end.isBefore(atual.start),
            )
        }
    }
}
