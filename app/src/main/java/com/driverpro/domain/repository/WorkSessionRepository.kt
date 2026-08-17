package com.driverpro.domain.repository

import com.driverpro.domain.model.WorkSession
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de acesso às sessões de trabalho.
 *
 * A interface vive no domínio e a implementação em `data.repository`: o
 * domínio não conhece Room.
 */
interface WorkSessionRepository {

    /** Emite todas as sessões, da mais recente para a mais antiga. */
    fun observeSessions(): Flow<List<WorkSession>>

    /**
     * Sessões dentro de um período, inclusive nas duas pontas.
     *
     * Ainda não é usado por nenhuma tela — o dashboard com filtros é da
     * v0.5.0. Existe agora porque a consulta por período é o acesso que o
     * produto inteiro depende, e é melhor tê-la coberta por teste desde o
     * início do que descobrir o SQL errado junto com a tela.
     */
    fun observeSessionsBetween(start: LocalDate, end: LocalDate): Flow<List<WorkSession>>

    suspend fun getSession(id: Long): WorkSession?

    suspend fun addSession(session: WorkSession): Long

    suspend fun updateSession(session: WorkSession)

    suspend fun deleteSession(id: Long)
}
