package com.driverpro.testing

import com.driverpro.domain.model.Vehicle
import com.driverpro.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Repositório em memória para testes de use case e ViewModel.
 *
 * Existe para que essas camadas sejam testadas sem Room e sem emulador. O
 * comportamento que importa é reproduzido fielmente: ids auto-incrementais e
 * ordenação do mais recente para o mais antigo, como faz o `VehicleDao`.
 */
class FakeVehicleRepository(
    initialVehicles: List<Vehicle> = emptyList(),
) : VehicleRepository {

    private val vehicles = MutableStateFlow(initialVehicles)
    private var nextId = (initialVehicles.maxOfOrNull { it.id } ?: 0L) + 1

    /** Instantâneo do estado atual, para asserções diretas nos testes. */
    val current: List<Vehicle> get() = vehicles.value

    override fun observeVehicles(): Flow<List<Vehicle>> =
        vehicles.map { list -> list.sortedByDescending { it.createdAt } }

    override fun observeVehicle(id: Long): Flow<Vehicle?> =
        vehicles.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getVehicle(id: Long): Vehicle? =
        vehicles.value.firstOrNull { it.id == id }

    override suspend fun countVehicles(): Int = vehicles.value.size

    override suspend fun addVehicle(vehicle: Vehicle): Long {
        val id = nextId++
        vehicles.value = vehicles.value + vehicle.copy(id = id)
        return id
    }

    override suspend fun updateVehicle(vehicle: Vehicle) {
        vehicles.value = vehicles.value.map { if (it.id == vehicle.id) vehicle else it }
    }

    override suspend fun deleteVehicle(id: Long) {
        vehicles.value = vehicles.value.filterNot { it.id == id }
    }

    override suspend fun setCurrent(id: Long) {
        vehicles.value = vehicles.value.map { it.copy(isCurrent = it.id == id) }
    }

    override suspend fun promoteOldestToCurrentIfNone() {
        if (vehicles.value.any { it.isCurrent }) return
        val oldest = vehicles.value.minByOrNull { it.createdAt } ?: return
        vehicles.value = vehicles.value.map { it.copy(isCurrent = it.id == oldest.id) }
    }
}
