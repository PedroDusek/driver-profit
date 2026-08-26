package com.driverpro.dashboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.driverpro.R
import com.driverpro.core.ui.component.TrendBadge
import com.driverpro.dashboard.domain.DashboardPeriod
import com.driverpro.dashboard.domain.MetricChange

/**
 * Desenha a variação de um indicador contra o período anterior.
 *
 * Traduz [MetricChange] — que é domínio, e não conhece recurso de string — no
 * texto e nas cores do [TrendBadge]. É aqui que os três casos viram frases
 * diferentes:
 *
 * - **Sem base anterior**: "sem dados no período anterior". Não é o mesmo que
 *   variação zero, e dizer "0%" ali seria afirmar algo que não se sabe.
 * - **Base zero**: houve período anterior, mas o indicador valia zero lá —
 *   variação percentual sobre zero não existe.
 * - **Medida**: a seta e a porcentagem, coloridas por `better` e **não** pela
 *   direção, para custo que sobe nunca aparecer em verde.
 */
@Composable
fun MetricTrend(change: MetricChange, period: DashboardPeriod) {
    val against = stringResource(DashboardLabels.comparison(period))

    when (change) {
        MetricChange.NoBaseline -> TrendBadge(
            text = stringResource(R.string.dashboard_trend_no_baseline),
        )

        MetricChange.NoBase -> TrendBadge(
            text = stringResource(R.string.dashboard_trend_no_base),
        )

        is MetricChange.Measured -> if (change.isFlat) {
            TrendBadge(text = stringResource(R.string.dashboard_trend_flat, against))
        } else {
            TrendBadge(
                text = stringResource(R.string.dashboard_trend_change, change.percent, against),
                rising = change.rising,
                better = change.better,
            )
        }
    }
}
