package com.driverprofit.core.ui.format

import androidx.annotation.StringRes
import com.driverprofit.R
import com.driverprofit.domain.model.ChargingCapability
import com.driverprofit.domain.model.CombustionFuel
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehiclePowertrain
import com.driverprofit.domain.model.VehicleValidationError

/**
 * Tradução dos enums de domínio para textos da interface.
 *
 * Fica na camada de apresentação de propósito: o domínio devolve o *motivo* de
 * um erro, não a frase exibida. Assim ele continua sem depender de `Context` e
 * testável na JVM, e o texto pode mudar sem tocar em regra de negócio.
 */
object VehicleLabels {

    @StringRes
    fun powertrain(value: VehiclePowertrain): Int = when (value) {
        VehiclePowertrain.COMBUSTION -> R.string.powertrain_combustion
        VehiclePowertrain.HYBRID -> R.string.powertrain_hybrid
        VehiclePowertrain.ELECTRIC -> R.string.powertrain_electric
    }

    @StringRes
    fun combustionFuel(value: CombustionFuel): Int = when (value) {
        CombustionFuel.GASOLINE -> R.string.fuel_gasoline
        CombustionFuel.ETHANOL -> R.string.fuel_ethanol
        CombustionFuel.FLEX -> R.string.fuel_flex
        CombustionFuel.CNG -> R.string.fuel_cng
        CombustionFuel.FLEX_CNG -> R.string.fuel_flex_cng
    }

    @StringRes
    fun chargingCapability(value: ChargingCapability): Int = when (value) {
        ChargingCapability.NONE -> R.string.charging_none
        ChargingCapability.PLUG_IN -> R.string.charging_plug_in
        ChargingCapability.UNKNOWN -> R.string.charging_unknown
    }

    @StringRes
    fun field(value: VehicleField): Int = when (value) {
        VehicleField.BRAND -> R.string.vehicle_brand
        VehicleField.MODEL -> R.string.vehicle_model
        VehicleField.YEAR -> R.string.vehicle_year
        VehicleField.INITIAL_ODOMETER -> R.string.vehicle_initial_odometer
        VehicleField.POWERTRAIN -> R.string.vehicle_powertrain
        VehicleField.COMBUSTION_FUEL -> R.string.vehicle_combustion_fuel
        VehicleField.CHARGING_CAPABILITY -> R.string.vehicle_charging_capability
    }

    @StringRes
    fun error(value: VehicleValidationError): Int = when (value) {
        VehicleValidationError.REQUIRED -> R.string.error_required
        VehicleValidationError.YEAR_OUT_OF_RANGE -> R.string.error_year_out_of_range
        VehicleValidationError.NEGATIVE_ODOMETER -> R.string.error_negative_odometer
        VehicleValidationError.ODOMETER_TOO_HIGH -> R.string.error_odometer_too_high
        VehicleValidationError.NOT_APPLICABLE -> R.string.error_not_applicable
    }
}
