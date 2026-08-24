package com.driverpro.vehicle.domain

import com.driverpro.vehicle.domain.VehicleRepository

/**
 * Troca o veículo atual (v0.12.0).
 *
 * Só existe quando há dois ou mais veículos cadastrados — com um só, ele já
 * nasce atual (`SaveVehicleUseCase`) e não há entre quais escolher.
 */
class SetCurrentVehicleUseCase(
    private val repository: VehicleRepository,
) {
    suspend operator fun invoke(id: Long) = repository.setCurrent(id)
}
