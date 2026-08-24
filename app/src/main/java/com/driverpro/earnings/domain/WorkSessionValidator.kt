package com.driverpro.earnings.domain

import com.driverpro.earnings.domain.WorkSession
import com.driverpro.earnings.domain.WorkSessionDraft
import com.driverpro.earnings.domain.WorkSessionField
import com.driverpro.earnings.domain.WorkSessionFieldError
import com.driverpro.earnings.domain.WorkSessionValidationError
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

        // Faturamento, corridas, tempo e distância são todos obrigatórios.
        //
        // Não por burocracia: o dashboard agrega período dividindo
        // soma(faturamento) por soma(horas). Uma sessão com valor preenchido e
        // horas em branco entraria com o valor no numerador e zero no
        // denominador, produzindo um R$/hora inflado — exibido com a mesma
        // confiança de um número correto. Um dado ausente e visível é ruim; um
        // indicador errado e invisível é pior (PRD §59: correção primeiro).
        //
        // Zero é resposta válida: um dia de 6h online sem nenhuma corrida
        // existe. O que não se aceita é o campo em branco.
        when {
            draft.revenue == null ->
                add(error(WorkSessionField.REVENUE, WorkSessionValidationError.REQUIRED))
            draft.revenue.isNegative ->
                add(error(WorkSessionField.REVENUE, WorkSessionValidationError.NEGATIVE))
        }
        when {
            draft.rides == null ->
                add(error(WorkSessionField.RIDES, WorkSessionValidationError.REQUIRED))
            draft.rides < 0 ->
                add(error(WorkSessionField.RIDES, WorkSessionValidationError.NEGATIVE))
        }
        when {
            draft.distanceKm == null ->
                add(error(WorkSessionField.DISTANCE, WorkSessionValidationError.REQUIRED))
            draft.distanceKm < 0 ->
                add(error(WorkSessionField.DISTANCE, WorkSessionValidationError.NEGATIVE))
        }
        when {
            draft.onlineTime == null ->
                add(error(WorkSessionField.ONLINE_TIME, WorkSessionValidationError.REQUIRED))
            draft.onlineTime.minutes > MAX_ONLINE_MINUTES_PER_DAY ->
                add(
                    error(
                        WorkSessionField.ONLINE_TIME,
                        WorkSessionValidationError.ONLINE_TIME_TOO_LONG,
                    ),
                )
        }

        if (draft.note.length > WorkSession.MAX_NOTE_LENGTH) {
            add(error(WorkSessionField.NOTE, WorkSessionValidationError.NOTE_TOO_LONG))
        }

        // Tudo preenchido, mas tudo zero, é um dia que não aconteceu: não
        // informa nada e ainda polui o histórico.
        //
        // Só faz sentido reclamar disso quando os quatro campos foram
        // informados. Com campos em branco, o erro correto é "campo
        // obrigatório" — acusar "sessão vazia" em cima disso seria ruído.
        if (isFullyFilled(draft) && isAllZero(draft)) {
            add(error(WorkSessionField.REVENUE, WorkSessionValidationError.EMPTY_SESSION))
        }
    }

    /**
     * Converte um rascunho válido em [WorkSession].
     *
     * Só chame depois de [validate] retornar lista vazia — daí os `!!`, que
     * documentam a pré-condição em vez de escondê-la atrás de defaults
     * silenciosos que reintroduziriam o zero implícito.
     */
    fun toSession(
        draft: WorkSessionDraft,
        createdAt: Instant = clock.instant(),
    ): WorkSession = WorkSession(
        id = draft.id,
        vehicleId = draft.vehicleId,
        date = draft.date!!,
        platform = draft.platform!!,
        rides = draft.rides!!,
        revenue = draft.revenue!!,
        onlineTime = draft.onlineTime!!,
        distanceKm = draft.distanceKm!!,
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

    private fun isFullyFilled(draft: WorkSessionDraft): Boolean =
        draft.revenue != null &&
            draft.rides != null &&
            draft.onlineTime != null &&
            draft.distanceKm != null

    private fun isAllZero(draft: WorkSessionDraft): Boolean =
        draft.revenue?.isZero == true &&
            draft.rides == 0 &&
            draft.onlineTime?.isZero == true &&
            draft.distanceKm == 0L

    private fun error(field: WorkSessionField, error: WorkSessionValidationError) =
        WorkSessionFieldError(field, error)

    companion object {
        /** Um dia tem 24 horas; acima disso é erro de digitação. */
        const val MAX_ONLINE_MINUTES_PER_DAY: Long = 24 * 60
    }
}
