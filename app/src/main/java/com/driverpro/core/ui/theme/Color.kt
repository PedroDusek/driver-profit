package com.driverpro.core.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta de marca — verde, seguindo a logo e a referência visual fornecidas
// pelo Pedro (`IMAGENS/`). Substitui o índigo da primeira versão do
// redesign: lá a marca tinha sido deliberadamente separada do verde de
// resultado financeiro; a referência nova usa **um verde só** para as duas
// coisas — é a decisão que vale agora (ver ARCHITECTURE.md).
internal val BrandGreen10 = Color(0xFF04432B)
internal val BrandGreen30 = Color(0xFF0E5C3D)
internal val BrandGreen40 = Color(0xFF15A46E)
internal val BrandGreen80 = Color(0xFF6FE3B4)
internal val BrandGreen90 = Color(0xFFD4F5E6)
internal val BrandGreen20 = Color(0xFF04321F)

internal val Slate10 = Color(0xFF0B1F16)
internal val Slate20 = Color(0xFF20352A)
internal val Slate30 = Color(0xFF38503F)
internal val Slate40 = Color(0xFF4F6459)
internal val Slate80 = Color(0xFFAEC7BB)
internal val Slate90 = Color(0xFFD2E8DA)

internal val Amber10 = Color(0xFF271A00)
internal val Amber20 = Color(0xFF3F2E00)
internal val Amber30 = Color(0xFF5B4300)
internal val Amber40 = Color(0xFF7A5A00)
internal val Amber80 = Color(0xFFEBC24A)
internal val Amber90 = Color(0xFFFFDF9C)

internal val Red10 = Color(0xFF410E0B)
internal val Red20 = Color(0xFF601410)
internal val Red30 = Color(0xFF8C1D18)
internal val Red40 = Color(0xFFB3261E)
internal val Red80 = Color(0xFFF2B8B5)
internal val Red90 = Color(0xFFF9DEDC)

// Neutros claros com leve matiz verde-acinzentado — página cinza-clara com
// cards brancos "flutuando" por cima, como na referência (em vez do card
// tonal quase indistinguível do fundo que o M3 puro produz).
internal val LightBackground = Color(0xFFF4F6F4)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFE2E6E3)
internal val LightOnSurfaceVariant = Color(0xFF444C47)
internal val LightOnSurface = Color(0xFF12181F)
internal val LightOutline = Color(0xFF74796F)
internal val LightOutlineVariant = Color(0xFFC4C9C1)
internal val LightSurfaceContainerLow = Color(0xFFF7F8F6)
internal val LightSurfaceContainer = Color(0xFFF0F2EF)
internal val LightSurfaceContainerHigh = Color(0xFFE9EBE7)
internal val LightSurfaceContainerHighest = Color(0xFFE2E5E1)

// Neutros escuros com matiz navy — a cor de fundo da logo, não um cinza
// neutro genérico.
internal val DarkBackground = Color(0xFF0A0F1C)
internal val DarkSurface = Color(0xFF121B2E)
internal val DarkSurfaceVariant = Color(0xFF2A3444)
internal val DarkOnSurfaceVariant = Color(0xFFB7C2D6)
internal val DarkOnSurface = Color(0xFFE7EAF0)
internal val DarkOutline = Color(0xFF7C8AA0)
internal val DarkOutlineVariant = Color(0xFF2A3444)
internal val DarkSurfaceContainerLowest = Color(0xFF070B14)
internal val DarkSurfaceContainerLow = Color(0xFF101A2D)
internal val DarkSurfaceContainer = Color(0xFF141F35)
internal val DarkSurfaceContainerHigh = Color(0xFF1A263D)
internal val DarkSurfaceContainerHighest = Color(0xFF212E48)

/**
 * Cor semântica de resultado financeiro — lucro/prejuízo.
 *
 * O verde aqui é intencionalmente o mesmo da marca (`BrandGreen*`): a
 * referência visual usa um verde só para as duas coisas, e a história do
 * produto ("verde = seu lucro crescendo") sustenta isso. Continua sendo um
 * tipo à parte, e não um alias direto de `BrandGreen`, para o card de lucro
 * do Dashboard poder trocar de cor (verde/vermelho) sem depender da cor de
 * marca se um dia elas voltarem a divergir.
 */
object ProfitColors {
    val positiveLight = BrandGreen40
    val onPositiveLight = Color(0xFFFFFFFF)
    val positiveContainerLight = BrandGreen90
    val onPositiveContainerLight = BrandGreen10

    val positiveDark = BrandGreen80
    val onPositiveDark = BrandGreen20
    val positiveContainerDark = BrandGreen30
    val onPositiveContainerDark = BrandGreen90

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
 * Cor de destaque fixa por categoria de despesa, usada no chip de ícone das
 * linhas de lista ([com.driverpro.core.ui.component.IconChip]) e no gráfico
 * de rosca do breakdown ([com.driverpro.core.ui.component.DonutChart]).
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

/**
 * Cor de destaque fixa por plataforma, usada no breakdown "Por plataforma"
 * de Ganhos. Não tenta reproduzir a marca real de cada app — só precisa
 * distinguir as linhas de relance, e verde já está ocupado pela marca do
 * DriverPro.
 */
object PlatformAccentColors {
    val uber = Color(0xFF1A1A1A)
    val ninetyNine = Color(0xFFC98A00)
    val inDrive = Color(0xFF2563EB)
    val other = Color(0xFF6B7280)
}
