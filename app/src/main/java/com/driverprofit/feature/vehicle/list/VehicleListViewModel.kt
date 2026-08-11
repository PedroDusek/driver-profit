package com.driverprofit.feature.vehicle.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.usecase.DeleteVehicleUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da tela de lista de veículos. */
sealed interface VehicleListUiState {

    /** Primeira carga do banco, antes da primeira emissão. */
    data object Loading : VehicleListUiState

    /** Nenhum veículo cadastrado — a tela mostra o estado vazio. */
    data object Empty : VehicleListUiState

    data class Content(val vehicles: List<Vehicle>) : VehicleListUiState
}

/**
 * Lista de veículos cadastrados.
 *
 * Não contém regra de negócio: apenas traduz o `Flow` do use case em estado de
 * tela e encaminha o pedido de exclusão (PRD §25).
 */
class VehicleListViewModel(
    observeVehicles: ObserveVehiclesUseCase,
    private val deleteVehicle: DeleteVehicleUseCase,
) : ViewModel() {

    /** Veículo aguardando confirmação de exclusão, ou `null`. */
    private val pendingDeletion = MutableStateFlow<Vehicle?>(null)

    val vehiclePendingDeletion: StateFlow<Vehicle?> = pendingDeletion.asStateFlow()

    val uiState: StateFlow<VehicleListUiState> = observeVehicles()
        .map { vehicles ->
            if (vehicles.isEmpty()) VehicleListUiState.Empty
            else VehicleListUiState.Content(vehicles)
        }
        .stateIn(
            scope = viewModelScope,
            // Mantém a assinatura viva por 5s durante mudanças de configuração,
            // evitando reconsultar o banco a cada rotação de tela.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = VehicleListUiState.Loading,
        )

    fun onDeleteRequested(vehicle: Vehicle) {
        pendingDeletion.value = vehicle
    }

    fun onDeleteDismissed() {
        pendingDeletion.value = null
    }

    /**
     * Confirma a exclusão. A lista se atualiza sozinha: o `Flow` do Room
     * reemite assim que a linha some.
     */
    fun onDeleteConfirmed() {
        val vehicle = pendingDeletion.value ?: return
        pendingDeletion.value = null
        viewModelScope.launch {
            deleteVehicle(vehicle.id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
