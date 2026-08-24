package com.driverpro.domain.usecase

import com.driverpro.core.domain.Money
import com.driverpro.domain.model.DashboardMetrics
import com.driverpro.core.domain.DateRange
import com.driverpro.earnings.domain.ObserveWorkSessionsBetweenUseCase
import com.driverpro.personal.domain.ObservePersonalUsageInPeriodUseCase
import com.driverpro.expenses.domain.ObserveAccruedExpensesUseCase
import com.driverpro.expenses.domain.ObserveExpensesBetweenUseCase
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
    private val observeAccruedInPeriod: ObserveAccruedExpensesUseCase,
) {

    operator fun invoke(range: DateRange): Flow<DashboardMetrics> = combine(
        observeWorkSessionsBetween(range.start, range.end),
        observeExpensesBetween(range.start, range.end),
        observePersonalUsageInPeriod(range),
        observeAccruedInPeriod(range),
    ) { sessions, expenses, personalUsage, accrued ->
        DashboardMetrics.of(
            sessions = sessions,
            expenses = expenses,
            // O recorte proporcional acontece aqui, e não no SQL: uma viagem
            // que atravessa a virada do mês entra nos dois períodos, cada um
            // com a fatia de dias que lhe cabe (PRD §22).
            personalKilometers = personalUsage.sumOf { it.kilometersWithin(range) },
            // Mesma ideia do recorte proporcional acima, aplicada a dinheiro:
            // o seguro anual entrega a cada mês a fatia de dias que lhe cabe.
            // Só custo fixo que aceita competência entra (v0.11.0: não é mais
            // todo custo fixo — IPVA saiu). O filtro é por `allowsAccrual`, e
            // não por `accrual` estar vazio, para não depender de nenhuma
            // linha antiga de IPVA ainda ter competência gravada de antes
            // desta versão.
            accruedFixedCost = Money.sum(
                accrued.filter { it.category.allowsAccrual }
                    .map { it.amountWithin(range) },
            ),
        )
    }
}
