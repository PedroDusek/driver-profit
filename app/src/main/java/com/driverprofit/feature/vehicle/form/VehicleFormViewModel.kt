package com.driverprofit.feature.vehicle.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehicleFuel
import com.driverprofit.domain.model.VehicleValidationError
import com.driverprofit.domain.model.toDraft
import com.driverprofit.domain.usecase.GetVehicleUseCase
import com.driverprofit.domain.usecase.SaveVehicleResult
import com.driverprofit.domain.usecase.SaveVehicleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado do formulário de veículo. */
data class VehicleFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val name: String = "",
    val fuel: VehicleFuel? = null,
    val errors: Map<VehicleField, VehicleValidationError> = emptyMap(),
    val savedVehicleId: Long? = null,
) {
    fun errorFor(field: VehicleField): VehicleValidationError? = errors[field]
}

/**
 * Cadastro e edição de veículo.
 *
 * A ViewModel não valida nada: ela monta o [VehicleDraft] e entrega ao
 * [SaveVehicleUseCase], que é o dono da regra (PRD §25). O que volta são
 * erros por campo, que viram estado de tela.
 */
class VehicleFormViewModel(
    savedStateHandle: SavedStateHandle,
    private val getVehicle: GetVehicleUseCase,
    private val saveVehicle: SaveVehicleUseCase,
) : ViewModel() {

    private val vehicleId: Long =
        savedStateHandle.get<Long>(DriverProfitDestination.ARG_VEHICLE_ID)
            ?: Vehicle.UNSAVED_ID

    private val _uiState = MutableStateFlow(
        VehicleFormUiState(
            isEditing = vehicleId != Vehicle.UNSAVED_ID,
            isLoading = vehicleId != Vehicle.UNSAVED_ID,
        ),
    )
    val uiState: StateFlow<VehicleFormUiState> = _uiState.asStateFlow()

    init {
        if (vehicleId != Vehicle.UNSAVED_ID) {
            loadVehicle(vehicleId)
        }
    }

    private fun loadVehicle(id: Long) {
        viewModelScope.launch {
            val vehicle = getVehicle(id)
            _uiState.update { state ->
                if (vehicle == null) {
                    // Veículo excluído em outra tela enquanto esta abria.
                    state.copy(isLoading = false, isEditing = false)
                } else {
                    val draft = vehicle.toDraft()
                    state.copy(
                        name = draft.name,
                        fuel = draft.fuel,
                        isLoading = false,
                        isEditing = true,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = updateField(VehicleField.NAME) { copy(name = value) }

    fun onFuelChange(value: VehicleFuel) = updateField(VehicleField.FUEL) { copy(fuel = value) }

    fun onSave() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            when (val result = saveVehicle(_uiState.value.toDraft(vehicleId))) {
                is SaveVehicleResult.Success ->
                    _uiState.update { it.copy(isSaving = false, savedVehicleId = result.id) }

                is SaveVehicleResult.Invalid ->
                    _uiState.update { it.copy(isSaving = false, errors = result.errors.toMap()) }
            }
        }
    }

    /** Consumido pela UI após navegar de volta, para não navegar duas vezes. */
    fun onNavigatedBack() {
        _uiState.update { it.copy(savedVehicleId = null) }
    }

    /**
     * Aplica a alteração e apaga o erro daquele campo.
     *
     * Manter a mensagem de erro enquanto o usuário corrige o campo é ruído: o
     * erro reaparece no próximo save se ainda existir.
     */
    private fun updateField(
        field: VehicleField,
        transform: VehicleFormUiState.() -> VehicleFormUiState,
    ) {
        _uiState.update { it.transform().copy(errors = it.errors - field) }
    }
}

private fun List<VehicleFieldError>.toMap(): Map<VehicleField, VehicleValidationError> =
    associate { it.field to it.error }

/**
 * Converte o estado de tela no rascunho que o domínio entende.
 *
 * [vehicleId] precisa ser o id do veículo em edição — é ele que faz o
 * [SaveVehicleUseCase] atualizar em vez de inserir.
 */
internal fun VehicleFormUiState.toDraft(vehicleId: Long = Vehicle.UNSAVED_ID): VehicleDraft =
    VehicleDraft(
        id = vehicleId,
        name = name,
        fuel = fuel,
    )
