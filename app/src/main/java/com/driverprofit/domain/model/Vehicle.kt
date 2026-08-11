package com.driverprofit.domain.model

import java.time.Instant

/**
 * Veículo do motorista.
 *
 * O cadastro guarda apenas o mínimo necessário: um [name] para o motorista
 * reconhecer qual carro é, e o [fuel], que determina a unidade de medida do
 * abastecimento e portanto o cálculo de custo.
 *
 * Marca, modelo e ano foram deliberadamente deixados de fora — nenhum deles
 * entra em qualquer conta de rentabilidade, e cada campo a mais é uma barreira
 * entre o motorista e o primeiro lançamento.
 *
 * Quilometragem também não vive aqui: ela será registrada por lançamento,
 * servindo a controles de manutenção (troca de óleo, pneus), e não como
 * atributo do veículo.
 */
data class Vehicle(
    val id: Long = UNSAVED_ID,
    val name: String,
    val fuel: VehicleFuel,
    val createdAt: Instant,
) {
    /** Insumos que o formulário de abastecimento deve oferecer (PRD §7). */
    val refuelOptions: List<FuelType> get() = fuel.refuelOptions

    /** Indica se o formulário de carregamento elétrico deve ser exibido. */
    val supportsChargingRecords: Boolean get() = fuel.supportsCharging

    companion object {
        /** Id atribuído a um veículo que ainda não foi persistido. */
        const val UNSAVED_ID: Long = 0L

        /** Acima disso o nome vira ilegível na lista. */
        const val MAX_NAME_LENGTH: Int = 40
    }
}
