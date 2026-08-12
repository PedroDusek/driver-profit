package com.driverprofit.domain.repository

import com.driverprofit.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Contrato de acesso às despesas.
 *
 * A interface vive no domínio e a implementação em `data.repository`.
 */
interface ExpenseRepository {

    /** Emite todas as despesas, da mais recente para a mais antiga. */
    fun observeExpenses(): Flow<List<Expense>>

    /**
     * Despesas de um período, inclusive nas duas pontas.
     *
     * Base dos filtros do dashboard (PRD §20) e do custo por quilômetro.
     */
    fun observeExpensesBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>>

    suspend fun getExpense(id: Long): Expense?

    suspend fun addExpense(expense: Expense): Long

    suspend fun updateExpense(expense: Expense)

    suspend fun deleteExpense(id: Long)
}
