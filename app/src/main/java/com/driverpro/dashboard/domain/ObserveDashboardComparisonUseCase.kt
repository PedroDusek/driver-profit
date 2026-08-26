package com.driverpro.dashboard.domain

import com.driverpro.core.domain.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Os indicadores do período escolhido ao lado dos do anterior equivalente.
 *
 * Reaproveita [ObserveDashboardUseCase] duas vezes em vez de duplicar o
 * `combine` de jornadas, despesas, uso pessoal e competência: ele já é
 * parametrizado por um [DateRange] qualquer, então "o período anterior" é só
 * outro intervalo. Nenhuma consulta nova precisou existir no repositório.
 *
 * `combine` de novo, e não dois `Flow` separados na ViewModel, pelo mesmo
 * motivo do use case de dentro: a comparação é uma conta **entre** os dois
 * lados, e emiti-los em separado deixaria a tela mostrar, por um instante, o
 * número novo contra a variação antiga.
 */
class ObserveDashboardComparisonUseCase(
    private val observeDashboard: ObserveDashboardUseCase,
) {

    operator fun invoke(current: DateRange, previous: DateRange): Flow<DashboardComparison> =
        combine(
            observeDashboard(current),
            observeDashboard(previous),
        ) { currentMetrics, previousMetrics ->
            DashboardComparison(current = currentMetrics, previous = previousMetrics)
        }
}
