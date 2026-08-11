package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow

/** Observa a lista de veículos cadastrados, reagindo a cada alteração. */
class ObserveVehiclesUseCase(
    private val repository: VehicleRepository,
) {
    operator fun invoke(): Flow<List<Vehicle>> = repository.observeVehicles()
}

/** Busca um veículo específico para edição. */
class GetVehicleUseCase(
    private val repository: VehicleRepository,
) {
    suspend operator fun invoke(id: Long): Vehicle? = repository.getVehicle(id)
}

/**
 * Exclui um veículo (PRD §5).
 *
 * A partir da v0.4.0, quando existirem despesas vinculadas, este use case
 * passa a ser o lugar de decidir o que fazer com os lançamentos do veículo
 * excluído. Hoje não há vínculo, então a exclusão é direta.
 */
class DeleteVehicleUseCase(
    private val repository: VehicleRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteVehicle(id)
}
