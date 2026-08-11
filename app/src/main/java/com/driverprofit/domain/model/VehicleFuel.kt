package com.driverprofit.domain.model

/**
 * Como o veículo é alimentado.
 *
 * Lista plana e única, e não três eixos separados: o cadastro precisa apenas
 * do suficiente para calcular custo de abastecimento, e a informação que
 * importa para essa conta é **quais insumos** o carro aceita e **em que
 * unidade** cada um é medido.
 *
 * [FLEX] e [HYBRID] não são insumos — são capacidades. No momento do
 * abastecimento o motorista escolhe entre as opções de [refuelOptions], e o
 * lançamento guarda qual foi usada, mantendo histórico separado por insumo
 * (PRD §9).
 */
enum class VehicleFuel(val refuelOptions: List<FuelType>) {
    GASOLINE(listOf(FuelType.GASOLINE)),
    ETHANOL(listOf(FuelType.ETHANOL)),
    FLEX(listOf(FuelType.GASOLINE, FuelType.ETHANOL)),
    CNG(listOf(FuelType.CNG)),

    /** Flex de fábrica com kit GNV instalado. */
    FLEX_CNG(listOf(FuelType.GASOLINE, FuelType.ETHANOL, FuelType.CNG)),

    ELECTRIC(listOf(FuelType.ELECTRICITY)),

    /**
     * Combustão + elétrico.
     *
     * Inclui gasolina e etanol porque os híbridos vendidos no Brasil costumam
     * ser flex. Se o carro não for plug-in, o motorista simplesmente nunca
     * registra um carregamento — não vale um campo a mais no cadastro para
     * capturar isso.
     */
    HYBRID(listOf(FuelType.GASOLINE, FuelType.ETHANOL, FuelType.ELECTRICITY)),
    ;

    /** Indica se faz sentido oferecer registro de carregamento elétrico. */
    val supportsCharging: Boolean
        get() = FuelType.ELECTRICITY in refuelOptions
}

/**
 * Insumo efetivamente colocado no veículo em um abastecimento ou recarga.
 *
 * A unidade de medida faz parte do tipo para impedir, em tempo de compilação,
 * que GNV seja tratado em litros ou energia em litros (PRD §10, §27).
 */
enum class FuelType(val unit: MeasurementUnit) {
    GASOLINE(MeasurementUnit.LITER),
    ETHANOL(MeasurementUnit.LITER),
    CNG(MeasurementUnit.CUBIC_METER),
    ELECTRICITY(MeasurementUnit.KILOWATT_HOUR),
}

/** Unidades de medida usadas para quantificar combustível e energia. */
enum class MeasurementUnit {
    LITER,
    CUBIC_METER,
    KILOWATT_HOUR,
}
