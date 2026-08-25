package com.driverpro.core.ui.format

import com.driverpro.core.domain.Quantity

/**
 * Entrada e saída de quantidades digitadas pelo motorista.
 *
 * Diferente do valor em dinheiro, que é digitado em centavos, a quantidade é
 * digitada como o motorista lê na bomba: `35,478`. Forçar dígitos puros aqui
 * faria alguém teclar `35` esperando 35 litros e obter 0,035.
 *
 * O parser é próprio, e não `toDouble()`, por dois motivos: aceitar vírgula
 * como separador decimal sem depender de `Locale`, e nunca passar por ponto
 * flutuante no caminho até o inteiro persistido.
 */
object QuantityInput {

    private const val MAX_DECIMALS = 3
    private const val MAX_WHOLE_DIGITS = 6

    /**
     * Converte o texto digitado em [Quantity].
     *
     * Devolve `null` para texto vazio ou inválido — o chamador decide se isso
     * é "ainda não preencheu" ou erro.
     */
    fun parse(text: String): Quantity? {
        val cleaned = text.trim().replace(',', '.')
        if (cleaned.isEmpty()) return null

        val parts = cleaned.split('.')
        if (parts.size > 2) return null

        val whole = parts[0].ifEmpty { "0" }
        val decimals = parts.getOrNull(1).orEmpty()

        if (!whole.all(Char::isDigit) || !decimals.all(Char::isDigit)) return null
        if (whole.length > MAX_WHOLE_DIGITS || decimals.length > MAX_DECIMALS) return null

        val wholeValue = whole.toLongOrNull() ?: return null
        // Completa até milésimos: "5" vira 500, "05" vira 050, "478" vira 478.
        val thousandths = decimals.padEnd(MAX_DECIMALS, '0').toLongOrNull() ?: return null

        return Quantity.of(wholeValue, thousandths)
    }

    /**
     * Mantém apenas o que pode fazer parte de um número decimal, para filtrar
     * a digitação em tempo real sem rejeitar um texto ainda incompleto como
     * `"35,"`.
     */
    fun sanitize(text: String): String {
        val filtered = text.filter { it.isDigit() || it == ',' || it == '.' }
            .replace('.', ',')
        val firstSeparator = filtered.indexOf(',')
        if (firstSeparator < 0) return filtered.take(MAX_WHOLE_DIGITS)

        val whole = filtered.substring(0, firstSeparator).take(MAX_WHOLE_DIGITS)
        val decimals = filtered.substring(firstSeparator + 1)
            .replace(",", "")
            .take(MAX_DECIMALS)
        return "$whole,$decimals"
    }

    /** `Quantity(35478)` → `"35,478"`; `Quantity(35000)` → `"35"`. */
    fun display(value: Quantity): String {
        if (value.fraction == 0L) return value.whole.toString()
        val decimals = value.fraction.toString().padStart(MAX_DECIMALS, '0').trimEnd('0')
        return "${value.whole},$decimals"
    }
}
