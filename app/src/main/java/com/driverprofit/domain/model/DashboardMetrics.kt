package com.driverprofit.domain.model

import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration

/**
 * Indicadores de rentabilidade de um período (PRD §21, §29).
 *
 * Kotlin puro, sem Android e sem Room: é o cálculo que **é** o produto, e ele
 * precisa cair na primeira linha de teste, com JUnit, sem emulador. Por isso
 * também não vive em ViewModel nem em Composable (PRD §54).
 *
 * A classe guarda só somas; tudo que é razão entre duas somas é propriedade
 * derivada. Assim não existe a possibilidade de um total e um indicador
 * discordarem — o segundo é sempre calculado do primeiro.
 *
 * Toda divisão passa por `Money.per`, que devolve `null` quando o divisor é
 * zero (PRD §21). Um período sem quilômetros não tem R$/km igual a zero: ele
 * não tem R$/km, e a tela exibe "—".
 */
data class DashboardMetrics(
    val totalRevenue: Money,
    val totalExpenses: Money,
    val expensesByCategory: Map<ExpenseCategory, Money>,
    val totalRides: Int,
    val totalKilometers: Long,
    val totalOnlineTime: WorkDuration,
) {

    /** Faturamento menos **todas** as despesas do período (PRD §21). */
    val netProfit: Money get() = totalRevenue - totalExpenses

    /**
     * Despesas que variam com o quanto se roda (PRD §22).
     *
     * É esta soma, e não [totalExpenses], que entra no [costPerKm].
     */
    val operationalExpenses: Money
        get() = Money.sum(expensesByCategory.filterKeys { it.isOperationalCost }.values)

    /** Seguro, IPVA e financiamento: custo do mês, não custo do quilômetro. */
    val fixedExpenses: Money get() = totalExpenses - operationalExpenses

    val revenuePerKm: Money? get() = totalRevenue.per(totalKilometers)

    val revenuePerHour: Money? get() = totalRevenue.per(totalOnlineTime.toHours())

    val revenuePerRide: Money? get() = totalRevenue.per(totalRides.toLong())

    /**
     * Quanto custa rodar um quilômetro — o indicador central do produto.
     *
     * Usa apenas [operationalExpenses]. Seguro e IPVA não variam com a
     * distância: lançar o seguro anual em um dia de trabalho jogaria o custo/km
     * daquele dia para as alturas e faria o motorista concluir que rodar não
     * compensa, o que seria falso (PRD §22).
     */
    val costPerKm: Money? get() = operationalExpenses.per(totalKilometers)

    val profitPerKm: Money? get() = netProfit.per(totalKilometers)

    val profitPerHour: Money? get() = netProfit.per(totalOnlineTime.toHours())

    /**
     * `true` quando o período não tem nenhum lançamento.
     *
     * Distingue "você não trabalhou nesse período" de "você trabalhou e o
     * resultado foi zero" — mensagens diferentes, e só a primeira é motivo
     * para esconder os indicadores.
     */
    val isEmpty: Boolean
        get() = totalRevenue.isZero &&
            totalExpenses.isZero &&
            expensesByCategory.isEmpty() &&
            totalRides == 0 &&
            totalKilometers == 0L &&
            totalOnlineTime.isZero

    companion object {

        /** Período sem nenhum lançamento. */
        val EMPTY: DashboardMetrics = DashboardMetrics(
            totalRevenue = Money.ZERO,
            totalExpenses = Money.ZERO,
            expensesByCategory = emptyMap(),
            totalRides = 0,
            totalKilometers = 0L,
            totalOnlineTime = WorkDuration.ZERO,
        )

        /**
         * Agrega as sessões e despesas **já filtradas pelo período**.
         *
         * A filtragem é do repositório, que consulta por data em SQL indexado;
         * repeti-la aqui duplicaria a regra em dois lugares que podem divergir.
         */
        fun of(sessions: List<WorkSession>, expenses: List<Expense>): DashboardMetrics =
            DashboardMetrics(
                totalRevenue = Money.sum(sessions.map { it.revenue }),
                totalExpenses = Money.sum(expenses.map { it.amount }),
                expensesByCategory = expenses
                    .groupBy { it.category }
                    .mapValues { (_, list) -> Money.sum(list.map { it.amount }) },
                totalRides = sessions.sumOf { it.rides },
                totalKilometers = sessions.sumOf { it.distanceKm },
                totalOnlineTime = WorkDuration.sum(sessions.map { it.onlineTime }),
            )
    }
}
