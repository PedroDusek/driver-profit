package com.driverprofit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
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
}
