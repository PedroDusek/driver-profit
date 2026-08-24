package com.driverpro.earnings.data

import com.driverpro.earnings.data.WorkSessionDao
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.earnings.domain.WorkSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Implementação offline-first apoiada em Room.
 *
 * A conversão `LocalDate` → epoch day acontece aqui, na fronteira: o domínio
 * fala em datas, o banco fala em inteiros.
 */
class OfflineWorkSessionRepository(
    private val workSessionDao: WorkSessionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WorkSessionRepository {

    override fun observeSessions(): Flow<List<WorkSession>> =
        workSessionDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeSessionsBetween(
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<WorkSession>> =
        workSessionDao.observeBetween(start.toEpochDay(), end.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getSession(id: Long): WorkSession? =
        withContext(ioDispatcher) { workSessionDao.findById(id)?.toDomain() }

    override suspend fun addSession(session: WorkSession): Long =
        withContext(ioDispatcher) { workSessionDao.insert(session.toEntity()) }

    override suspend fun updateSession(session: WorkSession) =
        withContext(ioDispatcher) { workSessionDao.update(session.toEntity()) }

    override suspend fun deleteSession(id: Long) =
        withContext(ioDispatcher) { workSessionDao.deleteById(id) }
}
