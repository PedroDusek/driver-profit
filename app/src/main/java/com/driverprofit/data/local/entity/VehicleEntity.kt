package com.driverprofit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehicleFuel
import java.time.Instant

/**
 * Representação do veículo no banco. Não sai da camada de dados: use
 * [toDomain] / [toEntity] nas fronteiras.
 *
 * Schema versão 2 — marca, modelo, ano e odômetro inicial foram removidos na
 * migração 1→2. Ver `docs/DATABASE.md`.
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Como o motorista chama o carro: "Onix branco", "carro do trabalho". */
    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "fuel")
    val fuel: VehicleFuel,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun VehicleEntity.toDomain(): Vehicle = Vehicle(
    id = id,
    name = name,
    fuel = fuel,
    createdAt = createdAt,
)

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    name = name,
    fuel = fuel,
    createdAt = createdAt,
)
