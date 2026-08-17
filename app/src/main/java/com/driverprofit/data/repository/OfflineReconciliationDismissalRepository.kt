package com.driverprofit.data.repository

import com.driverprofit.data.local.dao.ReconciliationDismissalDao
import com.driverprofit.data.local.entity.toDomain
import com.driverprofit.data.local.entity.toEntity
import com.driverprofit.domain.model.ReconciliationDismissal
import com.driverprofit.domain.repository.ReconciliationDismissalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Implementação offline-first apoiada em Room. */
class OfflineReconciliationDismissalRepository(
    private val dao: ReconciliationDismissalDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ReconciliationDismissalRepository {

    override fun observeAll(): Flow<List<ReconciliationDismissal>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(dismissal: ReconciliationDismissal) {
        withContext(ioDispatcher) { dao.upsert(dismissal.toEntity()) }
    }
}
