package com.driverpro.core.ui.format

import androidx.annotation.StringRes
import com.driverpro.R
import com.driverpro.domain.model.DashboardPeriod

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
}
