package com.driverpro.maintenance.data

import com.driverpro.maintenance.data.MaintenanceScheduleDao
import com.driverpro.maintenance.domain.MaintenanceItem
import com.driverpro.maintenance.domain.MaintenanceSchedule
import com.driverpro.maintenance.domain.MaintenanceScheduleRepository
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
