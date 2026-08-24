package com.driverpro.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A "janela" do DriverPro — o card que todas as telas usam.
 *
 * Existe porque a decisão de como um card se parece estava espalhada por nove
 * lugares, cada um repetindo `elevation = 2.dp` por hábito. Concentrá-la aqui
 * é o que faz uma mudança de estilo alcançar a tela inteira de uma vez, em
 * vez de deixar um card para trás.
 *
 * **Plano, com contorno — não elevado.** Sombra em fundo escuro é
 * praticamente invisível: o `defaultElevation` do Material 3 gastava
 * composição desenhando um degradê que ninguém enxerga. O que separa o card
 * da página aqui são duas coisas visíveis: o preenchimento mais claro
 * (`surfaceContainerLow`) e o fio de contorno (`outlineVariant`), que é como
 * a referência do Figma resolve o mesmo problema.
 */
@Composable
fun DriverProCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}
