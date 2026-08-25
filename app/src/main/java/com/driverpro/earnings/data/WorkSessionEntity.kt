package com.driverpro.earnings.data

import com.driverpro.vehicle.data.VehicleEntity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverpro.core.domain.Money
import com.driverpro.core.domain.WorkDuration
import com.driverpro.earnings.domain.Platform
import com.driverpro.earnings.domain.WorkSession
import java.time.Instant
import java.time.LocalDate

/**
 * Sessão de trabalho no banco. Não sai da camada de dados: use [toDomain] /
 * [toEntity] nas fronteiras.
 *
 * O índice em `date` existe porque toda consulta do dashboard filtra por
 * período (PRD §20) — e `date` é epoch day, então a comparação é numérica.
 * O índice em `vehicle_id` é exigido pelo Room para a chave estrangeira, como
 * em `ExpenseEntity`.
 *
 * `ON DELETE SET NULL` no veículo — excluir um carro não pode apagar o
 * histórico de ganhos; a sessão fica órfã e continua somando.
 *
 * As unidades estão nos nomes das colunas de propósito: `revenue_cents`,
 * `online_minutes`, `distance_km`. Ler o schema não pode deixar dúvida sobre
 * o que está guardado ali.
 */
@Entity(
    tableName = "work_sessions",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["date"]), Index(value = ["vehicle_id"])],
)
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Veículo atual no momento do lançamento (v0.12.0). Gravado automaticamente. */
    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long? = null,

    /** Epoch day. */
    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "platform")
    val platform: Platform,

    @ColumnInfo(name = "rides")
    val rides: Int,

    /** Centavos — nunca reais em ponto flutuante (PRD §26). */
    @ColumnInfo(name = "revenue_cents")
    val revenueCents: Long,

    @ColumnInfo(name = "online_minutes")
    val onlineMinutes: Long,

    @ColumnInfo(name = "distance_km")
    val distanceKm: Long,

    @ColumnInfo(name = "note")
    val note: String,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun WorkSessionEntity.toDomain(): WorkSession = WorkSession(
    id = id,
    vehicleId = vehicleId,
    date = date,
    platform = platform,
    rides = rides,
    revenue = Money(revenueCents),
    onlineTime = WorkDuration(onlineMinutes),
    distanceKm = distanceKm,
    note = note,
    createdAt = createdAt,
)

fun WorkSession.toEntity(): WorkSessionEntity = WorkSessionEntity(
    id = id,
    vehicleId = vehicleId,
    date = date,
    platform = platform,
    rides = rides,
    revenueCents = revenue.cents,
    onlineMinutes = onlineTime.minutes,
    distanceKm = distanceKm,
    note = note,
    createdAt = createdAt,
)
