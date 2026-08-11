package com.driverprofit.domain.model

/**
 * Capacidade de recarga elétrica do veículo (PRD §12).
 *
 * O modelo **não** presume que todo híbrido seja plug-in:
 *  - híbrido convencional → `HYBRID` + [NONE]
 *  - híbrido plug-in      → `HYBRID` + [PLUG_IN]
 *  - elétrico puro        → `ELECTRIC` + [PLUG_IN]
 *
 * [UNKNOWN] existe para quando o motorista não sabe informar; a UI deve
 * tratá-lo como "pergunte depois", nunca como plug-in.
 */
enum class ChargingCapability {
    /** Não recebe carga externa (híbrido convencional). */
    NONE,

    /** Pode ser carregado na tomada / eletroposto. */
    PLUG_IN,

    /** Ainda não informado pelo motorista. */
    UNKNOWN,
    ;

    /** Só habilita o formulário de carregamento quando há certeza. */
    val allowsChargingRecords: Boolean
        get() = this == PLUG_IN
}
