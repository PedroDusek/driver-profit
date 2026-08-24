package com.driverpro.feature.personal.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.core.navigation.DriverProDestination
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageDraft
import com.driverpro.domain.model.PersonalUsageField
import com.driverpro.domain.model.PersonalUsageFieldError
import com.driverpro.domain.model.PersonalUsageValidationError
import com.driverpro.vehicle.domain.Vehicle
import com.driverpro.domain.usecase.GetPersonalUsageUseCase
import com.driverpro.vehicle.domain.ObserveVehiclesUseCase
import com.driverpro.domain.usecase.SavePersonalUsageResult
import com.driverpro.domain.usecase.SavePersonalUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Estado do formulário de uso pessoal. */
data class PersonalUsageFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: Long? = null,
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val distanceInput: String = "",
    val note: String = "",
    val errors: Map<PersonalUsageField, PersonalUsageValidationError> = emptyMap(),
    val savedId: Long? = null,
) {
    val distanceKm: Long? get() = distanceInput.toLongOrNull()

    fun errorFor(field: PersonalUsageField): PersonalUsageValidationError? = errors[field]
}

/**
 * Registro e edição de viagem pessoal (PRD §22).
 *
 * Este é o mecanismo que põe a viagem **no período em que ela aconteceu**. A
 * conciliação por odômetro cobre o que ficou por registrar, mas ela só sabe
 * distribuir a sobra pelos dias — quem quer o fim de semana no lugar certo
 * lança aqui.
 */
class PersonalUsageFormViewModel(
    savedStateHandle: SavedStateHandle,
    observeVehicles: ObserveVehiclesUseCase,
    private val getPersonalUsage: GetPersonalUsageUseCase,
    private val savePersonalUsage: SavePersonalUsageUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val usageId: Long =
        savedStateHandle.get<Long>(DriverProDestination.ARG_PERSONAL_USAGE_ID)
            ?: PersonalUsage.UNSAVED_ID

    private val _uiState = MutableStateFlow(
        PersonalUsageFormUiState(isEditing = usageId != PersonalUsage.UNSAVED_ID),
    )
    val uiState: StateFlow<PersonalUsageFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val vehicles = observeVehicles().first()
            val existing = usageId.takeIf { it != PersonalUsage.UNSAVED_ID }
                ?.let { getPersonalUsage(it) }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    isEditing = existing != null,
                    vehicles = vehicles,
                    // Com um veículo só, escolher por ele é o certo.
                    vehicleId = existing?.vehicleId ?: vehicles.singleOrNull()?.id,
                    start = existing?.range?.start ?: LocalDate.now(clock),
                    end = existing?.range?.end?.takeIf { it != existing.range.start },
                    distanceInput = existing?.distanceKm?.toString().orEmpty(),
                    note = existing?.note.orEmpty(),
                )
            }
        }
    }

    fun onVehicleChange(value: Long) =
        updateField(PersonalUsageField.VEHICLE) { copy(vehicleId = value) }

    fun onStartChange(value: LocalDate) =
        updateField(PersonalUsageField.START) { copy(start = value) }

    /** `null` limpa o campo: fim em branco é viagem de um dia só. */
    fun onEndChange(value: LocalDate?) =
        updateField(PersonalUsageField.END) { copy(end = value) }

    fun onDistanceChange(value: String) = updateField(PersonalUsageField.DISTANCE) {
        copy(distanceInput = value.filter(Char::isDigit).take(MAX_DISTANCE_DIGITS))
    }

    fun onNoteChange(value: String) = updateField(PersonalUsageField.NOTE) { copy(note = value) }

    fun onSave() {
        if (_uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val state = _uiState.value
            val draft = PersonalUsageDraft(
                id = usageId,
                vehicleId = state.vehicleId,
                start = state.start,
                end = state.end,
                distanceKm = state.distanceKm,
                note = state.note,
            )
            when (val result = savePersonalUsage(draft)) {
                is SavePersonalUsageResult.Success ->
                    _uiState.update { it.copy(isSaving = false, savedId = result.id) }

                is SavePersonalUsageResult.Invalid ->
                    _uiState.update { it.copy(isSaving = false, errors = result.errors.toMap()) }
            }
        }
    }

    fun onNavigatedBack() {
        _uiState.update { it.copy(savedId = null) }
    }

    private fun updateField(
        field: PersonalUsageField,
        transform: PersonalUsageFormUiState.() -> PersonalUsageFormUiState,
    ) {
        _uiState.update { it.transform().copy(errors = it.errors - field) }
    }

    private companion object {
        const val MAX_DISTANCE_DIGITS = 6
    }
}

private fun List<PersonalUsageFieldError>.toMap():
    Map<PersonalUsageField, PersonalUsageValidationError> = associate { it.field to it.error }
