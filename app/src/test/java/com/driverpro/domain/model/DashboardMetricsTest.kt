package com.driverpro.domain.model

import com.driverpro.core.common.Money
import com.driverpro.core.common.WorkDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DashboardMetricsTest {

    private val day = LocalDate.of(2026, 8, 12)

    private fun session(
        revenue: Money,
        rides: Int,
        minutes: Long,
        km: Long,
    ) = WorkSession(
        date = day,
        platform = Platform.UBER,
        rides = rides,
        revenue = revenue,
        onlineTime = WorkDuration(minutes),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    private fun expense(category: ExpenseCategory, amount: Money) = Expense(
        date = day,
        category = category,
        amount = amount,
        createdAt = Instant.EPOCH,
    )

    /**
     * R$ 600,00 faturados em 30 corridas, 15 horas e 300 km, contra R$ 300,00
     * de despesas — das quais R$ 120,00 são seguro, que é custo fixo.
     */
    private val metrics = DashboardMetrics.of(
        sessions = listOf(
            session(Money.of(320, 50), rides = 18, minutes = 500, km = 180),
            session(Money.of(279, 50), rides = 12, minutes = 400, km = 120),
        ),
        expenses = listOf(
            expense(ExpenseCategory.FUEL, Money.of(150, 0)),
            expense(ExpenseCategory.MAINTENANCE, Money.of(30, 0)),
            expense(ExpenseCategory.INSURANCE, Money.of(120, 0)),
        ),
    )

    @Test
    fun `soma faturamento despesas e lucro`() {
        assertEquals(Money.of(600, 0), metrics.totalRevenue)
        assertEquals(Money.of(300, 0), metrics.totalExpenses)
        assertEquals(Money.of(300, 0), metrics.netProfit)
    }

    @Test
    fun `soma corridas quilometros e tempo`() {
        assertEquals(30, metrics.totalRides)
        assertEquals(300L, metrics.totalKilometers)
        assertEquals(WorkDuration.of(15, 0), metrics.totalOnlineTime)
    }

    @Test
    fun `calcula ganho por km hora e corrida`() {
        assertEquals(Money.of(2, 0), metrics.revenuePerKm)
        assertEquals(Money.of(40, 0), metrics.revenuePerHour)
        assertEquals(Money.of(20, 0), metrics.revenuePerRide)
    }

    @Test
    fun `calcula lucro por km e por hora`() {
        assertEquals(Money.of(1, 0), metrics.profitPerKm)
        assertEquals(Money.of(20, 0), metrics.profitPerHour)
    }

    @Test
    fun `custo por km usa so despesa operacional`() {
        // Despesas totais sao R$ 300,00 em 300 km, o que daria R$ 1,00/km.
        // Seguro nao varia com o quanto se roda (PRD 22): fora dele, sobram
        // R$ 180,00, e o custo real de rodar e R$ 0,60/km.
        assertEquals(Money.of(180, 0), metrics.operationalExpenses)
        assertEquals(Money.of(120, 0), metrics.fixedExpenses)
        assertEquals(Money.of(0, 60), metrics.costPerKm)
    }

    @Test
    fun `IPVA e financiamento tambem sao custo fixo`() {
        val fixedOnly = DashboardMetrics.of(
            sessions = emptyList(),
            expenses = listOf(
                expense(ExpenseCategory.VEHICLE_TAX, Money.of(400, 0)),
                expense(ExpenseCategory.FINANCING, Money.of(900, 0)),
            ),
        )

        assertEquals(Money.ZERO, fixedOnly.operationalExpenses)
        assertEquals(Money.of(1_300, 0), fixedOnly.fixedExpenses)
    }

    @Test
    fun `separa despesas por natureza`() {
        assertEquals(Money.of(150, 0), metrics.expensesByCategory[ExpenseCategory.FUEL])
        assertEquals(Money.of(30, 0), metrics.expensesByCategory[ExpenseCategory.MAINTENANCE])
        assertEquals(Money.of(120, 0), metrics.expensesByCategory[ExpenseCategory.INSURANCE])
    }

    // --- Uso pessoal (v0.7.0) ---

    /**
     * R$ 900 de custo operacional em 1.000 km totais — 800 de trabalho e 200
     * pessoais. É o exemplo que está no PRD §22.
     */
    private val comUsoPessoal = DashboardMetrics.of(
        sessions = listOf(session(Money.of(1_280, 0), rides = 62, minutes = 2_310, km = 800)),
        expenses = listOf(
            expense(ExpenseCategory.FUEL, Money.of(600, 0)),
            expense(ExpenseCategory.MAINTENANCE, Money.of(200, 0)),
            expense(ExpenseCategory.CAR_WASH, Money.of(100, 0)),
        ),
        personalKilometers = 200,
    )

    @Test
    fun `custo por km usa a distancia total e nao so a de trabalho`() {
        // Sem contar o km pessoal daria R$ 1,125/km - o indicador central do
        // produto inflado em 25% para quem usa o carro no fim de semana.
        assertEquals(1_000L, comUsoPessoal.totalKilometers)
        assertEquals(Money.of(0, 90), comUsoPessoal.costPerKm)
    }

    @Test
    fun `a reparticao soma exatamente a despesa operacional`() {
        // As duas linhas da tela precisam fechar contra o extrato. Por isso a
        // parte pessoal e calculada por diferenca, e nao por proporcao propria.
        assertEquals(Money.of(720, 0), comUsoPessoal.workOperationalCost)
        assertEquals(Money.of(180, 0), comUsoPessoal.personalOperationalCost)
        assertEquals(
            comUsoPessoal.operationalExpenses,
            comUsoPessoal.workOperationalCost + comUsoPessoal.personalOperationalCost,
        )
    }

    @Test
    fun `o lucro nao paga o combustivel do fim de semana`() {
        // Faturamento 1.280 menos os 720 que cabem ao trabalho.
        assertEquals(Money.of(720, 0), comUsoPessoal.workExpenses)
        assertEquals(Money.of(560, 0), comUsoPessoal.netProfit)
    }

    @Test
    fun `sem uso pessoal o resultado e identico ao de antes`() {
        // Garantia de nao regressao: quem nao declara nada nao pode ver
        // numero nenhum mudar.
        val semPessoal = DashboardMetrics.of(
            sessions = listOf(session(Money.of(600, 0), rides = 30, minutes = 900, km = 300)),
            expenses = listOf(expense(ExpenseCategory.FUEL, Money.of(180, 0))),
        )

        assertEquals(300L, semPessoal.totalKilometers)
        assertEquals(Money.of(180, 0), semPessoal.workOperationalCost)
        assertEquals(Money.ZERO, semPessoal.personalOperationalCost)
        assertEquals(semPessoal.totalExpenses, semPessoal.workExpenses)
        assertEquals(Money.of(420, 0), semPessoal.netProfit)
        assertFalse(semPessoal.hasPersonalUsage)
    }

    @Test
    fun `custo fixo nao entra no rateio com o uso pessoal`() {
        // Passear no domingo nao gera parcela de financiamento (PRD 22).
        val comFinanciamento = DashboardMetrics.of(
            sessions = listOf(session(Money.of(2_000, 0), rides = 60, minutes = 2_400, km = 800)),
            expenses = listOf(
                expense(ExpenseCategory.FUEL, Money.of(600, 0)),
                expense(ExpenseCategory.FINANCING, Money.of(1_200, 0)),
            ),
            personalKilometers = 200,
        )

        assertEquals(Money.of(1_200, 0), comFinanciamento.fixedExpenses)
        // 600 x 800/1000 = 480 de operacional, mais os 1.200 inteiros.
        assertEquals(Money.of(480, 0), comFinanciamento.workOperationalCost)
        assertEquals(Money.of(1_680, 0), comFinanciamento.workExpenses)
        // O custo/km continua olhando so o operacional sobre a distancia total.
        assertEquals(Money.of(0, 60), comFinanciamento.costPerKm)
    }

    @Test
    fun `sem quilometro nenhum o custo inteiro fica com o trabalho`() {
        // Nao ha base para repartir; o comportamento conservador e o mesmo de
        // antes da v0.7.0.
        val semKm = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), rides = 5, minutes = 300, km = 0)),
            expenses = listOf(expense(ExpenseCategory.FUEL, Money.of(40, 0))),
        )

        assertEquals(Money.of(40, 0), semKm.workOperationalCost)
        assertEquals(Money.ZERO, semKm.personalOperationalCost)
    }

    @Test
    fun `periodo so com uso pessoal nao e vazio`() {
        val soPessoal = DashboardMetrics.of(
            sessions = emptyList(),
            expenses = emptyList(),
            personalKilometers = 150,
        )

        assertFalse(soPessoal.isEmpty)
        assertTrue(soPessoal.hasPersonalUsage)
    }

    @Test
    fun `periodo sem quilometros nao tem indicador por km`() {
        // Nao e R$ 0,00/km: o indicador simplesmente nao existe (PRD 21).
        val noDistance = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), rides = 5, minutes = 120, km = 0)),
            expenses = listOf(expense(ExpenseCategory.FUEL, Money.of(40, 0))),
        )

        assertNull(noDistance.revenuePerKm)
        assertNull(noDistance.costPerKm)
        assertNull(noDistance.profitPerKm)
    }

    @Test
    fun `periodo sem horas nao tem indicador por hora`() {
        val noTime = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), rides = 5, minutes = 0, km = 60)),
            expenses = emptyList(),
        )

        assertNull(noTime.revenuePerHour)
        assertNull(noTime.profitPerHour)
    }

    @Test
    fun `periodo sem corridas nao tem ganho por corrida`() {
        val noRides = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), rides = 0, minutes = 300, km = 60)),
            expenses = emptyList(),
        )

        assertNull(noRides.revenuePerRide)
    }

    @Test
    fun `periodo sem lancamento nenhum e vazio e nao estoura`() {
        val empty = DashboardMetrics.of(sessions = emptyList(), expenses = emptyList())

        assertEquals(DashboardMetrics.EMPTY, empty)
        assertTrue(empty.isEmpty)
        assertEquals(Money.ZERO, empty.netProfit)
        assertNull(empty.revenuePerKm)
        assertNull(empty.revenuePerHour)
        assertNull(empty.revenuePerRide)
        assertNull(empty.costPerKm)
        assertNull(empty.profitPerKm)
        assertNull(empty.profitPerHour)
    }

    @Test
    fun `prejuizo aparece como lucro negativo por km e por hora`() {
        // Gastar mais do que se fatura precisa produzir numero negativo, e nao
        // zero nem ausencia de indicador.
        val loss = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), rides = 5, minutes = 600, km = 100)),
            expenses = listOf(expense(ExpenseCategory.MAINTENANCE, Money.of(250, 0))),
        )

        assertEquals(Money.of(-150, 0), loss.netProfit)
        assertEquals(Money.of(-1, 50), loss.profitPerKm)
        assertEquals(Money.of(-15, 0), loss.profitPerHour)
        assertTrue(loss.netProfit.isNegative)
    }

    @Test
    fun `recarga gratuita conta como lancamento do periodo`() {
        // R$ 0,00 e valor valido (PRD 11). Se "vazio" fosse deduzido so dos
        // totais, um periodo com carga gratuita sumiria da tela.
        val freeCharge = DashboardMetrics.of(
            sessions = emptyList(),
            expenses = listOf(expense(ExpenseCategory.CHARGING, Money.ZERO)),
        )

        assertFalse(freeCharge.isEmpty)
        assertEquals(Money.ZERO, freeCharge.totalExpenses)
    }

    @Test
    fun `jornada sem faturamento nao e periodo vazio`() {
        val badDay = DashboardMetrics.of(
            sessions = listOf(session(Money.ZERO, rides = 0, minutes = 300, km = 0)),
            expenses = emptyList(),
        )

        assertFalse(badDay.isEmpty)
        assertEquals(WorkDuration.of(5, 0), badDay.totalOnlineTime)
    }
}
