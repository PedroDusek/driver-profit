package com.driverprofit.core.navigation

import com.driverprofit.domain.model.Expense
import com.driverprofit.domain.model.PersonalUsage
import com.driverprofit.domain.model.Vehicle
import com.driverprofit.domain.model.WorkSession

/**
 * Destinos de navegação.
 *
 * Rotas ficam concentradas aqui para evitar strings mágicas espalhadas pelos
 * Composables (PRD §54). Cada nova tela adiciona uma constante nesta lista.
 *
 * Formulários usam uma rota só para cadastro e edição, com o id como
 * argumento opcional: a tela é a mesma, muda apenas se ela começa preenchida.
 */
object DriverProfitDestination {

    /** Tela principal — dashboard de rentabilidade (PRD §20). */
    const val DASHBOARD = "dashboard"

    // --- Veículos ---

    const val VEHICLE_LIST = "vehicle_list"

    const val ARG_VEHICLE_ID = "vehicleId"

    const val VEHICLE_FORM = "vehicle_form?$ARG_VEHICLE_ID={$ARG_VEHICLE_ID}"

    fun vehicleForm(vehicleId: Long = Vehicle.UNSAVED_ID): String =
        "vehicle_form?$ARG_VEHICLE_ID=$vehicleId"

    // --- Ganhos ---

    /** Histórico de sessões de trabalho (PRD §19). */
    const val EARNINGS_LIST = "earnings_list"

    const val ARG_SESSION_ID = "sessionId"

    const val EARNINGS_FORM = "earnings_form?$ARG_SESSION_ID={$ARG_SESSION_ID}"

    fun earningsForm(sessionId: Long = WorkSession.UNSAVED_ID): String =
        "earnings_form?$ARG_SESSION_ID=$sessionId"

    // --- Despesas ---

    /** Histórico de despesas (PRD §19). */
    const val EXPENSES_LIST = "expenses_list"

    const val ARG_EXPENSE_ID = "expenseId"

    const val EXPENSE_FORM = "expense_form?$ARG_EXPENSE_ID={$ARG_EXPENSE_ID}"

    fun expenseForm(expenseId: Long = Expense.UNSAVED_ID): String =
        "expense_form?$ARG_EXPENSE_ID=$expenseId"

    // --- Uso pessoal ---

    /** Quilometragem rodada fora do trabalho (PRD §22). */
    const val PERSONAL_USAGE_LIST = "personal_usage_list"

    const val ARG_PERSONAL_USAGE_ID = "personalUsageId"

    const val PERSONAL_USAGE_FORM =
        "personal_usage_form?$ARG_PERSONAL_USAGE_ID={$ARG_PERSONAL_USAGE_ID}"

    fun personalUsageForm(usageId: Long = PersonalUsage.UNSAVED_ID): String =
        "personal_usage_form?$ARG_PERSONAL_USAGE_ID=$usageId"

    /** Destino inicial do grafo de navegação. */
    const val START = DASHBOARD
}
