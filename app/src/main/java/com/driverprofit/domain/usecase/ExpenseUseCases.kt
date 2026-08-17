package com.driverprofit.domain.usecase

import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.ExpenseDraft
import com.driverprofit.domain.model.ExpenseFieldError
import com.driverprofit.domain.repository.ExpenseRepository
import com.driverprofit.domain.repository.VehicleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Resultado de uma tentativa de salvar despesa. */
sealed interface SaveExpenseResult {

    data class Success(val id: Long) : SaveExpenseResult

    /** Rejeitada pela validação. Nada foi gravado. */
    data class Invalid(val errors: List<ExpenseFieldError>) : SaveExpenseResult
}

/**
 * Registra ou atualiza uma despesa (PRD §17).
 *
 * Busca o veículo antes de validar: parte das regras depende do que aquele
 * carro aceita, e deixar essa consulta na ViewModel espalharia regra de
 * negócio para fora do domínio (PRD §25).
 */
class SaveExpenseUseCase(
    private val expenseRepository: ExpenseRepository,
    private val vehicleRepository: VehicleRepository,
    private val validator: ExpenseValidator,
) {

    suspend operator fun invoke(draft: ExpenseDraft): SaveExpenseResult {
        val vehicle = draft.vehicleId?.let { vehicleRepository.getVehicle(it) }

        val readings = draft.vehicleId?.let { readingsFor(it, exceptId = draft.id) }.orEmpty()

        val errors = validator.validate(draft, vehicle, OdometerHistory(readings))
        if (errors.isNotEmpty()) return SaveExpenseResult.Invalid(errors)

        if (!draft.isEditing) {
            return SaveExpenseResult.Success(
                expenseRepository.addExpense(validator.toExpense(draft)),
            )
        }

        // Preserva o createdAt original: corrigir o valor de um abastecimento
        // não muda quando ele entrou no app.
        val existing = expenseRepository.getExpense(draft.id)
            ?: return SaveExpenseResult.Success(
                expenseRepository.addExpense(validator.toExpense(draft)),
            )

        expenseRepository.updateExpense(validator.toExpense(draft, createdAt = existing.createdAt))
        return SaveExpenseResult.Success(draft.id)
    }

    /**
     * Leituras já registradas do veículo, exceto a do próprio lançamento.
     *
     * A exclusão importa na edição: sem ela, corrigir uma despesa a compararia
     * consigo mesma e qualquer alteração de leitura seria recusada.
     */
    private suspend fun readingsFor(vehicleId: Long, exceptId: Long): List<OdometerReading> =
        expenseRepository.observeExpenses().first()
            .filter { it.vehicleId == vehicleId && it.id != exceptId }
            .mapNotNull { expense ->
                expense.odometerKm?.let { OdometerReading(expense.date, it) }
            }
}

/** Observa o histórico completo de despesas (PRD §19). */
class ObserveExpensesUseCase(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(): Flow<List<Expense>> = repository.observeExpenses()
}

/** Observa as despesas de um período — base do custo/km do dashboard. */
class ObserveExpensesBetweenUseCase(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(start: LocalDate, end: LocalDate): Flow<List<Expense>> =
        repository.observeExpensesBetween(start, end)
}

/**
 * Despesas cuja competência encosta no período — base do custo fixo por km
 * (PRD §22).
 */
class ObserveAccruedExpensesUseCase(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(range: DateRange): Flow<List<Expense>> =
        repository.observeAccruedBetween(range.start, range.end)
}

/**
 * Quilometragem atual de um veículo (PRD §23).
 *
 * É a maior leitura de odômetro já lançada para ele. `null` enquanto não
 * houver nenhuma — inclusive para todo o histórico anterior à v0.6.0, que não
 * tem leitura.
 */
class ObserveVehicleOdometerUseCase(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(vehicleId: Long): Flow<Long?> =
        repository.observeLatestOdometer(vehicleId)
}

/** Quilometragem atual de todos os veículos, para a lista. */
class ObserveVehicleOdometersUseCase(
    private val repository: ExpenseRepository,
) {
    operator fun invoke(): Flow<Map<Long, Long>> = repository.observeOdometers()
}

/** Busca uma despesa específica para edição. */
class GetExpenseUseCase(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: Long): Expense? = repository.getExpense(id)
}

/** Exclui uma despesa. */
class DeleteExpenseUseCase(
    private val repository: ExpenseRepository,
) {
    suspend operator fun invoke(id: Long) = repository.deleteExpense(id)
}
