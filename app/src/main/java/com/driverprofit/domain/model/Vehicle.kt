package com.driverprofit.domain.model

import java.time.Instant

/**
 * Veículo do motorista — modelo de domínio (PRD §14).
 *
 * Este é o tipo que circula da camada de domínio para cima. A entidade Room
 * equivalente vive em `data.local.entity` e nunca deve vazar para a UI.
 *
 * @param initialOdometerKm quilometragem no momento do cadastro. Serve de
 *   âncora para o cálculo de consumo estimado (PRD §23).
 * @param combustionFuel `null` quando o veículo não tem motor a combustão.
 * @param chargingCapability `null` quando o veículo não tem motor elétrico.
 */
data class Vehicle(
    val id: Long = UNSAVED_ID,
    val brand: String,
    val model: String,
    val year: Int,
    val initialOdometerKm: Long,
    val powertrain: VehiclePowertrain,
    val combustionFuel: CombustionFuel?,
    val chargingCapability: ChargingCapability?,
    val createdAt: Instant,
) {
    /** Combustíveis que o formulário de abastecimento deve oferecer (PRD §7). */
    val refuelOptions: List<FuelType>
        get() = combustionFuel?.refuelOptions.orEmpty()

    /** Indica se o formulário de carregamento elétrico deve ser exibido. */
    val supportsChargingRecords: Boolean
        get() = powertrain.mayBeCharged && chargingCapability?.allowsChargingRecords == true

    companion object {
        /** Id atribuído a um veículo que ainda não foi persistido. */
        const val UNSAVED_ID: Long = 0L
    }
}
