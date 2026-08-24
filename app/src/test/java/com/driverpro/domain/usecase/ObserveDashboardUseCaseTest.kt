package com.driverpro.domain.usecase

import com.driverpro.core.domain.Money
import com.driverpro.core.domain.DateRange
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.ObserveAccruedExpensesUseCase
import com.driverpro.expenses.domain.ObserveExpensesBetweenUseCase
import com.driverpro.expenses.domain.FakeExpenseRepository
import com.driverpro.testing.FakePersonalUsageRepository
import com.driverpro.testing.FakeWorkSessionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ObserveDashboardUseCaseTest {

    private val agosto = DateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31))

    private fun useCase(expenses: FakeExpenseRepository) = ObserveDashboardUseCase(
        observeWorkSessionsBetween = ObserveWorkSessionsBetweenUseCase(FakeWorkSessionRepository()),
        observeExpensesBetween = ObserveExpensesBetweenUseCase(expenses),
        observePersonalUsageInPeriod =
            ObservePersonalUsageInPeriodUseCase(FakePersonalUsageRepository()),
        observeAccruedInPeriod = ObserveAccruedExpensesUseCase(expenses),
    )

    /**
     * Simula uma linha de IPVA gravada antes da v0.11.0, quando a categoria
     * ainda aceitava competência. O dashboard precisa ignorá-la mesmo assim —
     * não pode depender de nenhuma migração de dado ter limpado isso.
     */
    @Test
    fun `ipva com competencia de versao anterior nao entra no custo fixo rateado`() = runTest {
        val expenses = FakeExpenseRepository(
            listOf(
                Expense(
                    date = LocalDate.of(2026, 1, 15),
                    category = ExpenseCategory.VEHICLE_TAX,
                    amount = Money.of(1200, 0),
                    accrual = DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                    createdAt = Instant.EPOCH,
                ),
            ),
        )

        val metrics = useCase(expenses).invoke(agosto).first()

        assertEquals(Money.ZERO, metrics.accruedFixedCost)
    }

    @Test
    fun `ipva sem competencia conta inteiro so no mes lancado`() = runTest {
        val expenses = FakeExpenseRepository(
            listOf(
                Expense(
                    date = LocalDate.of(2026, 8, 10),
                    category = ExpenseCategory.VEHICLE_TAX,
                    amount = Money.of(1200, 0),
                    createdAt = Instant.EPOCH,
                ),
            ),
        )

        val agostoMetrics = useCase(expenses).invoke(agosto).first()
        val setembro = DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))
        val setembroMetrics = useCase(expenses).invoke(setembro).first()

        assertEquals(Money.ZERO, agostoMetrics.accruedFixedCost)
        assertEquals(Money.of(1200, 0), agostoMetrics.fixedExpenses)
        assertEquals(Money.ZERO, setembroMetrics.fixedExpenses)
    }

    @Test
    fun `seguro com competencia continua ratado pelo custo fixo`() = runTest {
        val expenses = FakeExpenseRepository(
            listOf(
                Expense(
                    date = LocalDate.of(2026, 3, 5),
                    category = ExpenseCategory.INSURANCE,
                    amount = Money.of(1200, 0),
                    accrual = DateRange(LocalDate.of(2026, 3, 1), LocalDate.of(2027, 2, 28)),
                    createdAt = Instant.EPOCH,
                ),
            ),
        )

        val metrics = useCase(expenses).invoke(agosto).first()

        // 31 dias de agosto sobre 365 dias de competencia, de R$ 1200,00.
        assertEquals(Money.of(101, 92), metrics.accruedFixedCost)
    }
}
