package com.driverpro.expenses.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OdometerHistoryTest {

    private val history = OdometerHistory(
        listOf(
            OdometerReading(LocalDate.of(2026, 8, 10), 100_550),
            OdometerReading(LocalDate.of(2026, 8, 16), 105_000),
        ),
    )

    @Test
    fun `leitura entre as vizinhas e aceita`() {
        assertFalse(history.contradicts(LocalDate.of(2026, 8, 12), 102_000))
    }

    @Test
    fun `leitura abaixo da anterior e recusada`() {
        // Digito trocado: 92.000 num dia posterior a uma leitura de 100.550.
        assertTrue(history.contradicts(LocalDate.of(2026, 8, 12), 92_000))
    }

    @Test
    fun `leitura acima da posterior e recusada`() {
        // 110.000 em 12/08 contradiz os 105.000 de 16/08.
        assertTrue(history.contradicts(LocalDate.of(2026, 8, 12), 110_000))
    }

    @Test
    fun `leitura igual a vizinha e aceita`() {
        // Carro parado entre dois lancamentos e possivel.
        assertFalse(history.contradicts(LocalDate.of(2026, 8, 12), 100_550))
        assertFalse(history.contradicts(LocalDate.of(2026, 8, 12), 105_000))
    }

    @Test
    fun `leitura do mesmo dia nao entra na comparacao`() {
        // Duas paradas no mesmo dia nao tem ordem conhecida; exigir coerencia
        // entre elas recusaria lancamento legitimo.
        assertFalse(history.contradicts(LocalDate.of(2026, 8, 16), 104_000))
    }

    @Test
    fun `lancamento a frente de tudo so tem piso`() {
        assertFalse(history.contradicts(LocalDate.of(2026, 8, 20), 200_000))
        assertTrue(history.contradicts(LocalDate.of(2026, 8, 20), 104_000))
    }

    @Test
    fun `sem historico nada e recusado`() {
        assertFalse(OdometerHistory.EMPTY.contradicts(LocalDate.of(2026, 8, 12), 1))
    }

    @Test
    fun `sem data nao ha o que comparar`() {
        assertFalse(history.contradicts(null, 1))
    }
}
