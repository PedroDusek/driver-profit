package com.driverprofit.data.local.database

import androidx.room.TypeConverter
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.VehiclePowertrain
import java.time.Instant
import java.time.LocalDate

/**
 * Conversores de tipo do Room.
 *
 * Enums são persistidos pelo `name` e não pelo `ordinal`: reordenar as
 * constantes do enum não pode corromper dados já gravados.
 *
 * Datas seguem PRD §28 — nunca strings como "11/08/2026":
 *  - [LocalDate] vira epoch day (`Long`), o que mantém consultas por período
 *    como simples comparações numéricas em SQL;
 *  - [Instant] vira epoch millis (`Long`).
 */
class Converters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun powertrainToName(value: VehiclePowertrain?): String? = value?.name

    @TypeConverter
    fun nameToPowertrain(value: String?): VehiclePowertrain? =
        value?.let(VehiclePowertrain::valueOf)

    @TypeConverter
    fun combustionFuelToName(value: CombustionFuel?): String? = value?.name

    @TypeConverter
    fun nameToCombustionFuel(value: String?): CombustionFuel? =
        value?.let(CombustionFuel::valueOf)

    @TypeConverter
    fun chargingCapabilityToName(value: ChargingCapability?): String? = value?.name

    @TypeConverter
    fun nameToChargingCapability(value: String?): ChargingCapability? =
        value?.let(ChargingCapability::valueOf)
}
