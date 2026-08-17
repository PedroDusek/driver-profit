package com.driverpro.testing

import com.driverpro.domain.model.WorkSession
import com.driverpro.domain.repository.WorkSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repositório em memória para testes de use case e ViewModel.
 *
 * Reproduz o que importa do `WorkSessionDao`: ids auto-incrementais,
 * ordenação por data decrescente com desempate por id, e filtro de período
 * inclusivo nas duas pontas.
 */
class FakeWorkSessionRepository(
    initialSessions: List<WorkSession> = emptyList(),
) : WorkSessionRepository {

    private val sessions = MutableStateFlow(initialSessions)
    private var nextId = (initialSessions.maxOfOrNull { it.id } ?: 0L) + 1

    /** Instantâneo do estado atual, para asserções diretas nos testes. */
    val current: List<WorkSession> get() = sessions.value

    private fun List<WorkSession>.sorted(): List<WorkSession> =
        sortedWith(compareByDescending<WorkSession> { it.date }.thenByDescending { it.id })

    override fun observeSessions(): Flow<List<WorkSession>> =
        sessions.map { it.sorted() }

    override fun observeSessionsBetween(
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<WorkSession>> = sessions.map { list ->
        list.filter { it.date >= start && it.date <= end }.sorted()
    }

    override suspend fun getSession(id: Long): WorkSession? =
        sessions.value.firstOrNull { it.id == id }

    override suspend fun addSession(session: WorkSession): Long {
        val id = nextId++
        sessions.value = sessions.value + session.copy(id = id)
        return id
    }

    override suspend fun updateSession(session: WorkSession) {
        sessions.value = sessions.value.map { if (it.id == session.id) session else it }
    }

    override suspend fun deleteSession(id: Long) {
        sessions.value = sessions.value.filterNot { it.id == id }
    }
}
