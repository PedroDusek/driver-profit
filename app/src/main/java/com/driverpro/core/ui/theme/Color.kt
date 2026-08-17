package com.driverpro.core.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de marca — índigo profundo. Separada de propósito do verde/vermelho
// de [ProfitColors]: a cor de marca aparece em botão, FAB, chip selecionado e
// navegação, e nunca deveria competir com o significado de "lucro" ou
// "prejuízo" — antes da v0.14.0 a primária ERA o verde de resultado, o que
// misturava as duas coisas.
internal val BrandIndigo10 = Color(0xFF0A1477)
internal val BrandIndigo40 = Color(0xFF3B48C4)
internal val BrandIndigo90 = Color(0xFFE0E0FF)
internal val BrandIndigo80 = Color(0xFFBFC3FF)
internal val BrandIndigo20 = Color(0xFF1A2299)
internal val BrandIndigo30 = Color(0xFF2E37AC)

internal val Slate40 = Color(0xFF5A5D72)
internal val Slate90 = Color(0xFFDFE1F8)
internal val Slate10 = Color(0xFF171A2C)
internal val Slate80 = Color(0xFFC3C5DC)
internal val Slate30 = Color(0xFF434559)
internal val Slate20 = Color(0xFF2C2F42)

internal val Amber40 = Color(0xFF7A5A00)
internal val Amber90 = Color(0xFFFFDF9C)
internal val Amber10 = Color(0xFF271A00)
internal val Amber80 = Color(0xFFEBC24A)
internal val Amber30 = Color(0xFF5B4300)
internal val Amber20 = Color(0xFF3F2E00)

internal val Red40 = Color(0xFFB3261E)
internal val Red90 = Color(0xFFF9DEDC)
internal val Red10 = Color(0xFF410E0B)
internal val Red80 = Color(0xFFF2B8B5)
internal val Red30 = Color(0xFF8C1D18)
internal val Red20 = Color(0xFF601410)

// Neutros com leve matiz índigo (em vez de cinza puro) para as superfícies em
// camada (surfaceContainer*) terem profundidade sem depender de sombra.
internal val Neutral99 = Color(0xFFFFFBFF)
internal val Neutral95 = Color(0xFFF7F5FB)
internal val Neutral92 = Color(0xFFF1EFF7)
internal val Neutral90 = Color(0xFFEBE9F2)
internal val Neutral87 = Color(0xFFE5E3EC)
internal val Neutral80 = Color(0xFFC7C5D0)
internal val Neutral60 = Color(0xFF767680)
internal val Neutral50 = Color(0xFF908F9A)
internal val Neutral20 = Color(0xFF1B1B1F)
internal val Neutral10 = Color(0xFF131318)
internal val Neutral6 = Color(0xFF0E0E13)
internal val Neutral17 = Color(0xFF1B1B21)
internal val Neutral22 = Color(0xFF1F1F26)
internal val Neutral30 = Color(0xFF292930)
internal val Neutral38 = Color(0xFF34343C)
internal val Neutral90Text = Color(0xFFE5E1E9)
internal val NeutralVariant30 = Color(0xFF46464F)

/**
 * Cor semântica de resultado financeiro — lucro/prejuízo.
 *
 * Deliberadamente separada da cor de marca ([BrandIndigo40]). É usada só onde
 * o número exibido É um resultado financeiro (card de lucro, indicadores por
 * km/hora), nunca como cor de interação genérica.
 */
object ProfitColors {
    val positiveLight = Color(0xFF1E7A4C)
    val onPositiveLight = Color(0xFFFFFFFF)
    val positiveContainerLight = Color(0xFFD3F5DE)
    val onPositiveContainerLight = Color(0xFF062E17)

    val positiveDark = Color(0xFF8BD6A8)
    val onPositiveDark = Color(0xFF063920)
    val positiveContainerDark = Color(0xFF145A34)
    val onPositiveContainerDark = Color(0xFFD3F5DE)

    val negativeLight = Red40
    val onNegativeLight = Color(0xFFFFFFFF)
    val negativeContainerLight = Red90
    val onNegativeContainerLight = Red10

    val negativeDark = Red80
    val onNegativeDark = Red20
    val negativeContainerDark = Red30
    val onNegativeContainerDark = Red90
}

/**
 * Cor de destaque fixa por categoria de despesa, usada só no chip de ícone
 * das linhas de lista ([com.driverpro.core.ui.component.IconChip]).
 *
 * Pré-atenção visual: reconhecer a natureza do lançamento pela cor antes de
 * ler o texto. Um tom só por categoria (não um par claro/escuro) porque o
 * chip deriva o fundo tonal a partir dele em tempo de composição — ver
 * `IconChip.kt`.
 */
object CategoryAccentColors {
    val fuel = Color(0xFFB8720A)
    val charging = Color(0xFF0E8A7D)
    val maintenance = Color(0xFF2E5AAC)
    val carWash = Color(0xFF0E7FA6)
    val toll = Color(0xFF5B5D72)
    val parking = Color(0xFF6B7280)
    val insurance = Color(0xFF7B4FA6)
    val vehicleTax = Color(0xFFB54708)
    val financing = Color(0xFF6449C2)
    val other = Color(0xFF6B6B76)
}
