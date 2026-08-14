package com.driverprofit.data.repository

import com.driverprofit.data.local.dao.MaintenanceScheduleDao
import com.driverprofit.data.local.entity.toDomain
import com.driverprofit.data.local.entity.toEntity
import com.driverprofit.domain.model.MaintenanceItem
import com.driverprofit.domain.model.MaintenanceSchedule
import com.driverprofit.domain.repository.MaintenanceScheduleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Implementação offline-first apoiada em Room. */
class OfflineMaintenanceScheduleRepository(
    private val dao: MaintenanceScheduleDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MaintenanceScheduleRepository {

    override fun observeAll(): Flow<List<MaintenanceSchedule>> =
        dao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>> =
        dao.observeForVehicle(vehicleId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun save(schedule: MaintenanceSchedule) {
        withContext(ioDispatcher) { dao.upsert(schedule.toEntity()) }
    }

    override suspend fun resetToDefault(vehicleId: Long, item: MaintenanceItem) {
        withContext(ioDispatcher) { dao.deleteFor(vehicleId, item) }
    }
}
