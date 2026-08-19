package com.driverpro.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun `of monta valor a partir de reais e centavos`() {
        assertEquals(28640L, Money.of(286, 40).cents)
        assertEquals(500L, Money.of(5, 0).cents)
        assertEquals(7L, Money.of(0, 7).cents)
    }

    @Test
    fun `of preserva o sinal em valores negativos`() {
        assertEquals(-28640L, Money.of(-286, 40).cents)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `of rejeita centavos fora do intervalo`() {
        Money.of(10, 100)
    }

    @Test
    fun `soma e subtracao operam em centavos inteiros`() {
        val faturamento = Money.of(320, 50)
        val despesas = Money.of(118, 75)

        assertEquals(Money.of(201, 75), faturamento - despesas)
        assertEquals(Money.of(439, 25), faturamento + despesas)
    }

    @Test
    fun `subtracao pode produzir lucro negativo`() {
        val lucro = Money.of(80, 0) - Money.of(120, 0)

        assertEquals(-4000L, lucro.cents)
        assertTrue(lucro.isNegative)
    }

    @Test
    fun `sum acumula sem perda de precisao`() {
        // 100 lancamentos de R$ 0,07. Em Double, 100 * 0.07 nao da exatamente 7,00.
        val valores = List(100) { Money(7L) }

        assertEquals(700L, Money.sum(valores).cents)
    }

    @Test
    fun `sum de colecao vazia e zero`() {
        assertEquals(Money.ZERO, Money.sum(emptyList()))
    }

    @Test
    fun `per divide o valor pela quantidade`() {
        val faturamento = Money.of(300, 0)

        assertEquals(Money.of(1, 50), faturamento.per(200.0))
    }

    @Test
    fun `per arredonda para o centavo mais proximo`() {
        // 10000 centavos / 3 = 3333,33... -> 3333
        assertEquals(Money(3333L), Money(10_000L).per(3.0))
        // 10001 centavos / 2 = 5000,5 -> 5001 (half-up)
        assertEquals(Money(5001L), Money(10_001L).per(2.0))
    }

    @Test
    fun `per retorna null quando a quantidade e zero`() {
        assertNull(Money.of(300, 0).per(0.0))
        assertNull(Money.of(300, 0).per(0L))
    }

    @Test
    fun `per retorna null quando a quantidade e negativa`() {
        assertNull(Money.of(300, 0).per(-10.0))
    }

    @Test
    fun `per retorna null para quantidades nao finitas`() {
        assertNull(Money.of(300, 0).per(Double.NaN))
        assertNull(Money.of(300, 0).per(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `per de valor zero e zero e nao null`() {
        // Faturamento zero com km rodados e uma informacao valida: R$ 0,00/km.
        assertEquals(Money.ZERO, Money.ZERO.per(150.0))
    }

    @Test
    fun `comparacao ordena por centavos`() {
        assertTrue(Money.of(10, 1) > Money.of(10, 0))
        assertTrue(Money.of(-1, 0) < Money.ZERO)
    }
}
