package com.driverpro.feature.personal.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.domain.model.DateRange
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.usecase.DeletePersonalUsageUseCase
import com.driverpro.domain.usecase.DismissReconciliationUseCase
import com.driverpro.domain.usecase.ObserveOdometerReconciliationUseCase
import com.driverpro.domain.usecase.ObservePersonalUsageUseCase
import com.driverpro.domain.usecase.OdometerReconciliation
import com.driverpro.domain.usecase.SaveReconciledPersonalUsageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    observeReconciliation: ObserveOdometerReconciliationUseCase,
    private val deletePersonalUsage: DeletePersonalUsageUseCase,
    private val saveReconciled: SaveReconciledPersonalUsageUseCase,
    private val dismissReconciliation: DismissReconciliationUseCase,
) : ViewModel() {

    private val pendingDeletion = MutableStateFlow<PersonalUsage?>(null)
    val usagePendingDeletion: StateFlow<PersonalUsage?> = pendingDeletion.asStateFlow()

    /**
     * Divergências dispensadas nesta sessão de tela.
     *
     * Guardadas por janela, e não como um booleano: resolver a de hoje não pode
     * silenciar a que aparecer no próximo abastecimento.
     */
    private val dismissed = MutableStateFlow<Set<DateRange>>(emptySet())

    /**
     * A conciliação aparece **sozinha** quando há divergência.
     *
     * Até a v0.9.0 ela dependia de o motorista apertar um botão em uma tela que
     * ele talvez nunca abrisse — e quem nunca apertava ficava com uso pessoal
     * zerado e custo/km inflado, que é o defeito que a v0.7.0 existia para
     * corrigir.
     */
    val pendingReconciliation: StateFlow<OdometerReconciliation?> =
        combine(observeReconciliation(), dismissed) { pending, ignored ->
            pending.map { it.reconciliation }
                .firstOrNull { it.period !in ignored }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null,
            )

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

    /** Traz de volta uma divergência que ele tinha dispensado. */
    fun onReconcileRequested() {
        dismissed.value = emptySet()
    }

    fun onReconcileDismissed() {
        val pending = pendingReconciliation.value ?: return
        dismissed.value = dismissed.value + pending.period
    }

    /**
     * O motorista confirmou que a sobra foi uso pessoal.
     *
     * Gravar o lançamento já zera a divergência sozinho: a sobra da janela
     * passa a ser descontada como uso pessoal declarado, e o `Flow` recalcula.
     */
    /**
     * O motorista aceitou deixar a sobra fora da conta.
     *
     * Gravar a dispensa já a tira da lista sozinho: o `Flow` recalcula, a sobra
     * passa a caber no que foi aceito, e a janela deixa de ser pendente.
     */
    fun onReconcileLeftOut() {
        val pending = pendingReconciliation.value ?: return
        viewModelScope.launch { dismissReconciliation(pending) }
    }

    fun onReconcileConfirmedAsPersonal() {
        val pending = pendingReconciliation.value ?: return
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
