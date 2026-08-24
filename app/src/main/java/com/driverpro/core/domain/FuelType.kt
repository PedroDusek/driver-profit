package com.driverpro.core.domain

/**
 * Insumo efetivamente colocado no veículo em um abastecimento ou recarga.
 *
 * A unidade de medida faz parte do tipo para impedir, em tempo de compilação,
 * que GNV seja tratado em litros ou energia em litros (PRD §10, §27).
 *
 * Vocabulário compartilhado entre despesas (o que foi comprado), manutenção
 * (piso de quilometragem implícita) e veículo (capacidade de abastecimento,
 * ver `VehicleFuel`) — não é conceito de nenhuma feature sozinha.
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
