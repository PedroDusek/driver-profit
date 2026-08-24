package com.driverpro.expenses.domain

import java.time.LocalDate

/** Uma leitura já registrada: quando foi lançada e o que o painel marcava. */
data class OdometerReading(
    val date: LocalDate,
    val odometerKm: Long,
)

/**
 * As leituras que um veículo já tem, para validar uma nova contra elas.
 *
 * Kotlin puro, entregue pronto ao validador. É o `SaveExpenseUseCase` que
 * carrega do repositório — assim o validador continua testável sem banco, que é
 * o que permite exercitar cada regra abaixo com JUnit (PRD §29).
 */
data class OdometerHistory(val readings: List<OdometerReading>) {

    /**
     * `true` quando [odometerKm] não cabe entre as leituras vizinhas por data.
     *
     * Leituras do **mesmo dia** ficam de fora da comparação: duas paradas no
     * mesmo dia não têm ordem conhecida, e exigir coerência entre elas
     * recusaria lançamento legítimo.
     */
    fun contradicts(date: LocalDate?, odometerKm: Long): Boolean {
        if (date == null) return false

        val floor = readings.filter { it.date.isBefore(date) }.maxOfOrNull { it.odometerKm }
        val ceiling = readings.filter { it.date.isAfter(date) }.minOfOrNull { it.odometerKm }

        return (floor != null && odometerKm < floor) || (ceiling != null && odometerKm > ceiling)
    }

    companion object {
        /** Sem histórico: nenhuma regra de vizinhança se aplica. */
        val EMPTY = OdometerHistory(emptyList())
    }
}
