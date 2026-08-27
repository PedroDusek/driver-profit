package com.driverpro.dashboard.domain

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.earnings.domain.Platform
import com.driverpro.earnings.domain.WorkSession
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class DashboardComparisonTest {

    private val day = LocalDate.of(2026, 8, 12)

    private fun session(revenue: Money, km: Long = 100, minutes: Long = 600) = WorkSession(
        date = day,
        platform = Platform.UBER,
        rides = 10,
        revenue = revenue,
        onlineTime = WorkDuration(minutes),
        distanceKm = km,
        createdAt = Instant.EPOCH,
    )

    private fun expense(amount: Money, category: ExpenseCategory = ExpenseCategory.FUEL) = Expense(
        date = day,
        category = category,
        amount = amount,
        odometerKm = 100_000,
        createdAt = Instant.EPOCH,
    )

    private fun metrics(revenue: Money, expense: Money) = DashboardMetrics.of(
        sessions = listOf(session(revenue)),
        expenses = if (expense.isZero) emptyList() else listOf(expense(expense)),
    )

    private fun comparison(
        currentRevenue: Money,
        currentExpense: Money,
        previousRevenue: Money,
        previousExpense: Money,
    ) = DashboardComparison(
        current = metrics(currentRevenue, currentExpense),
        previous = metrics(previousRevenue, previousExpense),
    )

    @Test
    fun `lucro que sobe e variacao positiva e boa`() {
        // Faturou 200 gastando 50 (lucro 150) contra 100 gastando 50
        // (lucro 50): 100 a mais sobre uma base de 50 e 200%.
        val r = comparison(
            currentRevenue = Money.of(200, 0), currentExpense = Money.of(50, 0),
            previousRevenue = Money.of(100, 0), previousExpense = Money.of(50, 0),
        ).netProfit as MetricChange.Measured

        assertEquals(200, r.percent)
        assertTrue(r.rising)
        assertTrue(r.better)
        assertEquals(Money.of(100, 0), r.absolute)
    }

    @Test
    fun `custo que sobe e variacao positiva mas ruim`() {
        // A polaridade e o que separa os dois casos. Pintar isto de verde
        // afirmaria o contrario do que aconteceu, que e pior que nao mostrar.
        val r = comparison(
            currentRevenue = Money.of(100, 0), currentExpense = Money.of(80, 0),
            previousRevenue = Money.of(100, 0), previousExpense = Money.of(40, 0),
        ).workExpenses as MetricChange.Measured

        assertEquals(100, r.percent)
        assertTrue(r.rising)
        assertFalse("custo subindo nunca e melhora", r.better)
    }

    @Test
    fun `custo que cai e melhora`() {
        val r = comparison(
            currentRevenue = Money.of(100, 0), currentExpense = Money.of(20, 0),
            previousRevenue = Money.of(100, 0), previousExpense = Money.of(40, 0),
        ).workExpenses as MetricChange.Measured

        assertEquals(50, r.percent)
        assertFalse(r.rising)
        assertTrue(r.better)
    }

    @Test
    fun `prejuizo que encolhe le como melhora, nao como piora`() {
        // Prejuizo de 100 virando prejuizo de 50. Sem o modulo no
        // denominador, o sinal negativo da base inverteria o resultado e o
        // app diria que piorou 50% justamente quando melhorou.
        val r = comparison(
            currentRevenue = Money.of(50, 0), currentExpense = Money.of(100, 0),
            previousRevenue = Money.of(0, 0), previousExpense = Money.of(100, 0),
        ).netProfit as MetricChange.Measured

        assertEquals(50, r.percent)
        assertTrue(r.rising)
        assertTrue("prejuizo menor e melhora", r.better)
    }

    @Test
    fun `mesmo numero nos dois periodos e variacao zero, nem boa nem ruim`() {
        val r = comparison(
            currentRevenue = Money.of(100, 0), currentExpense = Money.of(30, 0),
            previousRevenue = Money.of(100, 0), previousExpense = Money.of(30, 0),
        ).netProfit as MetricChange.Measured

        assertEquals(0, r.percent)
        assertTrue(r.isFlat)
        assertFalse(r.better)
    }

    @Test
    fun `periodo anterior sem lancamento nenhum nao tem base`() {
        // Nao e o mesmo que variacao zero: ninguem rodou, entao nao ha com o
        // que comparar. A tela diz isso com todas as letras.
        val r = DashboardComparison(
            current = metrics(Money.of(300, 0), Money.of(50, 0)),
            previous = DashboardMetrics.EMPTY,
        )

        assertEquals(MetricChange.NoBaseline, r.netProfit)
        assertEquals(MetricChange.NoBaseline, r.totalRevenue)
        assertEquals(MetricChange.NoBaseline, r.costPerKm)
    }

    @Test
    fun `base zero nao produz porcentagem`() {
        // Houve periodo anterior, mas o indicador valia zero la. Qualquer
        // valor novo seria "infinito por cento": mesma regra do Money.per,
        // divisao sem denominador devolve ausencia, nunca zero.
        val r = DashboardComparison(
            current = metrics(Money.of(300, 0), Money.of(50, 0)),
            // Faturou zero mas rodou, entao o periodo anterior nao esta vazio.
            previous = DashboardMetrics.of(
                sessions = listOf(session(Money.ZERO)),
                expenses = emptyList(),
            ),
        )

        assertEquals(MetricChange.NoBase, r.totalRevenue)
    }

    @Test
    fun `razao ausente de um dos lados nao tem variacao a declarar`() {
        // Periodo anterior com lancamento mas sem horas: revenuePerHour e
        // null la, entao nao ha do que subtrair.
        val semHoras = DashboardMetrics.of(
            sessions = listOf(session(Money.of(100, 0), minutes = 0)),
            expenses = emptyList(),
        )
        val r = DashboardComparison(
            current = metrics(Money.of(300, 0), Money.of(50, 0)),
            previous = semHoras,
        )

        assertEquals(MetricChange.NoBase, r.revenuePerHour)
    }

    @Test
    fun `porcentagem arredonda meio para cima`() {
        // 100 -> 133: 33% exatos. 100 -> 1335 centavos daria 33,5%, que
        // precisa virar 34 e nao 33.
        val r = DashboardComparison(
            current = DashboardMetrics.of(
                sessions = listOf(session(Money(13_350))),
                expenses = emptyList(),
            ),
            previous = DashboardMetrics.of(
                sessions = listOf(session(Money(10_000))),
                expenses = emptyList(),
            ),
        ).totalRevenue as MetricChange.Measured

        assertEquals(34, r.percent)
    }
}
