package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.VehicleRepository
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
 * Ganhos e despesas vinculados ao veículo excluído não são apagados — o banco
 * os orfaniza (`ON DELETE SET NULL`), preservando o histórico financeiro.
 *
 * Se o veículo excluído era o atual, promove o mais antigo dos que sobraram
 * (v0.12.0): o formulário de ganhos e despesas depende de sempre haver um
 * veículo atual quando existe pelo menos um cadastrado.
 */
class DeleteVehicleUseCase(
    private val repository: VehicleRepository,
) {
    suspend operator fun invoke(id: Long) {
        repository.deleteVehicle(id)
        repository.promoteOldestToCurrentIfNone()
    }
}
