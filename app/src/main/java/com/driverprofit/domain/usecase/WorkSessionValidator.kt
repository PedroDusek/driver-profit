package com.driverprofit.domain.usecase

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.model.WorkSessionDraft
import com.driverprofit.domain.model.WorkSessionField
import com.driverprofit.domain.model.WorkSessionFieldError
import com.driverprofit.domain.model.WorkSessionValidationError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Regras de validação da sessão de trabalho.
 *
 * Classe pura, com [Clock] injetado para que a checagem de data futura seja
 * determinística nos testes.
 *
 * Devolve **todos** os erros de uma vez, e não o primeiro encontrado.
 */
class WorkSessionValidator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    fun validate(draft: WorkSessionDraft): List<WorkSessionFieldError> = buildList {
        addAll(validateDate(draft.date))

        if (draft.platform == null) {
            add(error(WorkSessionField.PLATFORM, WorkSessionValidationError.REQUIRED))
        }

        // Faturamento, corridas, tempo e distância são individualmente
        // opcionais: um dia pode ter tido corridas sem que o motorista tenha
        // anotado a quilometragem, e forçar o preenchimento faria ele inventar
        // um número — pior que não ter o dado.
        draft.revenue?.let {
            if (it.isNegative) add(error(WorkSessionField.REVENUE, WorkSessionValidationError.NEGATIVE))
        }
        draft.rides?.let {
            if (it < 0) add(error(WorkSessionField.RIDES, WorkSessionValidationError.NEGATIVE))
        }
        draft.distanceKm?.let {
            if (it < 0) add(error(WorkSessionField.DISTANCE, WorkSessionValidationError.NEGATIVE))
        }
        draft.onlineTime?.let {
            if (it.minutes > MAX_ONLINE_MINUTES_PER_DAY) {
                add(error(WorkSessionField.ONLINE_TIME, WorkSessionValidationError.ONLINE_TIME_TOO_LONG))
            }
        }

        if (draft.note.length > WorkSession.MAX_NOTE_LENGTH) {
            add(error(WorkSessionField.NOTE, WorkSessionValidationError.NOTE_TOO_LONG))
        }

        // O que não pode é a sessão inteira estar vazia: um registro sem
        // nenhum número não informa nada e ainda polui o histórico.
        if (isEmpty(draft)) {
            add(error(WorkSessionField.REVENUE, WorkSessionValidationError.EMPTY_SESSION))
        }
    }

    /**
     * Converte um rascunho válido em [WorkSession].
     *
     * Campos numéricos não preenchidos viram zero: para o dashboard, "não
     * anotei quantos km rodei" e "rodei zero km" somam igual. O que não pode
     * é o dia inteiro estar em branco, e isso [validate] já barrou.
     */
    fun toSession(
        draft: WorkSessionDraft,
        createdAt: Instant = clock.instant(),
    ): WorkSession = WorkSession(
        id = draft.id,
        date = draft.date!!,
        platform = draft.platform!!,
        rides = draft.rides ?: 0,
        revenue = draft.revenue ?: Money.ZERO,
        onlineTime = draft.onlineTime ?: WorkDuration.ZERO,
        distanceKm = draft.distanceKm ?: 0L,
        note = draft.note.trim(),
        createdAt = createdAt,
    )

    private fun validateDate(date: LocalDate?): List<WorkSessionFieldError> = when {
        date == null ->
            listOf(error(WorkSessionField.DATE, WorkSessionValidationError.REQUIRED))
        date.isAfter(LocalDate.now(clock)) ->
            listOf(error(WorkSessionField.DATE, WorkSessionValidationError.DATE_IN_FUTURE))
        else -> emptyList()
    }

    private fun isEmpty(draft: WorkSessionDraft): Boolean =
        (draft.revenue?.isZero ?: true) &&
            (draft.rides ?: 0) == 0 &&
            (draft.onlineTime?.isZero ?: true) &&
            (draft.distanceKm ?: 0L) == 0L

    private fun error(field: WorkSessionField, error: WorkSessionValidationError) =
        WorkSessionFieldError(field, error)

    companion object {
        /** Um dia tem 24 horas; acima disso é erro de digitação. */
        const val MAX_ONLINE_MINUTES_PER_DAY: Long = 24 * 60
    }
}
