package com.driverprofit.data.repository

import com.driverprofit.data.local.dao.VehicleDao
import com.driverprofit.data.local.entity.toDomain
import com.driverprofit.data.local.entity.toEntity
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.repository.VehicleRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementação offline-first apoiada em Room (PRD §1).
 *
 * O [ioDispatcher] é injetado em vez de fixado para que os testes possam
 * substituí-lo por um dispatcher determinístico.
 */
class OfflineVehicleRepository(
    private val vehicleDao: VehicleDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : VehicleRepository {

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicleDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeVehicle(id: Long): Flow<Vehicle?> =
        vehicleDao.observeById(id)
            .map { entity -> entity?.toDomain() }
            .flowOn(ioDispatcher)

    override suspend fun getVehicle(id: Long): Vehicle? =
        withContext(ioDispatcher) { vehicleDao.findById(id)?.toDomain() }

    override suspend fun countVehicles(): Int =
        withContext(ioDispatcher) { vehicleDao.count() }

    override suspend fun addVehicle(vehicle: Vehicle): Long =
        withContext(ioDispatcher) { vehicleDao.insert(vehicle.toEntity()) }

    override suspend fun updateVehicle(vehicle: Vehicle) =
        withContext(ioDispatcher) { vehicleDao.update(vehicle.toEntity()) }

    override suspend fun deleteVehicle(id: Long) =
        withContext(ioDispatcher) { vehicleDao.deleteById(id) }
}
