package com.driverpro.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.driverpro.data.local.entity.ReconciliationDismissalEntity
import kotlinx.coroutines.flow.Flow

/** Acesso às dispensas de conciliação. Consumido apenas por repositories (PRD §25). */
@Dao
interface ReconciliationDismissalDao {

    @Query("SELECT * FROM reconciliation_dismissals")
    fun observeAll(): Flow<List<ReconciliationDismissalEntity>>

    /**
     * Grava a decisão, substituindo a anterior daquela janela.
     *
     * `REPLACE` sobre o índice único: decidir de novo sobre a mesma janela é
     * sempre sobrescrita, e resolvê-la aqui evita a corrida entre ler o id
     * existente e gravar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dismissal: ReconciliationDismissalEntity): Long

    @Query("SELECT COUNT(*) FROM reconciliation_dismissals")
    suspend fun count(): Int
}
