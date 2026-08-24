package com.driverpro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverpro.core.domain.DateRange
import com.driverpro.domain.model.PersonalUsage
import com.driverpro.domain.model.PersonalUsageSource
import java.time.Instant
import java.time.LocalDate

/**
 * Quilometragem rodada fora do trabalho (PRD §22).
 *
 * Tabela própria, e não uma coluna em `work_sessions`: uso pessoal não tem
 * plataforma, nem corridas, nem faturamento, e não é uma jornada. Enfiá-lo lá
 * obrigaria a deixar metade das colunas nulas e a filtrar toda consulta de
 * ganhos.
 *
 * `ON DELETE SET NULL` no veículo, como em `expenses`: trocar de carro não
 * pode apagar histórico.
 */
@Entity(
    tableName = "personal_usage",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["start_date"]),
        Index(value = ["end_date"]),
        Index(value = ["vehicle_id"]),
    ],
)
data class PersonalUsageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long? = null,

    /** Epoch day do primeiro dia do intervalo. */
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    /** Epoch day do último dia, inclusive. Igual a [startDate] num dia só. */
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,

    /** Quilômetros do intervalo inteiro, não por dia. */
    @ColumnInfo(name = "distance_km")
    val distanceKm: Long,

    /** `DECLARED` \| `RECONCILED`, gravado por `name`. */
    @ColumnInfo(name = "source")
    val source: PersonalUsageSource,

    @ColumnInfo(name = "note")
    val note: String,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun PersonalUsageEntity.toDomain(): PersonalUsage = PersonalUsage(
    id = id,
    vehicleId = vehicleId,
    range = DateRange(startDate, endDate),
    distanceKm = distanceKm,
    source = source,
    note = note,
    createdAt = createdAt,
)

fun PersonalUsage.toEntity(): PersonalUsageEntity = PersonalUsageEntity(
    id = id,
    vehicleId = vehicleId,
    startDate = range.start,
    endDate = range.end,
    distanceKm = distanceKm,
    source = source,
    note = note,
    createdAt = createdAt,
)
