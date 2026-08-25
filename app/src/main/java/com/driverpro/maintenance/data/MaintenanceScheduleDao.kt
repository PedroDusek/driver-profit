package com.driverpro.maintenance.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.driverpro.maintenance.data.MaintenanceScheduleEntity
import com.driverpro.maintenance.domain.MaintenanceItem
import kotlinx.coroutines.flow.Flow

/** Acesso aos intervalos de manutenção. Consumido apenas por repositories (PRD §25). */
@Dao
interface MaintenanceScheduleDao {

    /**
     * Todos os intervalos definidos, de todos os veículos.
     *
     * A tabela guarda só o que o motorista alterou, então ela é pequena por
     * construção — carregá-la inteira custa menos que abrir um `Flow` por
     * veículo na tela de manutenção e outro no aviso do dashboard.
     */
    @Query("SELECT * FROM maintenance_schedules")
    fun observeAll(): Flow<List<MaintenanceScheduleEntity>>

    @Query("SELECT * FROM maintenance_schedules WHERE vehicle_id = :vehicleId")
    fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceScheduleEntity>>

    /**
     * Grava o intervalo, substituindo o anterior daquele item.
     *
     * `REPLACE` sobre o índice único `(vehicle_id, item)`: alterar o intervalo
     * de um item é sempre uma sobrescrita, e resolvê-la aqui evita a corrida
     * entre ler o id existente e gravar.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(schedule: MaintenanceScheduleEntity): Long

    /** Devolve o item ao padrão do app — apagar a linha é o que significa "padrão". */
    @Query("DELETE FROM maintenance_schedules WHERE vehicle_id = :vehicleId AND item = :item")
    suspend fun deleteFor(vehicleId: Long, item: MaintenanceItem)

    @Query("SELECT COUNT(*) FROM maintenance_schedules")
    suspend fun count(): Int
}
