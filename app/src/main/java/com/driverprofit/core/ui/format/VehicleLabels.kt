package com.driverprofit.core.ui.format

import androidx.annotation.StringRes
import com.driverprofit.R
import com.driverprofit.domain.model.FuelType
import com.driverprofit.domain.model.VehicleField
import com.driverprofit.domain.model.VehicleFuel
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
    fun fuel(value: VehicleFuel): Int = when (value) {
        VehicleFuel.GASOLINE -> R.string.fuel_gasoline
        VehicleFuel.ETHANOL -> R.string.fuel_ethanol
        VehicleFuel.FLEX -> R.string.fuel_flex
        VehicleFuel.CNG -> R.string.fuel_cng
        VehicleFuel.FLEX_CNG -> R.string.fuel_flex_cng
        VehicleFuel.ELECTRIC -> R.string.fuel_electric
        VehicleFuel.HYBRID -> R.string.fuel_hybrid
    }

    @StringRes
    fun fuelType(value: FuelType): Int = when (value) {
        FuelType.GASOLINE -> R.string.fuel_gasoline
        FuelType.ETHANOL -> R.string.fuel_ethanol
        FuelType.CNG -> R.string.fuel_cng
        FuelType.ELECTRICITY -> R.string.fuel_electricity
    }

    @StringRes
    fun field(value: VehicleField): Int = when (value) {
        VehicleField.NAME -> R.string.vehicle_name
        VehicleField.FUEL -> R.string.vehicle_fuel
    }

    @StringRes
    fun error(value: VehicleValidationError): Int = when (value) {
        VehicleValidationError.REQUIRED -> R.string.error_required
        VehicleValidationError.NAME_TOO_LONG -> R.string.error_name_too_long
    }
}
