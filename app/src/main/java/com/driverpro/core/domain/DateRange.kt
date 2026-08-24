package com.driverpro.core.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Intervalo de dias **fechado nas duas pontas** — `start` e `end` entram.
 *
 * Fechado nos dois lados porque é assim que as consultas por período já
 * funcionam (`observeSessionsBetween`, `observeExpensesBetween`) e é assim que
 * o motorista lê "de 1 a 31 de agosto": os dois dias contam. Um intervalo
 * semiaberto obrigaria a somar um dia em cada chamada, e um dia esquecido em
 * um único lugar produziria um indicador silenciosamente errado.
 */
data class DateRange(val start: LocalDate, val end: LocalDate) {

    init {
        require(!end.isBefore(start)) {
            "fim do período ($end) não pode ser anterior ao início ($start)"
        }
    }

    /** Quantidade de dias do intervalo, contando as duas pontas. */
    val days: Long get() = ChronoUnit.DAYS.between(start, end) + 1

    /** `true` quando o intervalo cobre um único dia. */
    val isSingleDay: Boolean get() = start == end

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= end

    companion object {
        /** Intervalo de um dia só. */
        fun of(day: LocalDate): DateRange = DateRange(day, day)
    }
}
