package com.driverpro.core.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base. Verde = lucro, vermelho = prejuízo — os dois estados que o
// motorista precisa distinguir de relance no dashboard.
internal val Green40 = Color(0xFF2E6B4F)
internal val Green80 = Color(0xFF9BD4B4)
internal val GreenGrey40 = Color(0xFF4E635A)
internal val GreenGrey80 = Color(0xFFB6CCC0)
internal val Amber40 = Color(0xFF7A5A00)
internal val Amber80 = Color(0xFFEBC24A)

internal val Red40 = Color(0xFFB3261E)
internal val Red80 = Color(0xFFF2B8B5)

/** Cores semânticas de resultado, usadas pelos indicadores financeiros. */
object ProfitColors {
    val positiveLight = Green40
    val positiveDark = Green80
    val negativeLight = Red40
    val negativeDark = Red80
}
