package com.driverprofit.domain.model

/**
 * Tipo de propulsão do veículo.
 *
 * Propositalmente separado de [CombustionFuel] e [ChargingCapability] (PRD §13):
 * um híbrido flex plug-in é a combinação `HYBRID + FLEX + PLUG_IN`, e não um
 * valor único de enum. Isso mantém combinações futuras representáveis sem
 * migração de schema.
 */
enum class VehiclePowertrain {
    /** Somente motor a combustão. */
    COMBUSTION,

    /** Motor a combustão + motor elétrico. */
    HYBRID,

    /** Somente motor elétrico. */
    ELECTRIC,
    ;

    /** Indica se o veículo consome combustível líquido ou gasoso. */
    val usesCombustionFuel: Boolean
        get() = this == COMBUSTION || this == HYBRID

    /** Indica se o veículo pode receber energia elétrica externa. */
    val mayBeCharged: Boolean
        get() = this == ELECTRIC || this == HYBRID
}
