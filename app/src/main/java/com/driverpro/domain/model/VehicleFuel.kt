package com.driverpro.domain.model

import com.driverpro.core.domain.FuelType

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
