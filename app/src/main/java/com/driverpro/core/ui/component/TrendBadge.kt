package com.driverpro.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.driverpro.core.ui.theme.ProfitColors
import com.driverpro.core.ui.theme.TabularFigures
import com.driverpro.core.ui.theme.content

/**
 * Variação de um indicador contra o período anterior — a seta com a
 * porcentagem que aparece embaixo de cada número.
 *
 * **A cor vem de [better], não da direção da seta.** Custo que sobe aponta para
 * cima e é vermelho; lucro que sobe aponta para cima e é verde. Quem decide o
 * que é bom é o domínio (`MetricChange.better`), justamente para a tela não ter
 * como pintar de verde um custo que aumentou — o que afirmaria o contrário do
 * que aconteceu.
 *
 * Recebe o texto pronto em vez de um tipo do domínio: `core/ui` é design
 * system, e não deve conhecer o que é lucro ou custo. Quem traduz
 * `MetricChange` para estas três coisas é a tela de dashboard.
 *
 * @param rising `null` quando não há direção a mostrar (sem base de comparação
 *   ou variação zero) — aí nenhuma seta é desenhada.
 */
@Composable
fun TrendBadge(
    text: String,
    modifier: Modifier = Modifier,
    rising: Boolean? = null,
    better: Boolean? = null,
) {
    val color = when (better) {
        true -> ProfitColors.content(positive = true)
        false -> ProfitColors.content(positive = false)
        // Sem base ou sem variação: cinza de texto secundário, não verde nem
        // vermelho. Não houve melhora nem piora a sinalizar.
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (rising != null) {
            Icon(
                imageVector = if (rising) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                // A seta repete o que a porcentagem já diz; anunciá-la de novo
                // no TalkBack só duplicaria a informação.
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = TabularFigures),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
