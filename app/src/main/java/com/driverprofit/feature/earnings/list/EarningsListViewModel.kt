package com.driverprofit.feature.earnings.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.WorkSession
import com.driverprofit.domain.usecase.DeleteWorkSessionUseCase
import com.driverprofit.domain.usecase.ObserveWorkSessionsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Totais do histórico exibido.
 *
 * Não é o dashboard (PRD §20/§29) — é apenas a soma do que está na tela, para
 * o motorista não precisar somar de cabeça enquanto confere os lançamentos.
 * Os indicadores por período, com filtros, chegam na v0.5.0.
 */
data class EarningsSummary(
    val totalRevenue: Money,
    val totalRides: Int,
    val totalOnlineTime: WorkDuration,
    val totalDistanceKm: Long,
) {
    /** `null` quando não houve tempo online — não é zero, é indisponível. */
    val revenuePerHour: Money? get() = totalRevenue.per(totalOnlineTime.toHours())

    val revenuePerKm: Money? get() = totalRevenue.per(totalDistanceKm)

    companion object {
        fun of(sessions: List<WorkSession>): EarningsSummary = EarningsSummary(
            totalRevenue = Money.sum(sessions.map { it.revenue }),
            totalRides = sessions.sumOf { it.rides },
            totalOnlineTime = WorkDuration.sum(sessions.map { it.onlineTime }),
            totalDistanceKm = sessions.sumOf { it.distanceKm },
        )
    }
}

/** Estado da tela de histórico de ganhos. */
sealed interface EarningsListUiState {

    data object Loading : EarningsListUiState

    data object Empty : EarningsListUiState

    data class Content(
        val sessions: List<WorkSession>,
        val summary: EarningsSummary,
    ) : EarningsListUiState
}

/**
 * Histórico de sessões de trabalho (PRD §19).
 *
 * Não contém regra de negócio: traduz o `Flow` do use case em estado de tela
 * e encaminha o pedido de exclusão. Os totais são somas de tipos que já sabem
 * somar — `Money` e `WorkDuration` — e não cálculo financeiro solto aqui.
 */
class EarningsListViewModel(
    observeWorkSessions: ObserveWorkSessionsUseCase,
    private val deleteWorkSession: DeleteWorkSessionUseCase,
) : ViewModel() {

    private val pendingDeletion = MutableStateFlow<WorkSession?>(null)

    val sessionPendingDeletion: StateFlow<WorkSession?> = pendingDeletion.asStateFlow()

    val uiState: StateFlow<EarningsListUiState> = observeWorkSessions()
        .map { sessions ->
            if (sessions.isEmpty()) {
                EarningsListUiState.Empty
            } else {
                EarningsListUiState.Content(sessions, EarningsSummary.of(sessions))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = EarningsListUiState.Loading,
        )

    fun onDeleteRequested(session: WorkSession) {
        pendingDeletion.value = session
    }

    fun onDeleteDismissed() {
        pendingDeletion.value = null
    }

    fun onDeleteConfirmed() {
        val session = pendingDeletion.value ?: return
        pendingDeletion.value = null
        viewModelScope.launch {
            deleteWorkSession(session.id)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
