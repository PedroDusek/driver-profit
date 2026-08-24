package com.driverpro.domain.usecase

import com.driverpro.core.domain.DateRange
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageDraft
import com.driverpro.domain.model.PersonalUsageField
import com.driverpro.domain.model.PersonalUsageFieldError
import com.driverpro.domain.model.PersonalUsageSource
import com.driverpro.domain.model.PersonalUsageValidationError
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Regras de validação do lançamento de uso pessoal (PRD §22).
 *
 * Devolve todos os erros de uma vez, e devolve o motivo — nunca a frase
 * exibida.
 */
class PersonalUsageValidator(
    private val clock: Clock = Clock.systemDefaultZone(),
) {

    fun validate(draft: PersonalUsageDraft): List<PersonalUsageFieldError> = buildList {
        if (draft.vehicleId == null) {
            add(error(PersonalUsageField.VEHICLE, PersonalUsageValidationError.REQUIRED))
        }

        val hoje = LocalDate.now(clock)
        val start = draft.start
        val end = draft.end

        if (start == null) {
            add(error(PersonalUsageField.START, PersonalUsageValidationError.REQUIRED))
        } else if (start.isAfter(hoje)) {
            add(error(PersonalUsageField.START, PersonalUsageValidationError.DATE_IN_FUTURE))
        }

        // Fim em branco significa viagem de um dia só — o caso comum, e não
        // erro. Só quando ele existe é que precisa ser coerente com o início.
        if (end != null) {
            when {
                end.isAfter(hoje) ->
                    add(error(PersonalUsageField.END, PersonalUsageValidationError.DATE_IN_FUTURE))
                start != null && end.isBefore(start) ->
                    add(
                        error(
                            PersonalUsageField.END,
                            PersonalUsageValidationError.END_BEFORE_START,
                        ),
                    )
            }
        }

        when {
            draft.distanceKm == null ->
                add(error(PersonalUsageField.DISTANCE, PersonalUsageValidationError.REQUIRED))
            draft.distanceKm <= 0L || draft.distanceKm > PersonalUsage.MAX_DISTANCE_KM ->
                add(
                    error(
                        PersonalUsageField.DISTANCE,
                        PersonalUsageValidationError.DISTANCE_OUT_OF_RANGE,
                    ),
                )
        }

        if (draft.note.length > PersonalUsage.MAX_NOTE_LENGTH) {
            add(error(PersonalUsageField.NOTE, PersonalUsageValidationError.TEXT_TOO_LONG))
        }
    }

    /** Só chame depois de [validate] retornar lista vazia. */
    fun toPersonalUsage(
        draft: PersonalUsageDraft,
        source: PersonalUsageSource = PersonalUsageSource.DECLARED,
        createdAt: Instant = clock.instant(),
    ): PersonalUsage = PersonalUsage(
        id = draft.id,
        vehicleId = draft.vehicleId,
        // Fim ausente vira intervalo de um dia.
        range = DateRange(draft.start!!, draft.end ?: draft.start),
        distanceKm = draft.distanceKm!!,
        source = source,
        note = draft.note.trim(),
        createdAt = createdAt,
    )

    private fun error(field: PersonalUsageField, error: PersonalUsageValidationError) =
        PersonalUsageFieldError(field, error)
}
