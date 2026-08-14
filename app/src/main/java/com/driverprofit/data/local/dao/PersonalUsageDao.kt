package com.driverprofit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.driverprofit.data.local.entity.PersonalUsageEntity
import kotlinx.coroutines.flow.Flow

/** Acesso aos lançamentos de uso pessoal. Consumido apenas por repositories. */
@Dao
interface PersonalUsageDao {

    @Query("SELECT * FROM personal_usage ORDER BY start_date DESC, id DESC")
    fun observeAll(): Flow<List<PersonalUsageEntity>>

    /**
     * Lançamentos que **encostam** no período, e não apenas os contidos nele.
     *
     * A condição é de sobreposição de intervalos: uma viagem de 28/07 a 03/08
     * precisa aparecer tanto em julho quanto em agosto, com a parte de km que
     * cabe a cada mês. Filtrar por "começa dentro do período" perderia a
     * segunda metade dela — e o custo/km de agosto voltaria a ficar inflado,
     * que é justamente o defeito que este recurso existe para corrigir.
     *
     * O recorte proporcional em si é do domínio
     * (`PersonalUsage.kilometersWithin`), não do SQL.
     */
    @Query(
        """
        SELECT * FROM personal_usage
        WHERE start_date <= :endEpochDay AND end_date >= :startEpochDay
        ORDER BY start_date DESC, id DESC
        """,
    )
    fun observeOverlapping(
        startEpochDay: Long,
        endEpochDay: Long,
    ): Flow<List<PersonalUsageEntity>>

    /** Uso pessoal já declarado de um veículo num intervalo — base da conciliação. */
    @Query(
        """
        SELECT * FROM personal_usage
        WHERE vehicle_id = :vehicleId
          AND start_date <= :endEpochDay AND end_date >= :startEpochDay
        """,
    )
    suspend fun findOverlappingForVehicle(
        vehicleId: Long,
        startEpochDay: Long,
        endEpochDay: Long,
    ): List<PersonalUsageEntity>

    @Query("SELECT * FROM personal_usage WHERE id = :id")
    suspend fun findById(id: Long): PersonalUsageEntity?

    @Insert
    suspend fun insert(usage: PersonalUsageEntity): Long

    @Update
    suspend fun update(usage: PersonalUsageEntity)

    @Query("DELETE FROM personal_usage WHERE id = :id")
    suspend fun deleteById(id: Long)
}
