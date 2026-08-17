package com.driverpro.core.ui.format

import com.driverpro.core.common.Quantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuantityInputTest {

    @Test
    fun `aceita virgula como separador decimal`() {
        assertEquals(Quantity.of(35, 400), QuantityInput.parse("35,4"))
    }

    @Test
    fun `aceita ponto como separador decimal`() {
        assertEquals(Quantity.of(35, 400), QuantityInput.parse("35.4"))
    }

    @Test
    fun `completa os milesimos a direita`() {
        // "5" depois da virgula e meio decimo, nao cinco milesimos.
        assertEquals(Quantity.of(35, 500), QuantityInput.parse("35,5"))
        assertEquals(Quantity.of(35, 50), QuantityInput.parse("35,05"))
        assertEquals(Quantity.of(35, 478), QuantityInput.parse("35,478"))
    }

    @Test
    fun `numero inteiro nao e interpretado como milesimos`() {
        // O erro que motivou nao usar entrada em digitos puros aqui.
        assertEquals(Quantity.of(35), QuantityInput.parse("35"))
    }

    @Test
    fun `aceita valor comecando com separador`() {
        assertEquals(Quantity.of(0, 500), QuantityInput.parse(",5"))
    }

    @Test
    fun `texto vazio nao e quantidade`() {
        assertNull(QuantityInput.parse(""))
        assertNull(QuantityInput.parse("   "))
    }

    @Test
    fun `texto invalido nao e quantidade`() {
        assertNull(QuantityInput.parse("abc"))
        assertNull(QuantityInput.parse("35,4,5"))
        assertNull(QuantityInput.parse("35,4567"))
    }

    @Test
    fun `zero e quantidade valida no parser`() {
        // Quem recusa zero e a validacao, nao o parser.
        assertEquals(Quantity.ZERO, QuantityInput.parse("0"))
    }

    @Test
    fun `sanitize remove caracteres invalidos e normaliza o separador`() {
        assertEquals("35,4", QuantityInput.sanitize("3a5.4x"))
    }

    @Test
    fun `sanitize mantem um unico separador`() {
        assertEquals("35,45", QuantityInput.sanitize("35,4,5"))
    }

    @Test
    fun `sanitize limita as casas decimais`() {
        assertEquals("35,478", QuantityInput.sanitize("35,47899"))
    }

    @Test
    fun `sanitize preserva texto incompleto durante a digitacao`() {
        assertEquals("35,", QuantityInput.sanitize("35,"))
    }

    @Test
    fun `display omite decimais quando nao ha`() {
        assertEquals("35", QuantityInput.display(Quantity.of(35)))
    }

    @Test
    fun `display corta zeros a direita`() {
        assertEquals("35,4", QuantityInput.display(Quantity.of(35, 400)))
        assertEquals("35,05", QuantityInput.display(Quantity.of(35, 50)))
        assertEquals("35,478", QuantityInput.display(Quantity.of(35, 478)))
    }

    @Test
    fun `parse e display sao simetricos`() {
        listOf("35", "35,4", "35,05", "0,5", "1234,567").forEach { texto ->
            assertEquals(texto, QuantityInput.display(QuantityInput.parse(texto)!!))
        }
    }
}
