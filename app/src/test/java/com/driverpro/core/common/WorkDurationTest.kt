package com.driverpro.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkDurationTest {

    @Test
    fun `of converte horas e minutos para minutos`() {
        assertEquals(500L, WorkDuration.of(8, 20).minutes)
        assertEquals(0L, WorkDuration.of(0, 0).minutes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `of rejeita minutos fora do intervalo`() {
        WorkDuration.of(8, 60)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duracao negativa e rejeitada`() {
        WorkDuration(-1)
    }

    @Test
    fun `decompoe minutos em horas cheias e resto`() {
        val duracao = WorkDuration(500)

        assertEquals(8L, duracao.wholeHours)
        assertEquals(20L, duracao.remainingMinutes)
    }

    @Test
    fun `toHours devolve horas decimais para uso como divisor`() {
        assertEquals(8.0, WorkDuration.of(8, 0).toHours(), 1e-9)
        assertEquals(8.5, WorkDuration.of(8, 30).toHours(), 1e-9)
    }

    @Test
    fun `soma acumula jornadas`() {
        val semana = WorkDuration.sum(
            listOf(
                WorkDuration.of(8, 20),
                WorkDuration.of(7, 40),
                WorkDuration.of(9, 0),
            ),
        )

        assertEquals(WorkDuration.of(25, 0), semana)
    }

    @Test
    fun `sum de colecao vazia e zero`() {
        assertTrue(WorkDuration.sum(emptyList()).isZero)
    }

    @Test
    fun `ganho por hora usa a duracao como divisor`() {
        val faturamento = Money.of(250, 0)
        val jornada = WorkDuration.of(8, 20)

        // 25000 centavos / 8,3333h = 3000 centavos = R$ 30,00/h
        assertEquals(Money.of(30, 0), faturamento.per(jornada.toHours()))
    }

    @Test
    fun `ganho por hora e indisponivel quando nao houve tempo online`() {
        val faturamento = Money.of(250, 0)

        assertEquals(null, faturamento.per(WorkDuration.ZERO.toHours()))
    }
}
