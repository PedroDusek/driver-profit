package com.driverprofit.core.navigation

/**
 * Destinos de navegação.
 *
 * Rotas ficam concentradas aqui para evitar strings mágicas espalhadas pelos
 * Composables (PRD §54). Cada nova tela adiciona uma constante nesta lista.
 */
object DriverProfitDestination {

    /** Tela principal — dashboard de rentabilidade (PRD §20). */
    const val DASHBOARD = "dashboard"

    /** Destino inicial do grafo de navegação. */
    const val START = DASHBOARD
}
