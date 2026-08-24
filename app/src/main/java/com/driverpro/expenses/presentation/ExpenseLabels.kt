package com.driverpro.expenses.presentation

import androidx.annotation.StringRes
import com.driverpro.R
import com.driverpro.expenses.domain.ChargingLocation
import com.driverpro.expenses.domain.ExpenseCategory
import com.driverpro.expenses.domain.ExpenseField
import com.driverpro.expenses.domain.ExpenseValidationError
import com.driverpro.maintenance.domain.MaintenanceCategory
import com.driverpro.core.domain.MeasurementUnit

/** Tradução dos enums de despesa para textos da interface. */
object ExpenseLabels {

    @StringRes
    fun category(value: ExpenseCategory): Int = when (value) {
        ExpenseCategory.FUEL -> R.string.expense_category_fuel
        ExpenseCategory.CHARGING -> R.string.expense_category_charging
        ExpenseCategory.MAINTENANCE -> R.string.expense_category_maintenance
        ExpenseCategory.CAR_WASH -> R.string.expense_category_car_wash
        ExpenseCategory.TOLL -> R.string.expense_category_toll
        ExpenseCategory.PARKING -> R.string.expense_category_parking
        ExpenseCategory.INSURANCE -> R.string.expense_category_insurance
        ExpenseCategory.VEHICLE_TAX -> R.string.expense_category_vehicle_tax
        ExpenseCategory.FINANCING -> R.string.expense_category_financing
        ExpenseCategory.OTHER -> R.string.expense_category_other
    }

    @StringRes
    fun maintenance(value: MaintenanceCategory): Int = when (value) {
        MaintenanceCategory.OIL -> R.string.maintenance_oil
        MaintenanceCategory.FILTERS -> R.string.maintenance_filters
        MaintenanceCategory.TIRES -> R.string.maintenance_tires
        MaintenanceCategory.BRAKES -> R.string.maintenance_brakes
        MaintenanceCategory.SUSPENSION -> R.string.maintenance_suspension
        MaintenanceCategory.BATTERY -> R.string.maintenance_battery
        MaintenanceCategory.BELT -> R.string.maintenance_belt
        MaintenanceCategory.PARTS -> R.string.maintenance_parts
        MaintenanceCategory.ELECTRICAL -> R.string.maintenance_electrical
        MaintenanceCategory.MECHANICAL -> R.string.maintenance_mechanical
        MaintenanceCategory.BODYWORK -> R.string.maintenance_bodywork
        MaintenanceCategory.INSPECTION -> R.string.maintenance_inspection
        MaintenanceCategory.OTHER -> R.string.maintenance_other
    }

    @StringRes
    fun chargingLocation(value: ChargingLocation): Int = when (value) {
        ChargingLocation.RESIDENTIAL -> R.string.charging_residential
        ChargingLocation.COMMERCIAL -> R.string.charging_commercial
        ChargingLocation.PUBLIC -> R.string.charging_public
        ChargingLocation.OTHER -> R.string.charging_other
    }

    /** Símbolo curto da unidade, usado em campos e em preço por unidade. */
    @StringRes
    fun unit(value: MeasurementUnit): Int = when (value) {
        MeasurementUnit.LITER -> R.string.unit_liter
        MeasurementUnit.CUBIC_METER -> R.string.unit_cubic_meter
        MeasurementUnit.KILOWATT_HOUR -> R.string.unit_kwh
    }

    @StringRes
    fun field(value: ExpenseField): Int = when (value) {
        ExpenseField.DATE -> R.string.expense_date
        ExpenseField.CATEGORY -> R.string.expense_category
        ExpenseField.AMOUNT -> R.string.expense_amount
        ExpenseField.VEHICLE -> R.string.expense_vehicle
        ExpenseField.ODOMETER -> R.string.expense_odometer
        ExpenseField.DESCRIPTION -> R.string.expense_description
        ExpenseField.FUEL_TYPE -> R.string.expense_fuel_type
        ExpenseField.QUANTITY -> R.string.expense_quantity
        ExpenseField.STATION -> R.string.expense_station
        ExpenseField.CHARGING_LOCATION -> R.string.expense_charging_location
        ExpenseField.PLACE -> R.string.expense_place
        ExpenseField.MAINTENANCE_CATEGORY -> R.string.expense_maintenance_category
        ExpenseField.WORKSHOP -> R.string.expense_workshop
        ExpenseField.ACCRUAL -> R.string.expense_accrual
    }

    @StringRes
    fun error(value: ExpenseValidationError): Int = when (value) {
        ExpenseValidationError.REQUIRED -> R.string.error_required
        ExpenseValidationError.DATE_IN_FUTURE -> R.string.error_date_in_future
        ExpenseValidationError.NEGATIVE -> R.string.error_negative
        ExpenseValidationError.QUANTITY_ZERO -> R.string.error_quantity_zero
        ExpenseValidationError.FUEL_NOT_SUPPORTED_BY_VEHICLE ->
            R.string.error_fuel_not_supported
        ExpenseValidationError.VEHICLE_CANNOT_CHARGE -> R.string.error_vehicle_cannot_charge
        ExpenseValidationError.ODOMETER_OUT_OF_RANGE -> R.string.error_odometer_out_of_range
        ExpenseValidationError.ODOMETER_INCONSISTENT -> R.string.error_odometer_inconsistent
        ExpenseValidationError.ACCRUAL_INCOMPLETE -> R.string.error_accrual_incomplete
        ExpenseValidationError.ACCRUAL_NOT_ALLOWED -> R.string.error_accrual_not_allowed
        ExpenseValidationError.TEXT_TOO_LONG -> R.string.error_text_too_long
    }
}
