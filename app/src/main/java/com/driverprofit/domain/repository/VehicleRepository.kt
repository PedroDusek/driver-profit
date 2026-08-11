package com.driverprofit.domain.repository

import com.driverprofit.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso a veículos.
 *
 * A interface vive no domínio e a implementação em `data.repository`: o
 * domínio não conhece Room, e trocar a fonte de dados no futuro (backup,
 * sincronização) não obriga a mexer em regra de negócio.
 */
interface VehicleRepository {

    /** Emite a lista completa a cada alteração no banco. */
    fun observeVehicles(): Flow<List<Vehicle>>

    fun observeVehicle(id: Long): Flow<Vehicle?>

    suspend fun getVehicle(id: Long): Vehicle?

    /** Quantidade de veículos cadastrados — usado para decidir a tela inicial. */
    suspend fun countVehicles(): Int

    /** Persiste um novo veículo e devolve o id gerado. */
    suspend fun addVehicle(vehicle: Vehicle): Long

    suspend fun updateVehicle(vehicle: Vehicle)

    suspend fun deleteVehicle(id: Long)
}
