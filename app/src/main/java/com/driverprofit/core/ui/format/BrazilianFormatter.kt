package com.driverprofit.core.ui.format

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Formatação para o padrão brasileiro — **camada de apresentação apenas**
 * (PRD §26: a conversão de centavos para texto acontece só aqui).
 *
 * A formatação é feita manualmente em vez de usar `NumberFormat`: o resultado
 * de `NumberFormat` varia entre a JVM do desktop e o Android (separador de
 * milhar, espaço não-quebrável após "R$"), o que tornaria os testes
 * unitários instáveis. Aqui a saída é determinística.
 */
object BrazilianFormatter {

    // Padrão puramente numérico: independe de Locale, o que mantém a saída
    // idêntica no aparelho e na JVM dos testes.
    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /** `Money(28640)` → `"R$ 286,40"`; `Money(-500)` → `"-R$ 5,00"`. */
    fun money(value: Money): String {
        val negative = value.isNegative
        val absoluteCents = if (negative) -value.cents else value.cents
        val reais = absoluteCents / 100
        val cents = absoluteCents % 100
        val sign = if (negative) "-" else ""
        return "${sign}R$ ${groupThousands(reais)},${cents.toString().padStart(2, '0')}"
    }

    /**
     * Valor por unidade, com o sufixo já embutido: `"R$ 2,35/km"`.
     *
     * [value] nulo representa indicador indisponível — período sem
     * quilômetros, sem horas ou sem corridas (PRD §21). Nesse caso devolve
     * [UNAVAILABLE] em vez de "R$ 0,00", que seria uma informação falsa.
     */
    fun moneyPerUnit(value: Money?, unit: String): String =
        if (value == null) UNAVAILABLE else "${money(value)}/$unit"

    /** `WorkDuration(500)` → `"8h 20min"`. */
    fun duration(value: WorkDuration): String {
        val hours = value.wholeHours
        val minutes = value.remainingMinutes
        return when {
            hours == 0L -> "${minutes}min"
            minutes == 0L -> "${hours}h"
            else -> "${hours}h ${minutes}min"
        }
    }

    /** `LocalDate.of(2026, 8, 11)` → `"11/08/2026"`. */
    fun date(value: LocalDate): String = value.format(dateFormatter)

    /** Quilometragem inteira com separador de milhar: `"50.350 km"`. */
    fun kilometers(value: Long): String = "${groupThousands(value)} km"

    private fun groupThousands(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        return digits.reversed()
            .chunked(3)
            .joinToString(".")
            .reversed()
    }

    /** Texto exibido quando um indicador não pode ser calculado. */
    const val UNAVAILABLE: String = "—"
}
