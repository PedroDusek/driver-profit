package com.driverprofit.data.repository

import com.driverprofit.data.local.dao.PersonalUsageDao
import com.driverprofit.data.local.entity.toDomain
import com.driverprofit.data.local.entity.toEntity
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.repository.PersonalUsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Implementação offline-first apoiada em Room. */
class OfflinePersonalUsageRepository(
    private val dao: PersonalUsageDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PersonalUsageRepository {

    override fun observeAll(): Flow<List<PersonalUsage>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeOverlapping(period: DateRange): Flow<List<PersonalUsage>> =
        dao.observeOverlapping(period.start.toEpochDay(), period.end.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun findOverlappingForVehicle(
        vehicleId: Long,
        period: DateRange,
    ): List<PersonalUsage> = withContext(ioDispatcher) {
        dao.findOverlappingForVehicle(
            vehicleId,
            period.start.toEpochDay(),
            period.end.toEpochDay(),
        ).map { it.toDomain() }
    }

    override suspend fun getUsage(id: Long): PersonalUsage? =
        withContext(ioDispatcher) { dao.findById(id)?.toDomain() }

    override suspend fun addUsage(usage: PersonalUsage): Long =
        withContext(ioDispatcher) { dao.insert(usage.toEntity()) }

    override suspend fun updateUsage(usage: PersonalUsage) =
        withContext(ioDispatcher) { dao.update(usage.toEntity()) }

    override suspend fun deleteUsage(id: Long) =
        withContext(ioDispatcher) { dao.deleteById(id) }
}
