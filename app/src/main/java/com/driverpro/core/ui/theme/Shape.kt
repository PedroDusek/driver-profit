package com.driverpro.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Raio de canto maior que o padrão M3 (~12dp) nos cards e botões — a
 * assinatura visual "app moderno" que separa o DriverPro do Material 3 "de
 * fábrica" que o app tinha antes da v0.14.0.
 */
internal val DriverProShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
