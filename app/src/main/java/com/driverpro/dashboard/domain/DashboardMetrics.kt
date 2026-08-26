package com.driverpro.dashboard.domain

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory

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
    /**
     * Custo fixo que **compete** ao período, já rateado pelos dias (PRD §22).
     *
     * Diferente de [fixedExpenses], que é caixa: aquele é o que saiu do bolso
     * dentro do período, este é o que o período consumiu. O IPVA pago em
     * janeiro aparece nos doze meses aqui, e só em janeiro lá.
     */
    val accruedFixedCost: Money = Money.ZERO,
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

    /**
     * Custo fixo por quilômetro **trabalhado**.
     *
     * O divisor é [workKilometers], e não a distância total: financiamento,
     * seguro e IPVA existem pela decisão de ter o carro para trabalhar, e não
     * são causados pelo uso (PRD §22). Levar o carro ao mercado no domingo não
     * gera parcela nem aumenta o prêmio, então ratear esses custos pelo
     * quilômetro pessoal os atribuiria a algo que não os causou.
     *
     * É por isso que ele é um indicador separado, e não uma parcela do
     * [costPerKm]: os dois têm denominadores diferentes e somá-los seria somar
     * razões de bases distintas.
     */
    val fixedCostPerKm: Money? get() = accruedFixedCost.per(workKilometers)

    /**
     * Quanto custa **rodar** uma hora — só o custo variável.
     *
     * O numerador é [workOperationalCost], e não [operationalExpenses] como no
     * [costPerKm]. Lá os totais dos dois lados podem ser usados porque o rateio
     * se cancela algebricamente (`(E × Kt/K) ÷ Kt = E ÷ K`); aqui não existe
     * "hora pessoal", então o cancelamento não vale, e usar a despesa
     * operacional inteira misturaria numerador de uso total com denominador de
     * uso profissional.
     *
     * **Custo fixo fica de fora**, pelo mesmo motivo que fica fora do
     * [costPerKm]: seguro, IPVA e financiamento não variam com o quanto se
     * roda, nem com quantas horas se trabalha. Incluí-los produziria um número
     * sem sentido em período curto — no dia em que o IPVA de R$ 1.200 é pago
     * tendo-se trabalhado 3 horas, o custo da hora apareceria como R$ 416,67.
     * Eles seguem visíveis em [fixedCostPerKm], que é um indicador à parte.
     *
     * Consequência assumida: [revenuePerHour] − [costPerHour] **não** dá
     * [profitPerHour], e a diferença é justamente o custo fixo. É a mesma
     * assimetria que já existe entre [costPerKm] e [fixedCostPerKm].
     */
    val costPerHour: Money? get() = workOperationalCost.per(totalOnlineTime.toHours())

    val profitPerKm: Money? get() = netProfit.per(workKilometers)

    val profitPerHour: Money? get() = netProfit.per(totalOnlineTime.toHours())

    /** `true` quando há quilômetro pessoal a mostrar na repartição. */
    val hasPersonalUsage: Boolean get() = personalKilometers > 0L

    /**
     * Percentual da distância total que foi trabalho.
     *
     * Responde "quanto do carro a operação divide com a vida pessoal", que é
     * para o que a repartição serve. Exibido como percentual, e **não** como
     * centavos por km: "R$ 0,86/km profissional" convidaria a multiplicar por
     * quilômetro trabalhado e chegar a um valor que não existe — os 86 centavos
     * são por quilômetro **total**. O custo por km é o mesmo nos dois usos; o
     * que muda é quantos quilômetros cada um consumiu.
     */
    val workKilometerShare: Int?
        get() = if (totalKilometers <= 0L) {
            null
        } else {
            Math.round(workKilometers * PERCENT / totalKilometers.toDouble()).toInt()
        }

    /** Complemento de [workKilometerShare], para os dois sempre somarem 100. */
    val personalKilometerShare: Int?
        get() = workKilometerShare?.let { PERCENT.toInt() - it }

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

        private const val PERCENT = 100.0

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
            accruedFixedCost: Money = Money.ZERO,
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
            accruedFixedCost = accruedFixedCost,
        )
    }
}
