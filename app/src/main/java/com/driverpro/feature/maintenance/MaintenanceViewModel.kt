package com.driverpro.feature.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.domain.model.MaintenanceAlert
import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.VehicleMaintenance
import com.driverpro.domain.usecase.MaintenanceValidationError
import com.driverpro.domain.usecase.ObserveMaintenanceUseCase
import com.driverpro.domain.usecase.ResetMaintenanceIntervalUseCase
import com.driverpro.domain.usecase.SaveMaintenanceIntervalResult
import com.driverpro.domain.usecase.SaveMaintenanceIntervalUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Estado da tela de manutenção preventiva. */
sealed interface MaintenanceUiState {

    data object Loading : MaintenanceUiState

    /** Sem veículo cadastrado não há o que acompanhar. */
    data object Empty : MaintenanceUiState

    data class Content(val vehicles: List<VehicleMaintenance>) : MaintenanceUiState
}

/** Edição do intervalo de um item, aberta em diálogo. */
data class MaintenanceIntervalEdit(
    val vehicleId: Long,
    val item: MaintenanceItem,
    val input: String,
    val monitored: Boolean,
    val error: MaintenanceValidationError? = null,
)

/**
 * Manutenção preventiva (ROADMAP v0.9.0).
 *
 * Não decide nada sobre vencimento: os estados chegam prontos de
 * `MaintenanceMonitor`, que é domínio puro (PRD §29). Aqui só se escolhe o que
 * mostrar e o que gravar.
 */
class MaintenanceViewModel(
    observeMaintenance: ObserveMaintenanceUseCase,
    private val saveInterval: SaveMaintenanceIntervalUseCase,
    private val resetInterval: ResetMaintenanceIntervalUseCase,
) : ViewModel() {

    private val editing = MutableStateFlow<MaintenanceIntervalEdit?>(null)
    val intervalEdit: StateFlow<MaintenanceIntervalEdit?> = editing.asStateFlow()

    val uiState: StateFlow<MaintenanceUiState> = observeMaintenance()
        .map { vehicles ->
            if (vehicles.isEmpty()) {
                MaintenanceUiState.Empty
            } else {
                MaintenanceUiState.Content(vehicles)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MaintenanceUiState.Loading,
        )

    fun onEditRequested(vehicleId: Long, alert: MaintenanceAlert) {
        editing.value = MaintenanceIntervalEdit(
            vehicleId = vehicleId,
            item = alert.item,
            input = alert.intervalKm.toString(),
            // Preserva o estado: corrigir o intervalo de um item desligado não
            // pode religá-lo por efeito colateral.
            monitored = alert.monitored,
        )
    }

    fun onEditDismissed() {
        editing.value = null
    }

    fun onIntervalChange(value: String) {
        // Limpa o erro assim que ele digita: manter a mensagem enquanto o campo
        // muda faz parecer que a correção não foi registrada.
        editing.value = editing.value?.copy(input = value.filter { it.isDigit() }, error = null)
    }

    fun onEditConfirmed() {
        val edit = editing.value ?: return
        viewModelScope.launch {
            val result = saveInterval(
                vehicleId = edit.vehicleId,
                item = edit.item,
                intervalKm = edit.input.toLongOrNull(),
                monitored = edit.monitored,
            )
            editing.value = when (result) {
                is SaveMaintenanceIntervalResult.Success -> null
                is SaveMaintenanceIntervalResult.Invalid -> edit.copy(error = result.error)
            }
        }
    }

    /** Devolve o item ao padrão do app, apagando a preferência. */
    fun onResetRequested(vehicleId: Long, item: MaintenanceItem) {
        editing.value = null
        viewModelScope.launch { resetInterval(vehicleId, item) }
    }

    /**
     * Liga ou desliga o acompanhamento de um item.
     *
     * Desligar grava o intervalo corrente junto: o motorista que voltar atrás
     * encontra o número que tinha escolhido, e não o padrão do app.
     */
    fun onMonitoredChange(vehicleId: Long, alert: MaintenanceAlert, monitored: Boolean) {
        viewModelScope.launch {
            saveInterval(
                vehicleId = vehicleId,
                item = alert.item,
                intervalKm = alert.intervalKm,
                monitored = monitored,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
