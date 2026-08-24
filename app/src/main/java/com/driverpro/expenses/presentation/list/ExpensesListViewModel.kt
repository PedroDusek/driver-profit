package com.driverpro.expenses.presentation.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driverpro.core.domain.Money
import com.driverpro.expenses.domain.ConsumptionEstimate
import com.driverpro.expenses.domain.ConsumptionEstimator
import com.driverpro.expenses.domain.Expense
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.DeleteExpenseUseCase
import com.driverpro.expenses.domain.ObserveExpensesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Totais do histórico de despesas exibido.
 *
 * Separa combustível, energia, manutenção e outros porque é essa separação que
 * o PRD §22 exige para o custo operacional — e porque "gastei R$ 800" sem
 * saber em quê não ajuda ninguém a decidir nada.
 */
data class ExpensesSummary(
    val total: Money,
    val byCategory: Map<ExpenseCategory, Money>,
) {
    /** Soma apenas o que varia com o quanto se roda (PRD §22). */
    val operational: Money
        get() = Money.sum(
            byCategory.filterKeys { it.isOperationalCost }.values,
        )

    /** Custos fixos: seguro, IPVA e financiamento. */
    val fixed: Money get() = total - operational

    companion object {
        fun of(expenses: List<Expense>): ExpensesSummary = ExpensesSummary(
            total = Money.sum(expenses.map { it.amount }),
            byCategory = expenses
                .groupBy { it.category }
                .mapValues { (_, list) -> Money.sum(list.map { it.amount }) },
        )
    }
}

/** Filtro de natureza aplicado ao histórico (PRD §19). */
enum class ExpenseFilter {
    ALL,
    FUEL_AND_CHARGING,
    MAINTENANCE,
    OTHERS,
    ;

    fun matches(category: ExpenseCategory): Boolean = when (this) {
        ALL -> true
        FUEL_AND_CHARGING -> category == ExpenseCategory.FUEL ||
            category == ExpenseCategory.CHARGING
        MAINTENANCE -> category == ExpenseCategory.MAINTENANCE
        OTHERS -> category != ExpenseCategory.FUEL &&
            category != ExpenseCategory.CHARGING &&
            category != ExpenseCategory.MAINTENANCE
    }
}

/** Estado da tela de histórico de despesas. */
sealed interface ExpensesListUiState {

    data object Loading : ExpensesListUiState

    /** Nenhuma despesa cadastrada — diferente de "o filtro não achou nada". */
    data object Empty : ExpensesListUiState

    data class Content(
        val expenses: List<Expense>,
        val summary: ExpensesSummary,
        val filter: ExpenseFilter,
        /** `true` quando existem despesas, mas nenhuma passa no filtro atual. */
        val filteredOut: Boolean,
        /**
         * Consumo estimado por lançamento (PRD §23).
         *
         * Calculado sobre o histórico **inteiro**, e não sobre o que o filtro
         * mostra: o trecho de um abastecimento nasce da diferença para o
         * anterior, que pode estar fora do filtro.
         */
        val consumption: Map<Long, ConsumptionEstimate> = emptyMap(),
    ) : ExpensesListUiState
}

/**
 * Histórico de despesas (PRD §19).
 *
 * Os totais refletem o que está filtrado: se o motorista pede só manutenção,
 * o número no topo precisa ser o de manutenção, senão a tela mente.
 */
class ExpensesListViewModel(
    observeExpenses: ObserveExpensesUseCase,
    private val deleteExpense: DeleteExpenseUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(ExpenseFilter.ALL)
    private val pendingDeletion = MutableStateFlow<Expense?>(null)

    val expensePendingDeletion: StateFlow<Expense?> = pendingDeletion.asStateFlow()

    val uiState: StateFlow<ExpensesListUiState> =
        combine(observeExpenses(), filter) { expenses, activeFilter ->
            if (expenses.isEmpty()) {
                ExpensesListUiState.Empty
            } else {
                val visible = expenses.filter { activeFilter.matches(it.category) }
                ExpensesListUiState.Content(
                    expenses = visible,
                    summary = ExpensesSummary.of(visible),
                    filter = activeFilter,
                    filteredOut = visible.isEmpty(),
                    consumption = estimateConsumption(expenses),
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ExpensesListUiState.Loading,
        )

    fun onFilterChange(value: ExpenseFilter) {
        filter.value = value
    }

    fun onDeleteRequested(expense: Expense) {
        pendingDeletion.value = expense
    }

    fun onDeleteDismissed() {
        pendingDeletion.value = null
    }

    fun onDeleteConfirmed() {
        val expense = pendingDeletion.value ?: return
        pendingDeletion.value = null
        viewModelScope.launch {
            deleteExpense(expense.id)
        }
    }

    /**
     * Consumo por veículo, indexado pelo lançamento.
     *
     * Agrupado por veículo porque a diferença de odômetro só faz sentido
     * dentro do mesmo carro — dois veículos têm painéis independentes.
     */
    private fun estimateConsumption(expenses: List<Expense>): Map<Long, ConsumptionEstimate> =
        expenses
            .filter { it.vehicleId != null }
            .groupBy { it.vehicleId }
            .values
            .flatMap { ConsumptionEstimator.estimate(it) }
            .associateBy { it.expenseId }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
