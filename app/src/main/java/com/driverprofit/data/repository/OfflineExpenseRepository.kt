package com.driverprofit.data.repository

import com.driverprofit.data.local.dao.ExpenseDao
import com.driverprofit.data.local.entity.toDomain
import com.driverprofit.data.local.entity.toEntity
import com.driverprofit.domain.model.DateRange
import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Implementação offline-first apoiada em Room. */
class OfflineExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ExpenseRepository {

    override fun observeExpenses(): Flow<List<Expense>> =
        expenseDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeExpensesBetween(start: LocalDate, end: LocalDate): Flow<List<Expense>> =
        expenseDao.observeBetween(start.toEpochDay(), end.toEpochDay())
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeLatestOdometer(vehicleId: Long): Flow<Long?> =
        expenseDao.observeLatestOdometer(vehicleId).flowOn(ioDispatcher)

    override fun observeOdometers(): Flow<Map<Long, Long>> =
        expenseDao.observeOdometers()
            .map { rows -> rows.associate { it.vehicleId to it.odometerKm } }
            .flowOn(ioDispatcher)

    override suspend fun odometerDistanceIn(vehicleId: Long, period: DateRange): Long? =
        withContext(ioDispatcher) {
            val start = period.start.toEpochDay()
            val end = period.end.toEpochDay()

            val last = expenseDao.maxOdometerIn(vehicleId, start, end) ?: return@withContext null
            // Sem leitura anterior ao período, a menor de dentro é o melhor
            // ponto de partida disponível — e aí o trecho anterior a ela
            // simplesmente não entra, o que é honesto.
            val first = expenseDao.odometerBefore(vehicleId, start)
                ?: expenseDao.minOdometerIn(vehicleId, start, end)
                ?: return@withContext null

            (last - first).takeIf { it >= 0L }
        }

    override suspend fun getExpense(id: Long): Expense? =
        withContext(ioDispatcher) { expenseDao.findById(id)?.toDomain() }

    override suspend fun addExpense(expense: Expense): Long =
        withContext(ioDispatcher) { expenseDao.insert(expense.toEntity()) }

    override suspend fun updateExpense(expense: Expense) =
        withContext(ioDispatcher) { expenseDao.update(expense.toEntity()) }

    override suspend fun deleteExpense(id: Long) =
        withContext(ioDispatcher) { expenseDao.deleteById(id) }
}
