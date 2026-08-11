package com.driverprofit.domain.model

/**
 * Combustível que o motor a combustão do veículo aceita.
 *
 * `null` no veículo significa "não se aplica" — o caso de um elétrico puro.
 *
 * [FLEX] é uma *capacidade* do veículo, não um combustível abastecível: no
 * momento do abastecimento o motorista escolhe entre gasolina e etanol
 * (PRD §9), e o registro guarda qual foi usado.
 */
enum class CombustionFuel(val refuelOptions: List<FuelType>) {
    GASOLINE(listOf(FuelType.GASOLINE)),
    ETHANOL(listOf(FuelType.ETHANOL)),
    FLEX(listOf(FuelType.GASOLINE, FuelType.ETHANOL)),
    CNG(listOf(FuelType.CNG)),

    /** Flex de fábrica com kit GNV instalado. */
    FLEX_CNG(listOf(FuelType.GASOLINE, FuelType.ETHANOL, FuelType.CNG)),
}

/**
 * Combustível efetivamente colocado no veículo em um abastecimento.
 *
 * A unidade de medida faz parte do tipo para impedir que GNV seja tratado em
 * litros ou energia em litros (PRD §10, §27).
 */
enum class FuelType(val unit: MeasurementUnit) {
    GASOLINE(MeasurementUnit.LITER),
    ETHANOL(MeasurementUnit.LITER),
    CNG(MeasurementUnit.CUBIC_METER),
}

/** Unidades de medida usadas para quantificar energia/combustível. */
enum class MeasurementUnit {
    LITER,
    CUBIC_METER,
    KILOWATT_HOUR,
}
