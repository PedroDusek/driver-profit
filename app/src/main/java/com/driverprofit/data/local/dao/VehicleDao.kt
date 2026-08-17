package com.driverprofit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.driverprofit.data.local.entity.VehicleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Acesso ao veículo. Consumido apenas por repositories (PRD §25): nem
 * ViewModel nem Composable podem tocar em DAO.
 */
@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles ORDER BY created_at DESC")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<VehicleEntity?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun findById(id: Long): VehicleEntity?

    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun count(): Int

    @Insert
    suspend fun insert(vehicle: VehicleEntity): Long

    @Update
    suspend fun update(vehicle: VehicleEntity)

    @Delete
    suspend fun delete(vehicle: VehicleEntity)

    @Query("DELETE FROM vehicles WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE vehicles SET is_current = 0")
    suspend fun clearCurrent()

    @Query("UPDATE vehicles SET is_current = 1 WHERE id = :id")
    suspend fun markCurrent(id: Long)

    /** Troca o veículo atual atomicamente — nunca dois nem nenhum no meio do caminho. */
    @Transaction
    suspend fun setCurrent(id: Long) {
        clearCurrent()
        markCurrent(id)
    }

    @Query("SELECT COUNT(*) FROM vehicles WHERE is_current = 1")
    suspend fun countCurrent(): Int

    @Query(
        "UPDATE vehicles SET is_current = 1 " +
            "WHERE id = (SELECT id FROM vehicles ORDER BY created_at ASC LIMIT 1)",
    )
    suspend fun markOldestCurrent()

    /** Sem efeito se já existe um atual, ou se não há veículo nenhum. */
    @Transaction
    suspend fun promoteOldestToCurrentIfNone() {
        if (countCurrent() == 0) markOldestCurrent()
    }
}
