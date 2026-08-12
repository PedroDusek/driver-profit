package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import java.time.Instant
import java.time.LocalDate

/**
 * Uma sessão de trabalho — o que o motorista fez em um dia, numa plataforma
 * (PRD §15).
 *
 * O registro é por sessão e não por corrida: pedir para lançar corrida a
 * corrida seria trabalho demais para quem dirige o dia inteiro. O motorista
 * consulta o extrato da plataforma no fim do turno e lança o consolidado.
 *
 * Rodar na Uber e na 99 no mesmo dia são duas sessões — é isso que torna a
 * comparação entre plataformas possível depois.
 *
 * @param date dia de trabalho. Não é timestamp: o que importa é o dia, e é
 *   por dia que o dashboard vai agrupar.
 * @param distanceKm quilômetros rodados, em unidades inteiras. Meio quilômetro
 *   a mais ou a menos muda R$/km na terceira casa decimal — não vale a
 *   complexidade de fracionar.
 */
data class WorkSession(
    val id: Long = UNSAVED_ID,
    val date: LocalDate,
    val platform: Platform,
    val rides: Int,
    val revenue: Money,
    val onlineTime: WorkDuration,
    val distanceKm: Long,
    val note: String = "",
    val createdAt: Instant,
) {
    /**
     * Quanto a sessão rendeu por hora online.
     *
     * `null` quando não houve tempo registrado — uma sessão sem horas não tem
     * R$/hora igual a zero, ela simplesmente não tem R$/hora (PRD §21).
     */
    val revenuePerHour: Money? get() = revenue.per(onlineTime.toHours())

    /** Quanto a sessão rendeu por quilômetro. `null` sem quilometragem. */
    val revenuePerKm: Money? get() = revenue.per(distanceKm)

    /** Quanto rendeu cada corrida. `null` sem corridas. */
    val revenuePerRide: Money? get() = revenue.per(rides.toLong())

    companion object {
        /** Id atribuído a uma sessão que ainda não foi persistida. */
        const val UNSAVED_ID: Long = 0L

        /** Acima disso o campo de observação vira outra coisa. */
        const val MAX_NOTE_LENGTH: Int = 200
    }
}
