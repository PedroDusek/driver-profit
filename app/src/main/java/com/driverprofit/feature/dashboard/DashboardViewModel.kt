package com.driverprofit.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverprofit.domain.model.DashboardMetrics
import com.driverprofit.domain.model.DashboardPeriod
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.usecase.ObserveDashboardUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate

/** Estado da tela de dashboard. */
sealed interface DashboardUiState {

    /** Período selecionado — conhecido mesmo antes de os números chegarem. */
    val period: DashboardPeriod

    data class Loading(override val period: DashboardPeriod) : DashboardUiState

    data class Content(
        override val period: DashboardPeriod,
        /** Intervalo efetivamente consultado, já resolvido a partir de hoje. */
        val range: DateRange,
        val metrics: DashboardMetrics,
    ) : DashboardUiState
}

/**
 * Dashboard de rentabilidade (PRD §20).
 *
 * Não calcula nada: escolhe o período, pede os números ao use case e expõe o
 * resultado. Todo indicador vem pronto de `DashboardMetrics`, que é domínio
 * puro (PRD §29) — regra financeira dentro de ViewModel é proibida (PRD §54).
 *
 * O [clock] é injetado porque "hoje", "ontem" e "mês anterior" dependem da data
 * corrente: com `LocalDate.now()` fixo no código, o teste de "ontem" passaria
 * hoje e falharia na virada do mês.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    observeDashboard: ObserveDashboardUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val period = MutableStateFlow<DashboardPeriod>(DEFAULT_PERIOD)

    /**
     * Dia de referência dos períodos.
     *
     * Lido a cada consulta, e não guardado uma vez: um app aberto durante a
     * virada da meia-noite não pode continuar chamando o dia anterior de
     * "hoje".
     */
    fun today(): LocalDate = LocalDate.now(clock)

    val uiState: StateFlow<DashboardUiState> = period
        .flatMapLatest { selected ->
            val range = selected.rangeAt(today())
            observeDashboard(range).map { metrics ->
                DashboardUiState.Content(selected, range, metrics)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState.Loading(DEFAULT_PERIOD),
        )

    fun onPeriodChange(value: DashboardPeriod) {
        period.value = value
    }

    /** Aplica um intervalo escolhido no seletor de datas (PRD §20). */
    fun onCustomRangeChange(start: LocalDate, end: LocalDate) {
        // Ordena as pontas em vez de recusar: escolher a data final primeiro é
        // engano de toque, não pedido inválido.
        val range = if (end.isBefore(start)) DateRange(end, start) else DateRange(start, end)
        period.value = DashboardPeriod.Custom(range)
    }

    private companion object {
        /** O motorista abre o app para ver o dia dele. */
        val DEFAULT_PERIOD: DashboardPeriod = DashboardPeriod.Today

        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
