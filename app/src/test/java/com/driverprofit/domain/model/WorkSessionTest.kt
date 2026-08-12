package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * Cálculos básicos da sessão (PRD §21).
 *
 * São os indicadores que dão sentido ao produto — e os primeiros lugares onde
 * uma divisão por zero apareceria.
 */
class WorkSessionTest {

    private fun session(
        revenue: Money = Money.of(320, 50),
        onlineTime: WorkDuration = WorkDuration.of(8, 20),
        distanceKm: Long = 210,
        rides: Int = 18,
    ) = WorkSession(
        date = LocalDate.of(2026, 8, 11),
        platform = Platform.UBER,
        rides = rides,
        revenue = revenue,
        onlineTime = onlineTime,
        distanceKm = distanceKm,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `ganho por hora divide faturamento pelas horas online`() {
        // 25000 centavos / 8,3333h = 3000 centavos
        val s = session(revenue = Money.of(250, 0), onlineTime = WorkDuration.of(8, 20))

        assertEquals(Money.of(30, 0), s.revenuePerHour)
    }

    @Test
    fun `ganho por km divide faturamento pela distancia`() {
        val s = session(revenue = Money.of(300, 0), distanceKm = 200)

        assertEquals(Money.of(1, 50), s.revenuePerKm)
    }

    @Test
    fun `ganho por corrida divide faturamento pelas corridas`() {
        val s = session(revenue = Money.of(300, 0), rides = 20)

        assertEquals(Money.of(15, 0), s.revenuePerRide)
    }

    @Test
    fun `sem tempo online nao existe ganho por hora`() {
        // Nao e zero: e indisponivel. A UI exibe um traco (PRD 21).
        assertNull(session(onlineTime = WorkDuration.ZERO).revenuePerHour)
    }

    @Test
    fun `sem distancia nao existe ganho por km`() {
        assertNull(session(distanceKm = 0).revenuePerKm)
    }

    @Test
    fun `sem corridas nao existe ganho por corrida`() {
        assertNull(session(rides = 0).revenuePerRide)
    }

    @Test
    fun `faturamento zero com jornada registrada rende zero e nao indisponivel`() {
        // Rodar o dia inteiro e nao faturar nada e informacao valida.
        val s = session(revenue = Money.ZERO)

        assertEquals(Money.ZERO, s.revenuePerHour)
        assertEquals(Money.ZERO, s.revenuePerKm)
        assertEquals(Money.ZERO, s.revenuePerRide)
    }

    @Test
    fun `indicadores arredondam para o centavo mais proximo`() {
        // 10000 centavos / 3 corridas = 3333,33... -> 3333
        val s = session(revenue = Money(10_000), rides = 3)

        assertEquals(Money(3333), s.revenuePerRide)
    }
}
