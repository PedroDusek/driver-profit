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
 *
 * @param workKilometers quilômetros das jornadas de trabalho.
 * @param personalKilometers quilômetros rodados fora do trabalho, já
 *   proporcionalizados ao período (PRD §22).
 */
data class DashboardMetrics(
    val totalRevenue: Money,
    val totalExpenses: Money,
    val expensesByCategory: Map<ExpenseCategory, Money>,
    val totalRides: Int,
    val workKilometers: Long,
    val personalKilometers: Long = 0L,
    val totalOnlineTime: WorkDuration,
) {

    /**
     * Distância total percorrida pelo carro no período.
     *
     * É **este** o denominador do custo por quilômetro, e não apenas o
     * quilômetro de trabalho. O rateio proporcional das despesas se cancela
     * algebricamente — `(E × Kp/K) ÷ Kp = E ÷ K` — então basta o denominador
     * ser o total para o indicador ficar correto, sem precisar estimar consumo
     * nem preço médio de combustível (PRD §22).
     */
    val totalKilometers: Long get() = workKilometers + personalKilometers

    /**
     * Despesas que variam com o quanto se roda (PRD §22).
     *
     * É esta soma, e não [totalExpenses], que entra no [costPerKm].
     */
    val operationalExpenses: Money
        get() = Money.sum(expensesByCategory.filterKeys { it.isOperationalCost }.values)

    /**
     * Seguro, IPVA e financiamento.
     *
     * Ficam fora do custo por quilômetro **e** fora do rateio com o uso
     * pessoal: custo fixo não é causado pelo uso. Levar o carro ao mercado não
     * gera parcela de financiamento, não aumenta o prêmio do seguro e não muda
     * o IPVA — esses valores existiriam idênticos se o carro ficasse parado.
     * São 100% do trabalho (PRD §22).
     */
    val fixedExpenses: Money get() = totalExpenses - operationalExpenses

    /**
     * Parte do custo operacional que cabe ao trabalho.
     *
     * Sem quilômetro nenhum registrado não há base para repartir, e o custo
     * inteiro fica com o trabalho — que é o comportamento conservador e o que
     * o app já fazia antes da v0.7.0.
     */
    val workOperationalCost: Money
        get() = if (totalKilometers <= 0L) {
            operationalExpenses
        } else {
            Money(
                Math.round(
                    operationalExpenses.cents.toDouble() * workKilometers / totalKilometers,
                ),
            )
        }

    /**
     * Parte do custo operacional que cabe ao uso pessoal.
     *
     * Calculada por diferença, e não por proporção própria, para que as duas
     * partes somem **exatamente** [operationalExpenses]. Dois arredondamentos
     * independentes deixariam um centavo órfão, e a tela deixaria de fechar
     * contra o extrato.
     */
    val personalOperationalCost: Money get() = operationalExpenses - workOperationalCost

    /** Despesas cobradas do trabalho: os custos fixos mais a parte rateada. */
    val workExpenses: Money get() = fixedExpenses + workOperationalCost

    /**
     * Lucro do trabalho (PRD §21).
     *
     * Desconta apenas [workExpenses]: o combustível queimado no fim de semana
     * não é custo da operação. Sem uso pessoal registrado, isto é idêntico a
     * faturamento menos despesas totais.
     */
    val netProfit: Money get() = totalRevenue - workExpenses

    val revenuePerKm: Money? get() = totalRevenue.per(workKilometers)

    val revenuePerHour: Money? get() = totalRevenue.per(totalOnlineTime.toHours())

    val revenuePerRide: Money? get() = totalRevenue.per(totalRides.toLong())

    /**
     * Quanto custa mover o carro um quilômetro — o indicador central do
     * produto.
     *
     * Vale igual para quilômetro de trabalho e de lazer: combustível não sabe
     * quem estava no banco do passageiro. Por isso o denominador é
     * [totalKilometers] e o numerador é só [operationalExpenses] — seguro e
     * IPVA não variam com a distância (PRD §22).
     */
    val costPerKm: Money? get() = operationalExpenses.per(totalKilometers)

    val profitPerKm: Money? get() = netProfit.per(workKilometers)

    val profitPerHour: Money? get() = netProfit.per(totalOnlineTime.toHours())

    /** `true` quando há quilômetro pessoal a mostrar na repartição. */
    val hasPersonalUsage: Boolean get() = personalKilometers > 0L

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
            workKilometers == 0L &&
            personalKilometers == 0L &&
            totalOnlineTime.isZero

    companion object {

        /** Período sem nenhum lançamento. */
        val EMPTY: DashboardMetrics = DashboardMetrics(
            totalRevenue = Money.ZERO,
            totalExpenses = Money.ZERO,
            expensesByCategory = emptyMap(),
            totalRides = 0,
            workKilometers = 0L,
            personalKilometers = 0L,
            totalOnlineTime = WorkDuration.ZERO,
        )

        /**
         * Agrega os lançamentos **já filtrados pelo período**.
         *
         * A filtragem é do repositório, que consulta por data em SQL indexado;
         * repeti-la aqui duplicaria a regra em dois lugares que podem divergir.
         *
         * @param personalKilometers já proporcionalizado ao período pelo
         *   chamador, via `PersonalUsage.kilometersWithin`.
         */
        fun of(
            sessions: List<WorkSession>,
            expenses: List<Expense>,
            personalKilometers: Long = 0L,
        ): DashboardMetrics = DashboardMetrics(
            totalRevenue = Money.sum(sessions.map { it.revenue }),
            totalExpenses = Money.sum(expenses.map { it.amount }),
            expensesByCategory = expenses
                .groupBy { it.category }
                .mapValues { (_, list) -> Money.sum(list.map { it.amount }) },
            totalRides = sessions.sumOf { it.rides },
            workKilometers = sessions.sumOf { it.distanceKm },
            personalKilometers = personalKilometers,
            totalOnlineTime = WorkDuration.sum(sessions.map { it.onlineTime }),
        )
    }
}
