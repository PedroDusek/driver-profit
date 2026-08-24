package com.driverpro.feature.expenses

import com.driverpro.core.domain.Money
import com.driverpro.domain.model.Expense
import com.driverpro.domain.model.ExpenseCategory
import com.driverpro.domain.usecase.DeleteExpenseUseCase
import com.driverpro.domain.usecase.ObserveExpensesUseCase
import com.driverpro.feature.expenses.list.ExpenseFilter
import com.driverpro.feature.expenses.list.ExpensesListUiState
import com.driverpro.feature.expenses.list.ExpensesListViewModel
import com.driverpro.testing.FakeExpenseRepository
import com.driverpro.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun expense(
        id: Long,
        category: ExpenseCategory,
        amount: Money,
        date: LocalDate = LocalDate.of(2026, 8, 11),
    ) = Expense(
        id = id,
        date = date,
        category = category,
        amount = amount,
        createdAt = Instant.EPOCH,
    )

    private fun viewModel(repository: FakeExpenseRepository) = ExpensesListViewModel(
        observeExpenses = ObserveExpensesUseCase(repository),
        deleteExpense = DeleteExpenseUseCase(repository),
    )

    private val mixedRepository = FakeExpenseRepository(
        listOf(
            expense(1, ExpenseCategory.FUEL, Money.of(210, 0)),
            expense(2, ExpenseCategory.MAINTENANCE, Money.of(320, 0)),
            expense(3, ExpenseCategory.TOLL, Money.of(12, 50)),
            expense(4, ExpenseCategory.INSURANCE, Money.of(180, 0)),
        ),
    )

    @Test
    fun `estado inicial e Loading`() = runTest {
        assertEquals(
            ExpensesListUiState.Loading,
            viewModel(FakeExpenseRepository()).uiState.value,
        )
    }

    @Test
    fun `historico vazio produz estado Empty`() = runTest {
        val viewModel = viewModel(FakeExpenseRepository())

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        assertEquals(ExpensesListUiState.Empty, viewModel.uiState.value)
        job.cancel()
    }

    @Test
    fun `resumo soma tudo e separa por categoria`() = runTest {
        val viewModel = viewModel(mixedRepository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val summary = (viewModel.uiState.value as ExpensesListUiState.Content).summary
        assertEquals(Money.of(722, 50), summary.total)
        assertEquals(Money.of(210, 0), summary.byCategory[ExpenseCategory.FUEL])
        assertEquals(Money.of(320, 0), summary.byCategory[ExpenseCategory.MAINTENANCE])
        job.cancel()
    }

    @Test
    fun `custos fixos ficam separados do custo operacional`() = runTest {
        val viewModel = viewModel(mixedRepository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        // Seguro nao varia com o quanto se roda; misturar no custo/km de um
        // dia distorceria o indicador (PRD 22).
        val summary = (viewModel.uiState.value as ExpensesListUiState.Content).summary
        assertEquals(Money.of(542, 50), summary.operational)
        assertEquals(Money.of(180, 0), summary.fixed)
        job.cancel()
    }

    @Test
    fun `filtro de manutencao mostra so manutencao`() = runTest {
        val viewModel = viewModel(mixedRepository)

        val job = launchCollector(viewModel)
        viewModel.onFilterChange(ExpenseFilter.MAINTENANCE)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExpensesListUiState.Content
        assertEquals(listOf(2L), state.expenses.map { it.id })
        job.cancel()
    }

    @Test
    fun `filtro de combustivel inclui carregamento eletrico`() = runTest {
        val repository = FakeExpenseRepository(
            listOf(
                expense(1, ExpenseCategory.FUEL, Money.of(210, 0)),
                expense(2, ExpenseCategory.CHARGING, Money.of(45, 0)),
                expense(3, ExpenseCategory.TOLL, Money.of(12, 50)),
            ),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        viewModel.onFilterChange(ExpenseFilter.FUEL_AND_CHARGING)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExpensesListUiState.Content
        assertEquals(setOf(1L, 2L), state.expenses.map { it.id }.toSet())
        job.cancel()
    }

    @Test
    fun `o total acompanha o filtro`() = runTest {
        val viewModel = viewModel(mixedRepository)

        val job = launchCollector(viewModel)
        viewModel.onFilterChange(ExpenseFilter.MAINTENANCE)
        advanceUntilIdle()

        // Se o filtro mostra so manutencao, o total tem que ser o de
        // manutencao - senao a tela mente.
        val state = viewModel.uiState.value as ExpensesListUiState.Content
        assertEquals(Money.of(320, 0), state.summary.total)
        job.cancel()
    }

    @Test
    fun `filtro sem resultado nao vira estado vazio`() = runTest {
        val repository = FakeExpenseRepository(
            listOf(expense(1, ExpenseCategory.TOLL, Money.of(12, 50))),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        viewModel.onFilterChange(ExpenseFilter.MAINTENANCE)
        advanceUntilIdle()

        // "Voce nao tem despesas" e "o filtro nao achou nada" sao mensagens
        // diferentes.
        val state = viewModel.uiState.value
        assertTrue(state is ExpensesListUiState.Content)
        assertTrue((state as ExpensesListUiState.Content).filteredOut)
        job.cancel()
    }

    @Test
    fun `lista vem da data mais recente para a mais antiga`() = runTest {
        val repository = FakeExpenseRepository(
            listOf(
                expense(1, ExpenseCategory.TOLL, Money.of(10, 0), LocalDate.of(2026, 8, 1)),
                expense(2, ExpenseCategory.TOLL, Money.of(20, 0), LocalDate.of(2026, 8, 11)),
            ),
        )
        val viewModel = viewModel(repository)

        val job = launchCollector(viewModel)
        advanceUntilIdle()

        val state = viewModel.uiState.value as ExpensesListUiState.Content
        assertEquals(listOf(2L, 1L), state.expenses.map { it.id })
        job.cancel()
    }

    @Test
    fun `confirmar exclusao remove a despesa`() = runTest {
        val target = expense(1, ExpenseCategory.TOLL, Money.of(12, 50))
        val repository = FakeExpenseRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertNull(viewModel.expensePendingDeletion.value)
        assertTrue(repository.current.isEmpty())
    }

    @Test
    fun `cancelar exclusao mantem a despesa`() = runTest {
        val target = expense(1, ExpenseCategory.TOLL, Money.of(12, 50))
        val repository = FakeExpenseRepository(listOf(target))
        val viewModel = viewModel(repository)

        viewModel.onDeleteRequested(target)
        viewModel.onDeleteDismissed()
        advanceUntilIdle()

        assertNull(viewModel.expensePendingDeletion.value)
        assertEquals(1, repository.current.size)
        assertFalse(repository.current.isEmpty())
    }

    private fun TestScope.launchCollector(viewModel: ExpensesListViewModel): Job =
        launch { viewModel.uiState.collect {} }
}
