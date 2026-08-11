package com.driverprofit.core.common

/**
 * Tempo online do motorista, em **minutos** (PRD §27).
 *
 * `8h20` é armazenado como `500`. Horas decimais (`8.333…`) nunca são
 * persistidas: elas só aparecem em [toHours], no momento de dividir
 * faturamento por tempo.
 */
@JvmInline
value class WorkDuration(val minutes: Long) : Comparable<WorkDuration> {

    init {
        require(minutes >= 0) { "duração não pode ser negativa: $minutes" }
    }

    operator fun plus(other: WorkDuration): WorkDuration =
        WorkDuration(minutes + other.minutes)

    override fun compareTo(other: WorkDuration): Int = minutes.compareTo(other.minutes)

    val isZero: Boolean get() = minutes == 0L

    /** Parte inteira de horas — `500 min` → `8`. */
    val wholeHours: Long get() = minutes / MINUTES_PER_HOUR

    /** Minutos restantes após [wholeHours] — `500 min` → `20`. */
    val remainingMinutes: Long get() = minutes % MINUTES_PER_HOUR

    /**
     * Horas em ponto flutuante, usadas apenas como divisor em R$/hora.
     * Não persistir este valor.
     */
    fun toHours(): Double = minutes.toDouble() / MINUTES_PER_HOUR

    companion object {
        const val MINUTES_PER_HOUR: Long = 60L

        val ZERO: WorkDuration = WorkDuration(0L)

        /** `of(8, 20)` = 500 minutos. */
        fun of(hours: Long, minutes: Long): WorkDuration {
            require(minutes in 0 until MINUTES_PER_HOUR) {
                "minutos deve estar entre 0 e 59, recebido: $minutes"
            }
            return WorkDuration(hours * MINUTES_PER_HOUR + minutes)
        }

        fun sum(values: Iterable<WorkDuration>): WorkDuration =
            WorkDuration(values.sumOf { it.minutes })
    }
}
