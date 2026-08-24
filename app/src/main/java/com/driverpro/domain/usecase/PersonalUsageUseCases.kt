package com.driverpro.domain.usecase

import com.driverpro.core.domain.DateRange
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageDraft
import com.driverpro.domain.model.PersonalUsageFieldError
import com.driverpro.domain.model.PersonalUsageSource
import com.driverpro.domain.repository.PersonalUsageRepository
import kotlinx.coroutines.flow.Flow

/** Resultado de uma tentativa de salvar uso pessoal. */
sealed interface SavePersonalUsageResult {

    data class Success(val id: Long) : SavePersonalUsageResult

    /** Rejeitado pela validação. Nada foi gravado. */
    data class Invalid(val errors: List<PersonalUsageFieldError>) : SavePersonalUsageResult
}

/** Registra ou atualiza uma viagem pessoal (PRD §22). */
class SavePersonalUsageUseCase(
    private val repository: PersonalUsageRepository,
    private val validator: PersonalUsageValidator,
) {

    suspend operator fun invoke(
        draft: PersonalUsageDraft,
        source: PersonalUsageSource = PersonalUsageSource.DECLARED,
    ): SavePersonalUsageResult {
        val errors = validator.validate(draft)
        if (errors.isNotEmpty()) return SavePersonalUsageResult.Invalid(errors)

        if (!draft.isEditing) {
            return SavePersonalUsageResult.Success(
                repository.addUsage(validator.toPersonalUsage(draft, source)),
            )
        }

        // Preserva o createdAt original: corrigir a quilometragem de uma
        // viagem não muda quando ela entrou no app.
        val existing = repository.getUsage(draft.id)
            ?: return SavePersonalUsageResult.Success(
                repository.addUsage(validator.toPersonalUsage(draft, source)),
            )

        repository.updateUsage(
            validator.toPersonalUsage(draft, existing.source, existing.createdAt),
        )
        return SavePersonalUsageResult.Success(draft.id)
    }
}

/** Histórico completo de uso pessoal. */
class ObservePersonalUsageUseCase(
    private val repository: PersonalUsageRepository,
) {
    operator fun invoke(): Flow<List<PersonalUsage>> = repository.observeAll()
}

/** Uso pessoal que encosta num período — base do custo/km do dashboard. */
class ObservePersonalUsageInPeriodUseCase(
    private val repository: PersonalUsageRepository,
) {
    operator fun invoke(period: DateRange): Flow<List<PersonalUsage>> =
        repository.observeOverlapping(period)
}

class GetPersonalUsageUseCase(
    private val repository: PersonalUsageRepository,
) {
    suspend operator fun invoke(id: Long): PersonalUsage? = repository.getUsage(id)
}

class DeletePersonalUsageUseCase(
    private val repository: PersonalUsageRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteUsage(id)
}
