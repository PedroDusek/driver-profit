package com.driverpro.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverpro.core.domain.DateRange
import com.driverpro.domain.model.ReconciliationDismissal
import java.time.Instant
import java.time.LocalDate

/**
 * Sobra de odômetro que o motorista aceitou deixar de fora (PRD §22).
 *
 * `ON DELETE CASCADE`, como `maintenance_schedules` e ao contrário de
 * `expenses`: isto não é histórico financeiro, é uma decisão sobre um intervalo
 * de um carro. Sem o carro, ela não significa nada.
 *
 * Índice único sobre `(vehicle_id, start_date, end_date)`: uma janela tem uma
 * decisão, e decidir de novo substitui a anterior.
 */
@Entity(
    tableName = "reconciliation_dismissals",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["vehicle_id", "start_date", "end_date"], unique = true)],
)
data class ReconciliationDismissalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long,

    /** Epoch day do primeiro dia da janela conciliada. */
    @ColumnInfo(name = "start_date")
    val startDate: LocalDate,

    /** Epoch day do último dia, inclusive. */
    @ColumnInfo(name = "end_date")
    val endDate: LocalDate,

    /**
     * Quantos quilômetros foram aceitos fora da conta.
     *
     * Guardado, e não deduzido: é o que permite a dispensa caducar quando a
     * sobra muda.
     */
    @ColumnInfo(name = "dismissed_km")
    val dismissedKm: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun ReconciliationDismissalEntity.toDomain(): ReconciliationDismissal = ReconciliationDismissal(
    id = id,
    vehicleId = vehicleId,
    window = DateRange(startDate, endDate),
    dismissedKm = dismissedKm,
    createdAt = createdAt,
)

fun ReconciliationDismissal.toEntity(): ReconciliationDismissalEntity =
    ReconciliationDismissalEntity(
        id = id,
        vehicleId = vehicleId,
        startDate = window.start,
        endDate = window.end,
        dismissedKm = dismissedKm,
        createdAt = createdAt,
    )
