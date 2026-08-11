package com.driverprofit.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.VehiclePowertrain
import java.time.Instant

/**
 * Representação do veículo no banco. Não sai da camada de dados: use
 * [toDomain] / [toEntity] nas fronteiras.
 *
 * `combustion_fuel` e `charging_capability` são anuláveis porque não se
 * aplicam a todos os veículos (PRD §14) — um elétrico puro não tem
 * combustível, um híbrido convencional não recebe carga externa.
 */
@Entity(tableName = "vehicles")
data class VehicleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "brand")
    val brand: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "year")
    val year: Int,

    /** Quilometragem no cadastro. Inteiro: quilômetros, nunca metros. */
    @ColumnInfo(name = "initial_odometer_km")
    val initialOdometerKm: Long,

    @ColumnInfo(name = "powertrain")
    val powertrain: VehiclePowertrain,

    @ColumnInfo(name = "combustion_fuel")
    val combustionFuel: CombustionFuel?,

    @ColumnInfo(name = "charging_capability")
    val chargingCapability: ChargingCapability?,

    /** Epoch millis em UTC. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)

fun VehicleEntity.toDomain(): Vehicle = Vehicle(
    id = id,
    brand = brand,
    model = model,
    year = year,
    initialOdometerKm = initialOdometerKm,
    powertrain = powertrain,
    combustionFuel = combustionFuel,
    chargingCapability = chargingCapability,
    createdAt = createdAt,
)

fun Vehicle.toEntity(): VehicleEntity = VehicleEntity(
    id = id,
    brand = brand,
    model = model,
    year = year,
    initialOdometerKm = initialOdometerKm,
    powertrain = powertrain,
    combustionFuel = combustionFuel,
    chargingCapability = chargingCapability,
    createdAt = createdAt,
)
