package com.driverpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.driverpro.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acesso às sessões de trabalho. Consumido apenas por repositories (PRD §25).
 *
 * A ordenação é por data e, em empate, pelo id: dois lançamentos do mesmo dia
 * precisam sair sempre na mesma ordem, senão a lista "pula" a cada
 * recomposição.
 */
@Dao
interface WorkSessionDao {

    @Query("SELECT * FROM work_sessions ORDER BY date DESC, id DESC")
    fun observeAll(): Flow<List<WorkSessionEntity>>

    /**
     * Sessões de um período, inclusive nas duas pontas.
     *
     * `date` é epoch day, então o BETWEEN é comparação numérica e usa o
     * índice — não há parsing de texto envolvido.
     */
    @Query(
        """
        SELECT * FROM work_sessions
        WHERE date BETWEEN :startEpochDay AND :endEpochDay
        ORDER BY date DESC, id DESC
        """,
    )
    fun observeBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<WorkSessionEntity>>

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun findById(id: Long): WorkSessionEntity?

    @Query("SELECT COUNT(*) FROM work_sessions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(session: WorkSessionEntity): Long

    @Update
    suspend fun update(session: WorkSessionEntity)

    @Delete
    suspend fun delete(session: WorkSessionEntity)

    @Query("DELETE FROM work_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
