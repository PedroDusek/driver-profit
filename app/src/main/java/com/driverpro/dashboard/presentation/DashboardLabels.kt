package com.driverpro.dashboard.presentation

import androidx.annotation.StringRes
import com.driverpro.R
import com.driverpro.dashboard.domain.DashboardPeriod

/** Tradução dos períodos do dashboard para textos da interface. */
object DashboardLabels {

    @StringRes
    fun period(value: DashboardPeriod): Int = when (value) {
        DashboardPeriod.Today -> R.string.dashboard_period_today
        DashboardPeriod.Yesterday -> R.string.dashboard_period_yesterday
        DashboardPeriod.ThisWeek -> R.string.dashboard_period_this_week
        DashboardPeriod.ThisMonth -> R.string.dashboard_period_this_month
        DashboardPeriod.LastMonth -> R.string.dashboard_period_last_month
        is DashboardPeriod.Custom -> R.string.dashboard_period_custom
    }

    /**
     * Contra o que a variação está sendo medida — "vs ontem", "vs semana
     * passada".
     *
     * Nomear a base importa: sem isso, uma seta verde de 12% não diz se o
     * ganho foi contra ontem ou contra o mês passado, e o motorista não tem
     * como julgar se o número é bom.
     */
    @StringRes
    fun comparison(value: DashboardPeriod): Int = when (value) {
        DashboardPeriod.Today -> R.string.dashboard_compare_yesterday
        DashboardPeriod.Yesterday -> R.string.dashboard_compare_day_before
        DashboardPeriod.ThisWeek -> R.string.dashboard_compare_last_week
        DashboardPeriod.ThisMonth -> R.string.dashboard_compare_last_month
        // "Mês anterior" comparado com o mês antes dele: dizer "vs mês
        // anterior" seria ambíguo, porque o próprio período já se chama assim.
        DashboardPeriod.LastMonth -> R.string.dashboard_compare_previous_period
        is DashboardPeriod.Custom -> R.string.dashboard_compare_previous_period
    }
}
