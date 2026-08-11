package com.driverprofit.core.ui.format

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BrazilianFormatterTest {

    @Test
    fun `money usa virgula decimal e prefixo em reais`() {
        assertEquals("R$ 286,40", BrazilianFormatter.money(Money(28_640)))
        assertEquals("R$ 0,00", BrazilianFormatter.money(Money.ZERO))
        assertEquals("R$ 0,07", BrazilianFormatter.money(Money(7)))
    }

    @Test
    fun `money agrupa milhares com ponto`() {
        assertEquals("R$ 1.234,56", BrazilianFormatter.money(Money(123_456)))
        assertEquals("R$ 12.345,67", BrazilianFormatter.money(Money(1_234_567)))
        assertEquals("R$ 1.000.000,00", BrazilianFormatter.money(Money(100_000_000)))
    }

    @Test
    fun `money mantem o sinal antes do prefixo`() {
        assertEquals("-R$ 5,00", BrazilianFormatter.money(Money(-500)))
    }

    @Test
    fun `moneyPerUnit acrescenta a unidade`() {
        assertEquals("R$ 2,35/km", BrazilianFormatter.moneyPerUnit(Money(235), "km"))
    }

    @Test
    fun `moneyPerUnit exibe indisponivel quando o indicador nao existe`() {
        // Periodo sem quilometros: R$/km nao e zero, e indisponivel (PRD §21).
        assertEquals("—", BrazilianFormatter.moneyPerUnit(null, "km"))
    }

    @Test
    fun `duration exibe horas e minutos`() {
        assertEquals("8h 20min", BrazilianFormatter.duration(WorkDuration(500)))
    }

    @Test
    fun `duration omite parte vazia`() {
        assertEquals("8h", BrazilianFormatter.duration(WorkDuration.of(8, 0)))
        assertEquals("45min", BrazilianFormatter.duration(WorkDuration(45)))
        assertEquals("0min", BrazilianFormatter.duration(WorkDuration.ZERO))
    }

    @Test
    fun `date usa o formato brasileiro`() {
        assertEquals("11/08/2026", BrazilianFormatter.date(LocalDate.of(2026, 8, 11)))
        assertEquals("01/01/2026", BrazilianFormatter.date(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `kilometers agrupa milhares`() {
        assertEquals("50.350 km", BrazilianFormatter.kilometers(50_350))
        assertEquals("0 km", BrazilianFormatter.kilometers(0))
    }
}
