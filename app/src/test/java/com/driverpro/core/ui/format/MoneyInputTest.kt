package com.driverpro.core.ui.format

import com.driverpro.core.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes do campo monetário.
 *
 * O grupo de regressão no fim existe por causa de um defeito real: o campo
 * exibe o valor formatado, e `"R$ 0,00"` contém três dígitos. Reextrair todos
 * os dígitos do texto formatado travava o campo em `R$ 0,00`.
 */
class MoneyInputTest {

    @Test
    fun `campo vazio nao tem valor`() {
        assertNull(MoneyInput.toMoney(""))
        assertEquals("", MoneyInput.display(""))
    }

    @Test
    fun `digitos sao interpretados como centavos`() {
        assertEquals(Money.of(320, 50), MoneyInput.toMoney("32050"))
        assertEquals("R$ 320,50", MoneyInput.display("32050"))
    }

    @Test
    fun `zero digitado e valor zero e nao campo vazio`() {
        assertEquals(Money.ZERO, MoneyInput.toMoney("0"))
        assertEquals("R$ 0,00", MoneyInput.display("0"))
    }

    // --- Digitação ---

    @Test
    fun `primeiro digito entra num campo vazio`() {
        assertEquals("3", MoneyInput.onTextChanged("", "3"))
    }

    @Test
    fun `digitos vao se acumulando`() {
        var digits = ""
        // O campo devolve o texto formatado com o novo dígito no fim.
        digits = MoneyInput.onTextChanged(digits, "3")
        digits = MoneyInput.onTextChanged(digits, "R$ 0,03" + "2")
        digits = MoneyInput.onTextChanged(digits, "R$ 0,32" + "0")

        assertEquals("320", digits)
        assertEquals("R$ 3,20", MoneyInput.display(digits))
    }

    @Test
    fun `limite de digitos e respeitado`() {
        assertEquals("123", MoneyInput.onTextChanged("12", "R$ 0,12" + "34", maxDigits = 3))
    }

    // --- Apagar ---

    @Test
    fun `apagar remove um digito`() {
        // "R$ 3,20" tem 3 dígitos; o campo devolve 2 após o backspace.
        assertEquals("32", MoneyInput.onTextChanged("320", "R$ 3,2"))
    }

    @Test
    fun `apagar ate o fim esvazia o campo`() {
        var digits = "320"
        repeat(3) { digits = MoneyInput.onTextChanged(digits, MoneyInput.display(digits).dropLast(1)) }

        assertEquals("", digits)
        assertNull(MoneyInput.toMoney(digits))
    }

    // --- Regressão do defeito relatado ---

    @Test
    fun `apagar em R$ 0,00 esvazia o campo em vez de trava-lo`() {
        // O defeito: "R$ 0,00" tem três dígitos de formatação, então
        // reextrair tudo devolvia "00", que normalizava de volta para "0".
        // O campo ficava presoem R$ 0,00 e nunca esvaziava.
        assertEquals("", MoneyInput.onTextChanged("0", "R$ 0,0"))
    }

    @Test
    fun `digitar em R$ 0,00 acrescenta centavo em vez de real`() {
        // O outro sintoma: tocar no meio de "R$ 0,00" e digitar 3 produzia
        // R$ 30,00 em vez de R$ 0,03, porque a posição do dígito no texto
        // formatado mudava o resultado. O trecho inserido agora é achado por
        // diferença, então é o 3 que entra — não um zero da formatação.
        val digits = MoneyInput.onTextChanged("0", "R$ 30,00")

        assertEquals(Money.of(0, 3), MoneyInput.toMoney(digits))
    }

    @Test
    fun `edicao que nao muda a quantidade de digitos preserva o estado`() {
        // Mexer só na formatação não pode alterar o valor.
        assertEquals("320", MoneyInput.onTextChanged("320", "R$3,20"))
    }

    @Test
    fun `zeros a esquerda nao alteram o valor`() {
        assertEquals(Money.of(0, 5), MoneyInput.toMoney("005"))
    }
}
