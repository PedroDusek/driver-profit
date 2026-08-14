package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.DashboardMetrics
import com.driverprofit.domain.model.DateRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Indicadores de rentabilidade de um período (PRD §20, §21).
 *
 * Junta ganhos e despesas do mesmo intervalo e devolve o cálculo pronto. É um
 * `combine`, e não duas coleções separadas na ViewModel, porque lucro é uma
 * conta entre as duas listas: emiti-las em separado deixaria a tela mostrar,
 * por um instante, o faturamento novo com a despesa antiga.
 */
class ObserveDashboardUseCase(
    private val observeWorkSessionsBetween: ObserveWorkSessionsBetweenUseCase,
    private val observeExpensesBetween: ObserveExpensesBetweenUseCase,
    private val observePersonalUsageInPeriod: ObservePersonalUsageInPeriodUseCase,
) {

    operator fun invoke(range: DateRange): Flow<DashboardMetrics> = combine(
        observeWorkSessionsBetween(range.start, range.end),
        observeExpensesBetween(range.start, range.end),
        observePersonalUsageInPeriod(range),
    ) { sessions, expenses, personalUsage ->
        DashboardMetrics.of(
            sessions = sessions,
            expenses = expenses,
            // O recorte proporcional acontece aqui, e não no SQL: uma viagem
            // que atravessa a virada do mês entra nos dois períodos, cada um
            // com a fatia de dias que lhe cabe (PRD §22).
            personalKilometers = personalUsage.sumOf { it.kilometersWithin(range) },
        )
    }
}
