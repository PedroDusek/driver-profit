package com.driverprofit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverprofit.core.common.Money
import com.driverprofit.core.common.WorkDuration
import com.driverprofit.domain.model.Platform
import com.driverprofit.domain.model.WorkSession
import java.time.Instant
import java.time.LocalDate

/**
 * Sessão de trabalho no banco. Não sai da camada de dados: use [toDomain] /
 * [toEntity] nas fronteiras.
 *
 * O índice em `date` existe porque toda consulta do dashboard filtra por
 * período (PRD §20) — e `date` é epoch day, então a comparação é numérica.
 *
 * As unidades estão nos nomes das colunas de propósito: `revenue_cents`,
 * `online_minutes`, `distance_km`. Ler o schema não pode deixar dúvida sobre
 * o que está guardado ali.
 */
@Entity(
    tableName = "work_sessions",
    indices = [Index(value = ["date"])],
)
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

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
    date = date,
    platform = platform,
    rides = rides,
    revenueCents = revenue.cents,
    onlineMinutes = onlineTime.minutes,
    distanceKm = distanceKm,
    note = note,
    createdAt = createdAt,
)
