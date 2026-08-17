package com.driverpro.domain.usecase

import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.model.WorkSessionDraft
import com.driverpro.domain.model.WorkSessionFieldError
import com.driverpro.domain.repository.WorkSessionRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Resultado de uma tentativa de salvar sessão de trabalho. */
sealed interface SaveWorkSessionResult {

    /** Persistida com sucesso. [id] é o identificador final da sessão. */
    data class Success(val id: Long) : SaveWorkSessionResult

    /** Rejeitada pela validação. Nada foi gravado. */
    data class Invalid(val errors: List<WorkSessionFieldError>) : SaveWorkSessionResult
}

/**
 * Registra ou atualiza uma sessão de trabalho (PRD §15).
 *
 * Insert e update no mesmo use case: a regra é a mesma, muda só o destino, e
 * o id do rascunho já diz qual é.
 */
class SaveWorkSessionUseCase(
    private val repository: WorkSessionRepository,
    private val validator: WorkSessionValidator,
) {

    suspend operator fun invoke(draft: WorkSessionDraft): SaveWorkSessionResult {
        val errors = validator.validate(draft)
        if (errors.isNotEmpty()) return SaveWorkSessionResult.Invalid(errors)

        if (!draft.isEditing) {
            return SaveWorkSessionResult.Success(repository.addSession(validator.toSession(draft)))
        }

        // Preserva o createdAt original: corrigir o valor de um dia não muda
        // quando aquele lançamento entrou no app.
        val existing = repository.getSession(draft.id)
            ?: return SaveWorkSessionResult.Success(
                repository.addSession(validator.toSession(draft)),
            )

        repository.updateSession(validator.toSession(draft, createdAt = existing.createdAt))
        return SaveWorkSessionResult.Success(draft.id)
    }
}

/** Observa o histórico completo de sessões (PRD §19). */
class ObserveWorkSessionsUseCase(
    private val repository: WorkSessionRepository,
) {
    operator fun invoke(): Flow<List<WorkSession>> = repository.observeSessions()
}

/** Observa as sessões de um período — base dos filtros do dashboard (PRD §20). */
class ObserveWorkSessionsBetweenUseCase(
    private val repository: WorkSessionRepository,
) {
    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<WorkSession>> =
        repository.observeSessionsBetween(start, end)
}

/** Busca uma sessão específica para edição. */
class GetWorkSessionUseCase(
    private val repository: WorkSessionRepository,
) {
    suspend operator fun invoke(id: Long): WorkSession? = repository.getSession(id)
}

/** Exclui uma sessão. */
class DeleteWorkSessionUseCase(
    private val repository: WorkSessionRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteSession(id)
}
