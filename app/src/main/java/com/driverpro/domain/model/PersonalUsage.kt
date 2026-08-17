package com.driverpro.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit

/** De onde veio a informação de quilometragem pessoal. */
enum class PersonalUsageSource {
    /** O motorista lançou a viagem: data, ou intervalo, e quilômetros. */
    DECLARED,

    /**
     * Sobra da conciliação por odômetro — o que a leitura do painel não
     * conseguiu explicar com jornadas lançadas nem com uso pessoal declarado.
     */
    RECONCILED,
}

/**
 * Quilômetros rodados fora do trabalho (PRD §22).
 *
 * Existe porque o custo/km divide despesa **total** por quilômetros: se o
 * combustível queimado no fim de semana entra no numerador e não no
 * denominador, o indicador central do produto fica inflado para quem também
 * usa o carro na vida.
 *
 * O registro é um **intervalo**, e não um dia, por dois motivos: uma viagem de
 * fim de semana cobre vários dias, e a sobra da conciliação cobre todo o
 * período entre duas leituras de odômetro.
 *
 * @param distanceKm quilômetros do intervalo inteiro, não por dia.
 */
data class PersonalUsage(
    val id: Long = UNSAVED_ID,
    /**
     * Anulável pelo mesmo motivo da despesa: excluir um veículo não pode
     * apagar histórico. O registro fica órfão e continua contando na
     * quilometragem total, que é o correto para quem trocou de carro. O
     * formulário, esse sim, exige a escolha.
     */
    val vehicleId: Long? = null,
    val range: DateRange,
    val distanceKm: Long,
    val source: PersonalUsageSource = PersonalUsageSource.DECLARED,
    val note: String = "",
    val createdAt: Instant,
) {

    /**
     * Quantos destes quilômetros caem dentro de [period].
     *
     * Distribuídos proporcionalmente aos dias: uma viagem de 1.200 km entre 12
     * e 14 de julho contribui com 400 km para um período que cobre só o dia 13.
     *
     * Ratear por dias é uma aproximação — ninguém roda a mesma quantidade todo
     * dia. Mas o motorista não vai lançar quilometragem dia a dia de uma
     * viagem, e a alternativa seria jogar os 1.200 km inteiros num dia
     * arbitrário, o que distorceria muito mais.
     */
    fun kilometersWithin(period: DateRange): Long {
        val start = maxOf(range.start, period.start)
        val end = minOf(range.end, period.end)
        if (end.isBefore(start)) return 0L

        val overlapDays = ChronoUnit.DAYS.between(start, end) + 1
        if (overlapDays >= range.days) return distanceKm

        return Math.round(distanceKm.toDouble() * overlapDays / range.days)
    }

    companion object {
        const val UNSAVED_ID: Long = 0L

        const val MAX_NOTE_LENGTH: Int = 200

        /**
         * Teto por lançamento.
         *
         * Mesmo raciocínio do teto do odômetro: não protege o banco, protege o
         * custo/km de um dígito a mais na digitação.
         */
        const val MAX_DISTANCE_KM: Long = 999_999L
    }
}

/** Uso pessoal em preenchimento, ainda não validado. */
data class PersonalUsageDraft(
    val id: Long = PersonalUsage.UNSAVED_ID,
    val vehicleId: Long? = null,
    val start: java.time.LocalDate? = null,
    val end: java.time.LocalDate? = null,
    val distanceKm: Long? = null,
    val note: String = "",
) {
    val isEditing: Boolean get() = id != PersonalUsage.UNSAVED_ID
}

/** Campo do formulário de uso pessoal ao qual um erro se refere. */
enum class PersonalUsageField {
    VEHICLE,
    START,
    END,
    DISTANCE,
    NOTE,
}

/** Motivo pelo qual um campo foi rejeitado. */
enum class PersonalUsageValidationError {
    REQUIRED,

    /** Data no futuro — não se registra viagem que ainda não aconteceu. */
    DATE_IN_FUTURE,

    /** Fim antes do início. */
    END_BEFORE_START,

    /** Zero, negativo ou absurdamente alto. */
    DISTANCE_OUT_OF_RANGE,

    TEXT_TOO_LONG,
}

data class PersonalUsageFieldError(
    val field: PersonalUsageField,
    val error: PersonalUsageValidationError,
)
