package com.driverprofit.feature.vehicle.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.core.navigation.DriverProfitDestination
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleDraft
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFieldError
import com.driverprofit.domain.model.VehiclePowertrain
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

/**
 * Estado do formulário de veículo.
 *
 * [yearInput] e [odometerInput] são `String` e não números: o campo precisa
 * representar "vazio" e "meio digitado", estados que `Int` não tem. A
 * conversão acontece em [VehicleFormViewModel.currentDraft].
 */
data class VehicleFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val brand: String = "",
    val model: String = "",
    val yearInput: String = "",
    val odometerInput: String = "",
    val powertrain: VehiclePowertrain? = null,
    val combustionFuel: CombustionFuel? = null,
    val chargingCapability: ChargingCapability? = null,
    val errors: Map<VehicleField, VehicleValidationError> = emptyMap(),
    val savedVehicleId: Long? = null,
) {
    /** Exibe o seletor de combustível apenas para quem tem motor a combustão. */
    val showCombustionFuel: Boolean get() = powertrain?.usesCombustionFuel == true

    /** Exibe o seletor de recarga apenas para quem tem motor elétrico. */
    val showChargingCapability: Boolean get() = powertrain?.mayBeCharged == true

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
                    state.fromDraft(vehicle.toDraft()).copy(isLoading = false, isEditing = true)
                }
            }
        }
    }

    fun onBrandChange(value: String) = updateField(VehicleField.BRAND) { copy(brand = value) }

    fun onModelChange(value: String) = updateField(VehicleField.MODEL) { copy(model = value) }

    fun onYearChange(value: String) = updateField(VehicleField.YEAR) {
        copy(yearInput = value.filter(Char::isDigit).take(YEAR_MAX_DIGITS))
    }

    fun onOdometerChange(value: String) = updateField(VehicleField.INITIAL_ODOMETER) {
        copy(odometerInput = value.filter(Char::isDigit).take(ODOMETER_MAX_DIGITS))
    }

    /**
     * Troca a propulsão e limpa o que deixou de fazer sentido.
     *
     * Delegado a [VehicleDraft.withPowertrain] para que a regra viva no
     * domínio e valha para qualquer caminho, não só para esta tela.
     */
    fun onPowertrainChange(value: VehiclePowertrain?) {
        _uiState.update { state ->
            val cleaned = state.toDraft().withPowertrain(value)
            state.fromDraft(cleaned).copy(
                errors = state.errors - setOf(
                    VehicleField.POWERTRAIN,
                    VehicleField.COMBUSTION_FUEL,
                    VehicleField.CHARGING_CAPABILITY,
                ),
            )
        }
    }

    fun onCombustionFuelChange(value: CombustionFuel?) =
        updateField(VehicleField.COMBUSTION_FUEL) { copy(combustionFuel = value) }

    fun onChargingCapabilityChange(value: ChargingCapability?) =
        updateField(VehicleField.CHARGING_CAPABILITY) { copy(chargingCapability = value) }

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

    private companion object {
        const val YEAR_MAX_DIGITS = 4
        const val ODOMETER_MAX_DIGITS = 7
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
        brand = brand,
        model = model,
        year = yearInput.toIntOrNull(),
        initialOdometerKm = odometerInput.toLongOrNull(),
        powertrain = powertrain,
        combustionFuel = combustionFuel,
        chargingCapability = chargingCapability,
    )

internal fun VehicleFormUiState.fromDraft(draft: VehicleDraft): VehicleFormUiState = copy(
    brand = draft.brand,
    model = draft.model,
    yearInput = draft.year?.toString().orEmpty(),
    odometerInput = draft.initialOdometerKm?.toString().orEmpty(),
    powertrain = draft.powertrain,
    combustionFuel = draft.combustionFuel,
    chargingCapability = draft.chargingCapability,
)
