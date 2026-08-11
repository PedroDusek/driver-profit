package com.driverprofit.data.local.database

import androidx.room.TypeConverter
import com.driverprofit.domain.model.VehicleFuel
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
    fun vehicleFuelToName(value: VehicleFuel?): String? = value?.name

    @TypeConverter
    fun nameToVehicleFuel(value: String?): VehicleFuel? = value?.let(VehicleFuel::valueOf)
}
