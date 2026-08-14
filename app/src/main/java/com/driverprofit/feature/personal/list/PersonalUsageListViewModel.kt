package com.driverprofit.feature.personal.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.domain.model.DashboardPeriod
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.usecase.DeletePersonalUsageUseCase
import com.driverprofit.domain.usecase.ObservePersonalUsageUseCase
import com.driverprofit.domain.usecase.ObserveVehiclesUseCase
import com.driverprofit.domain.usecase.OdometerReconciliation
import com.driverprofit.domain.usecase.ReconcileOdometerUseCase
import com.driverprofit.domain.usecase.SaveReconciledPersonalUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Estado da tela de uso pessoal. */
sealed interface PersonalUsageListUiState {

    data object Loading : PersonalUsageListUiState

    data object Empty : PersonalUsageListUiState

    data class Content(val usages: List<PersonalUsage>) : PersonalUsageListUiState
}

/**
 * Histórico de uso pessoal, e a porta de entrada da conciliação por odômetro
 * (PRD §22).
 */
class PersonalUsageListViewModel(
    observePersonalUsage: ObservePersonalUsageUseCase,
    private val observeVehicles: ObserveVehiclesUseCase,
    private val deletePersonalUsage: DeletePersonalUsageUseCase,
    private val reconcileOdometer: ReconcileOdometerUseCase,
    private val saveReconciled: SaveReconciledPersonalUsageUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val pendingDeletion = MutableStateFlow<PersonalUsage?>(null)
    val usagePendingDeletion: StateFlow<PersonalUsage?> = pendingDeletion.asStateFlow()

    private val reconciliation = MutableStateFlow<OdometerReconciliation?>(null)
    val pendingReconciliation: StateFlow<OdometerReconciliation?> = reconciliation.asStateFlow()

    val uiState: StateFlow<PersonalUsageListUiState> = observePersonalUsage()
        .map { usages ->
            if (usages.isEmpty()) PersonalUsageListUiState.Empty
            else PersonalUsageListUiState.Content(usages)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = PersonalUsageListUiState.Loading,
        )

    /**
     * Confere o painel contra o lançado, no mês corrente.
     *
     * O mês é o recorte natural: é longo o bastante para acumular sobra que
     * valha a pena resolver, e curto o bastante para o motorista ainda lembrar
     * o que fez.
     */
    fun onReconcileRequested() {
        viewModelScope.launch {
            val vehicle = observeVehicles().first().firstOrNull() ?: return@launch
            val period = DashboardPeriod.ThisMonth.rangeAt(LocalDate.now(clock))
            reconciliation.value = reconcileOdometer(vehicle.id, period)
        }
    }

    fun onReconcileDismissed() {
        reconciliation.value = null
    }

    /** O motorista confirmou que a sobra foi uso pessoal. */
    fun onReconcileConfirmedAsPersonal() {
        val pending = reconciliation.value ?: return
        reconciliation.value = null
        viewModelScope.launch {
            saveReconciled(pending)
        }
    }

    fun onDeleteRequested(usage: PersonalUsage) {
        pendingDeletion.value = usage
    }

    fun onDeleteDismissed() {
        pendingDeletion.value = null
    }

    fun onDeleteConfirmed() {
        val usage = pendingDeletion.value ?: return
        pendingDeletion.value = null
        viewModelScope.launch {
            deletePersonalUsage(usage.id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
