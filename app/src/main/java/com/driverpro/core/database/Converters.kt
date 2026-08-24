package com.driverpro.core.database

import androidx.room.TypeConverter
import com.driverpro.expenses.domain.ChargingLocation
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.core.domain.FuelType
import com.driverpro.expenses.domain.MaintenanceCategory
import com.driverpro.domain.model.MaintenanceItem
import com.driverpro.domain.model.PersonalUsageSource
import com.driverpro.earnings.domain.Platform
import com.driverpro.vehicle.domain.VehicleFuel
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

    @TypeConverter
    fun platformToName(value: Platform?): String? = value?.name

    @TypeConverter
    fun nameToPlatform(value: String?): Platform? = value?.let(Platform::valueOf)

    @TypeConverter
    fun expenseCategoryToName(value: ExpenseCategory?): String? = value?.name

    @TypeConverter
    fun nameToExpenseCategory(value: String?): ExpenseCategory? =
        value?.let(ExpenseCategory::valueOf)

    @TypeConverter
    fun fuelTypeToName(value: FuelType?): String? = value?.name

    @TypeConverter
    fun nameToFuelType(value: String?): FuelType? = value?.let(FuelType::valueOf)

    @TypeConverter
    fun chargingLocationToName(value: ChargingLocation?): String? = value?.name

    @TypeConverter
    fun nameToChargingLocation(value: String?): ChargingLocation? =
        value?.let(ChargingLocation::valueOf)

    @TypeConverter
    fun maintenanceCategoryToName(value: MaintenanceCategory?): String? = value?.name

    @TypeConverter
    fun nameToMaintenanceCategory(value: String?): MaintenanceCategory? =
        value?.let(MaintenanceCategory::valueOf)

    @TypeConverter
    fun maintenanceItemToName(value: MaintenanceItem?): String? = value?.name

    @TypeConverter
    fun nameToMaintenanceItem(value: String?): MaintenanceItem? =
        value?.let(MaintenanceItem::valueOf)

    @TypeConverter
    fun personalUsageSourceToName(value: PersonalUsageSource?): String? = value?.name

    @TypeConverter
    fun nameToPersonalUsageSource(value: String?): PersonalUsageSource? =
        value?.let(PersonalUsageSource::valueOf)
}
