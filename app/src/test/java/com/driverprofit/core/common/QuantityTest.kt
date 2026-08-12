package com.driverprofit.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityTest {

    @Test
    fun `of monta a partir de inteiro e milesimos`() {
        assertEquals(35_478L, Quantity.of(35, 478).thousandths)
        assertEquals(35_000L, Quantity.of(35).thousandths)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `of rejeita milesimos fora do intervalo`() {
        Quantity.of(35, 1000)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `quantidade negativa e rejeitada`() {
        Quantity(-1)
    }

    @Test
    fun `decompoe em parte inteira e decimal`() {
        val q = Quantity(35_478)

        assertEquals(35L, q.whole)
        assertEquals(478L, q.fraction)
    }

    @Test
    fun `soma acumula sem perda de precisao`() {
        // 10 abastecimentos de 0,1 L. Em Double, 10 * 0.1 nao da exatamente 1.
        val total = Quantity.sum(List(10) { Quantity.of(0, 100) })

        assertEquals(Quantity.of(1), total)
    }

    @Test
    fun `preco por unidade divide o valor pela quantidade`() {
        val valor = Money.of(210, 0)
        val litros = Quantity.of(35)

        // 21000 centavos / 35 L = 600 centavos = R$ 6,00/L
        assertEquals(Money.of(6, 0), valor.per(litros.toUnits()))
    }

    @Test
    fun `preco por unidade com decimais arredonda para o centavo`() {
        val valor = Money.of(210, 0)
        val litros = Quantity.of(35, 400)

        // 21000 / 35,4 = 593,22... -> 593
        assertEquals(Money(593), valor.per(litros.toUnits()))
    }

    @Test
    fun `recarga gratuita tem preco zero por unidade e nao indisponivel`() {
        // kWh > 0 com valor 0 e caso previsto no PRD 11.
        assertEquals(Money.ZERO, Money.ZERO.per(Quantity.of(42).toUnits()))
    }

    @Test
    fun `sem quantidade nao existe preco por unidade`() {
        assertNull(Money.of(210, 0).per(Quantity.ZERO.toUnits()))
    }
}
