package com.driverpro.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Card de alerta clicável — ícone-chip colorido + título na mesma cor +
 * conteúdo (v0.14.1). Card branco com acento de cor, e não mais o card
 * inteiro pintado ([IconChip] + texto colorido, o mesmo padrão do resto do
 * app desde a revisão que seguiu `IMAGENS/`) — antes `OdometerGapCard` e
 * `MaintenanceWarningCard` preenchiam o card inteiro com
 * `tertiaryContainer`/`errorContainer`, a única coisa na tela que ainda
 * lembrava o Material 3 "de fábrica".
 */
@Composable
fun AlertCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            IconChip(icon = icon, tint = accentColor, contentDescription = null)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                )
                content()
            }
        }
    }
}
