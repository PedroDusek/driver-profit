package com.driverprofit.core.navigation

import com.driverprofit.domain.model.Vehicle

/**
 * Destinos de navegação.
 *
 * Rotas ficam concentradas aqui para evitar strings mágicas espalhadas pelos
 * Composables (PRD §54). Cada nova tela adiciona uma constante nesta lista.
 */
object DriverProfitDestination {

    /** Tela principal — dashboard de rentabilidade (PRD §20). */
    const val DASHBOARD = "dashboard"

    /** Lista de veículos cadastrados. */
    const val VEHICLE_LIST = "vehicle_list"

    const val ARG_VEHICLE_ID = "vehicleId"

    /**
     * Formulário de veículo, usado tanto para cadastro quanto para edição.
     *
     * O id vem como argumento opcional: ausente (ou [Vehicle.UNSAVED_ID])
     * significa cadastro novo. Uma rota só, em vez de duas, porque a tela é a
     * mesma — muda apenas se ela começa preenchida.
     */
    const val VEHICLE_FORM = "vehicle_form?$ARG_VEHICLE_ID={$ARG_VEHICLE_ID}"

    fun vehicleForm(vehicleId: Long = Vehicle.UNSAVED_ID): String =
        "vehicle_form?$ARG_VEHICLE_ID=$vehicleId"

    /** Destino inicial do grafo de navegação. */
    const val START = DASHBOARD
}
