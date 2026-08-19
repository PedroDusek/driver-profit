package com.driverpro.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Uma fatia do [DonutChart]: fração do total (0f..1f) e cor. */
data class DonutSlice(val fraction: Float, val color: Color)

/**
 * Gráfico de rosca (v0.14.1) — primeira visualização de dado do app além de
 * texto/barra, seguindo a referência visual em `IMAGENS/`. Usado no
 * breakdown de despesas por categoria, no Dashboard e em `ExpensesListScreen`.
 *
 * Desenhado com `Canvas` puro: sem biblioteca de gráfico nova (PRD §55), e as
 * fatias já chegam prontas de [DonutSlice] — a tela não faz conta financeira,
 * só decide fração e cor a partir do que o domínio já calculou.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    centerContent: @Composable () -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.fraction.coerceIn(0f, 1f)
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        centerContent()
    }
}
