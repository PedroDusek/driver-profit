package com.driverpro.testing

import com.driverpro.domain.model.DateRange
import com.driverpro.domain.model.Expense
import com.driverpro.domain.repository.ExpenseRepository
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

    /**
     * Sobreposição de competência, como o DAO — e não contenção por data.
     *
     * É o filtro mais fácil de errar aqui: o IPVA pago em janeiro precisa
     * aparecer em agosto, e a data dele está a sete meses de distância.
     */
    override fun observeAccruedBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>> =
        expenses.map { list ->
            list.filter { expense ->
                val accrual = expense.accrual
                accrual != null && accrual.start <= end && accrual.end >= start
            }.sorted()
        }

    /** `MAX`, como o DAO: odômetro só cresce e o lançamento pode vir fora de ordem. */
    override fun observeLatestOdometer(vehicleId: Long): Flow<Long?> = expenses.map { list ->
        list.filter { it.vehicleId == vehicleId }.mapNotNull { it.odometerKm }.maxOrNull()
    }

    override fun observeOdometers(): Flow<Map<Long, Long>> = expenses.map { list ->
        list.filter { it.vehicleId != null && it.odometerKm != null }
            .groupBy { it.vehicleId!! }
            .mapValues { (_, rows) -> rows.maxOf { it.odometerKm!! } }
    }

    /**
     * Parte da última leitura anterior ao período, como o repositório real —
     * senão o trecho entre ela e a primeira leitura de dentro sumiria.
     */
    override suspend fun odometerDistanceIn(vehicleId: Long, period: DateRange): Long? {
        val doVeiculo = expenses.value.filter { it.vehicleId == vehicleId }
        val dentro = doVeiculo
            .filter { it.date in period }
            .mapNotNull { it.odometerKm }
        val ultima = dentro.maxOrNull() ?: return null

        val antes = doVeiculo
            .filter { it.date < period.start }
            .mapNotNull { it.odometerKm }
            .maxOrNull()

        val primeira = antes ?: dentro.minOrNull() ?: return null
        return (ultima - primeira).takeIf { it >= 0L }
    }

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
