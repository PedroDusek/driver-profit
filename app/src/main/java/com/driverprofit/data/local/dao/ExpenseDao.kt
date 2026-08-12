package com.driverprofit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.driverprofit.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/** Acesso às despesas. Consumido apenas por repositories (PRD §25). */
@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    /**
     * Despesas de um período, inclusive nas duas pontas.
     *
     * `date` é epoch day, então o BETWEEN é comparação numérica e usa o índice.
     */
    @Query(
        """
        SELECT * FROM expenses
        WHERE date BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: Long): ExpenseEntity?

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun count(): Int

    @Insert
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}
