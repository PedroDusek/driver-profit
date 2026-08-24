package com.driverpro.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.driverpro.core.ui.theme.TabularFigures

/**
 * Cartão compacto para grades de indicadores (2 colunas no Dashboard).
 *
 * Substitui as linhas de `MetricRow` empilhadas por algo escaneável de
 * relance — o padrão de "stat tile" comum em app de finanças/dashboard, em
 * vez de uma lista de pares rótulo/valor que exige ler de cima a baixo.
 */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    DriverProCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TabularFigures),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
