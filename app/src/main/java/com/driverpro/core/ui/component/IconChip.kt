package com.driverpro.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Círculo tonal colorido com ícone — leading de toda linha de lista
 * ([ListItemCard]) desde a v0.14.0.
 *
 * O fundo é derivado de [tint] compondo-o a baixa opacidade sobre a superfície
 * atual, em vez de um par de cores fixas por tema: assim a mesma cor de
 * categoria funciona em claro e escuro sem precisar de dois tons cadastrados
 * à mão para cada uma (ver `CategoryAccentColors`).
 */
@Composable
fun IconChip(
    icon: ImageVector,
    tint: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val container = tint.copy(alpha = 0.16f).compositeOver(MaterialTheme.colorScheme.surfaceContainerHigh)
    Box(
        modifier = modifier
            .size(40.dp)
            .background(color = container, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}
