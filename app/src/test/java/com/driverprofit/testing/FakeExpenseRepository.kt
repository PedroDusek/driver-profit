package com.driverprofit.testing

import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Repositório em memória para testes de use case e ViewModel.
 *
 * Reproduz o que importa do `ExpenseDao`: ids auto-incrementais, ordenação por
 * data decrescente com desempate por id, e filtro de período inclusivo nas
 * duas pontas.
 */
class FakeExpenseRepository(
    initialExpenses: List<Expense> = emptyList(),
) : ExpenseRepository {

    private val expenses = MutableStateFlow(initialExpenses)
    private var nextId = (initialExpenses.maxOfOrNull { it.id } ?: 0L) + 1

    /** Instantâneo do estado atual, para asserções diretas nos testes. */
    val current: List<Expense> get() = expenses.value

    private fun List<Expense>.sorted(): List<Expense> =
        sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })

    override fun observeExpenses(): Flow<List<Expense>> = expenses.map { it.sorted() }

    override fun observeExpensesBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>> =
        expenses.map { list -> list.filter { it.date >= start && it.date <= end }.sorted() }

    override suspend fun getExpense(id: Long): Expense? =
        expenses.value.firstOrNull { it.id == id }

    override suspend fun addExpense(expense: Expense): Long {
        val id = nextId++
        expenses.value = expenses.value + expense.copy(id = id)
        return id
    }

    override suspend fun updateExpense(expense: Expense) {
        expenses.value = expenses.value.map { if (it.id == expense.id) expense else it }
    }

    override suspend fun deleteExpense(id: Long) {
        expenses.value = expenses.value.filterNot { it.id == id }
    }
}
