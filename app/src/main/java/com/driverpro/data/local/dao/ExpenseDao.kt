package com.driverpro.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.driverpro.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

/** Projeção de `MAX(odometer_km)` por veículo. */
data class VehicleOdometer(
    val vehicleId: Long,
    val odometerKm: Long,
)

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

    /**
     * Despesas cuja **competência encosta** no período (PRD §22).
     *
     * Sobreposição, e não contenção: o IPVA pago em 15/01 com competência de
     * 01/01 a 31/12 precisa aparecer em agosto, e a `date` dele está a sete
     * meses de distância. Filtrar por data perderia exatamente o lançamento que
     * esta consulta existe para encontrar.
     */
    @Query(
        """
        SELECT * FROM expenses
        WHERE accrual_start IS NOT NULL AND accrual_end IS NOT NULL
          AND accrual_start <= :endEpochDay AND accrual_end >= :startEpochDay
        """,
    )
    fun observeAccruedBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: Long): ExpenseEntity?

    /**
     * Maior leitura de odômetro já registrada para um veículo.
     *
     * `MAX`, e não "a leitura do lançamento mais recente": odômetro só cresce,
     * e o motorista pode lançar despesas fora de ordem — cadastrar hoje o
     * abastecimento da semana passada é comum. Ordenar por data devolveria uma
     * leitura menor que a real nesse caso.
     *
     * Devolve `null` quando o veículo ainda não tem nenhum lançamento com
     * leitura — o que inclui todo o histórico anterior à v0.6.0.
     */
    @Query("SELECT MAX(odometer_km) FROM expenses WHERE vehicle_id = :vehicleId")
    fun observeLatestOdometer(vehicleId: Long): Flow<Long?>

    /**
     * Última leitura de cada veículo, de uma vez.
     *
     * Uma consulta agrupada em vez de uma por veículo: a lista de veículos
     * precisa de todas ao mesmo tempo, e abrir um `Flow` por linha da lista
     * multiplicaria observadores do banco sem necessidade.
     */
    @Query(
        """
        SELECT `vehicle_id` AS vehicleId, MAX(`odometer_km`) AS odometerKm
        FROM expenses
        WHERE `vehicle_id` IS NOT NULL AND `odometer_km` IS NOT NULL
        GROUP BY `vehicle_id`
        """,
    )
    fun observeOdometers(): Flow<List<VehicleOdometer>>

    /**
     * Última leitura **anterior** ao período — a linha de partida da
     * conciliação.
     *
     * Sem ela, a distância percorrida no período começaria na primeira leitura
     * de dentro dele, e tudo o que o motorista rodou entre a última leitura
     * anterior e essa primeira ficaria de fora.
     */
    @Query(
        """
        SELECT MAX(odometer_km) FROM expenses
        WHERE vehicle_id = :vehicleId AND date < :startEpochDay
        """,
    )
    suspend fun odometerBefore(vehicleId: Long, startEpochDay: Long): Long?

    /** Menor e maior leitura dentro do período. */
    @Query(
        """
        SELECT MIN(odometer_km) FROM expenses
        WHERE vehicle_id = :vehicleId AND date BETWEEN :startEpochDay AND :endEpochDay
        """,
    )
    suspend fun minOdometerIn(vehicleId: Long, startEpochDay: Long, endEpochDay: Long): Long?

    @Query(
        """
        SELECT MAX(odometer_km) FROM expenses
        WHERE vehicle_id = :vehicleId AND date BETWEEN :startEpochDay AND :endEpochDay
        """,
    )
    suspend fun maxOdometerIn(vehicleId: Long, startEpochDay: Long, endEpochDay: Long): Long?

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
