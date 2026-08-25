package com.driverpro.vehicle.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.vehicle.domain.DeleteVehicleUseCase
import com.driverpro.expenses.domain.ObserveVehicleOdometersUseCase
import com.driverpro.vehicle.domain.ObserveVehiclesUseCase
import com.driverpro.vehicle.domain.SetCurrentVehicleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da tela de lista de veículos. */
sealed interface VehicleListUiState {

    /** Primeira carga do banco, antes da primeira emissão. */
    data object Loading : VehicleListUiState

    /** Nenhum veículo cadastrado — a tela mostra o estado vazio. */
    data object Empty : VehicleListUiState

    /**
     * @param odometers última leitura conhecida de cada veículo, por id. Um
     *   veículo ausente do mapa ainda não tem leitura — o que inclui todo
     *   lançamento anterior à v0.6.0.
     */
    data class Content(
        val vehicles: List<Vehicle>,
        val odometers: Map<Long, Long> = emptyMap(),
    ) : VehicleListUiState
}

/**
 * Lista de veículos cadastrados.
 *
 * Não contém regra de negócio: apenas traduz o `Flow` do use case em estado de
 * tela e encaminha o pedido de exclusão (PRD §25).
 */
class VehicleListViewModel(
    observeVehicles: ObserveVehiclesUseCase,
    observeVehicleOdometers: ObserveVehicleOdometersUseCase,
    private val deleteVehicle: DeleteVehicleUseCase,
    private val setCurrentVehicle: SetCurrentVehicleUseCase,
) : ViewModel() {

    /** Veículo aguardando confirmação de exclusão, ou `null`. */
    private val pendingDeletion = MutableStateFlow<Vehicle?>(null)

    val vehiclePendingDeletion: StateFlow<Vehicle?> = pendingDeletion.asStateFlow()

    val uiState: StateFlow<VehicleListUiState> =
        combine(observeVehicles(), observeVehicleOdometers()) { vehicles, odometers ->
            if (vehicles.isEmpty()) VehicleListUiState.Empty
            else VehicleListUiState.Content(vehicles, odometers)
        }
        .stateIn(
            scope = viewModelScope,
            // Mantém a assinatura viva por 5s durante mudanças de configuração,
            // evitando reconsultar o banco a cada rotação de tela.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = VehicleListUiState.Loading,
        )

    /**
     * Marca [vehicle] como o veículo atual (v0.12.0).
     *
     * Só existe efeito com dois ou mais veículos — a tela nem mostra o botão
     * quando há um só, porque ele já nasce atual sozinho.
     */
    fun onSetCurrent(vehicle: Vehicle) {
        if (vehicle.isCurrent) return
        viewModelScope.launch {
            setCurrentVehicle(vehicle.id)
        }
    }

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
