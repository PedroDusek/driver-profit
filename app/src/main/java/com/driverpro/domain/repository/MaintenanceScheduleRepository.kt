package com.driverpro.domain.repository

import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.MaintenanceSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso aos intervalos de manutenção (ROADMAP v0.9.0).
 *
 * Guarda apenas o que o motorista alterou: item sem registro usa
 * [MaintenanceItem.defaultIntervalKm]. É por isso que existe
 * [resetToDefault] e não um "salvar o padrão" — devolver um item ao padrão é
 * apagar a preferência, não gravar outra.
 */
interface MaintenanceScheduleRepository {

    fun observeAll(): Flow<List<MaintenanceSchedule>>

    fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceSchedule>>

    suspend fun save(schedule: MaintenanceSchedule)

    suspend fun resetToDefault(vehicleId: Long, item: MaintenanceItem)
}
