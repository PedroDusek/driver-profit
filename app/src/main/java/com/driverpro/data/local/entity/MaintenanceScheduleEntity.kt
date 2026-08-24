package com.driverpro.data.local.entity

import com.driverpro.vehicle.data.VehicleEntity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.MaintenanceSchedule
import java.time.Instant

/**
 * Intervalo de manutenção definido pelo motorista (ROADMAP v0.9.0).
 *
 * **Só existe linha para o que ele mudou.** Item sem registro usa o intervalo
 * padrão do enum, o que faz um veículo recém-cadastrado já nascer acompanhado
 * sem ritual de configuração — e mantém a tabela com meia dúzia de linhas em
 * vez de cinco por veículo.
 *
 * `ON DELETE CASCADE`, ao contrário de `expenses` e `personal_usage`, que usam
 * `SET NULL`: aquelas guardam histórico financeiro, que precisa sobreviver à
 * troca de carro. Esta guarda uma preferência sobre um carro específico, e sem
 * o carro ela não significa nada.
 */
@Entity(
    tableName = "maintenance_schedules",
    foreignKeys = [
        ForeignKey(
            entity = VehicleEntity::class,
            parentColumns = ["id"],
            childColumns = ["vehicle_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        // Único: um item não pode ter dois intervalos no mesmo veículo. Como
        // começa por vehicle_id, também é o índice que o Room exige para a
        // chave estrangeira — não são necessários dois.
        Index(value = ["vehicle_id", "item"], unique = true),
    ],
)
data class MaintenanceScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "vehicle_id")
    val vehicleId: Long,

    /** `OIL` \| `FILTERS` \| `BRAKES` \| `TIRES` \| `INSPECTION`, gravado por `name`. */
    @ColumnInfo(name = "item")
    val item: MaintenanceItem,

    @ColumnInfo(name = "interval_km")
    val intervalKm: Long,

    /** Falso quando o motorista desligou o acompanhamento deste item. */
    @ColumnInfo(name = "monitored")
    val monitored: Boolean,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun MaintenanceScheduleEntity.toDomain(): MaintenanceSchedule = MaintenanceSchedule(
    id = id,
    vehicleId = vehicleId,
    item = item,
    intervalKm = intervalKm,
    monitored = monitored,
    createdAt = createdAt,
)

fun MaintenanceSchedule.toEntity(): MaintenanceScheduleEntity = MaintenanceScheduleEntity(
    id = id,
    vehicleId = vehicleId,
    item = item,
    intervalKm = intervalKm,
    monitored = monitored,
    createdAt = createdAt,
)
