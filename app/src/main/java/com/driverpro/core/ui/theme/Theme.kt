package com.driverpro.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = BrandIndigo40,
    onPrimary = Neutral99,
    primaryContainer = BrandIndigo90,
    onPrimaryContainer = BrandIndigo10,
    secondary = Slate40,
    onSecondary = Neutral99,
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Amber40,
    onTertiary = Neutral99,
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Neutral99,
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = Neutral99,
    onBackground = Neutral20,
    surface = Neutral99,
    onSurface = Neutral20,
    surfaceVariant = Neutral87,
    onSurfaceVariant = NeutralVariant30,
    outline = Neutral60,
    outlineVariant = Neutral80,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Neutral95,
    surfaceContainer = Neutral92,
    surfaceContainerHigh = Neutral90,
    surfaceContainerHighest = Neutral87,
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigo80,
    onPrimary = BrandIndigo20,
    primaryContainer = BrandIndigo30,
    onPrimaryContainer = BrandIndigo90,
    secondary = Slate80,
    onSecondary = Slate20,
    secondaryContainer = Slate30,
    onSecondaryContainer = Slate90,
    tertiary = Amber80,
    onTertiary = Amber20,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber90,
    error = Red80,
    onError = Red20,
    errorContainer = Red30,
    onErrorContainer = Red90,
    background = Neutral10,
    onBackground = Neutral90Text,
    surface = Neutral10,
    onSurface = Neutral90Text,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = Neutral80,
    outline = Neutral50,
    outlineVariant = NeutralVariant30,
    surfaceContainerLowest = Neutral6,
    surfaceContainerLow = Neutral17,
    surfaceContainer = Neutral22,
    surfaceContainerHigh = Neutral30,
    surfaceContainerHighest = Neutral38,
)

/**
 * Tema do aplicativo.
 *
 * `dynamicColor` (Material You) desligado por padrão a partir da v0.14.0: um
 * app que acabou de fixar identidade visual própria não deveria trocar de cor
 * conforme o papel de parede do aparelho — isso é o oposto de "visual
 * profissional consistente". O parâmetro continua existindo para quem quiser
 * ligar, e para testes/previews pedirem um resultado determinístico.
 */
@Composable
fun DriverProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DriverProTypography,
        shapes = DriverProShapes,
        content = content,
    )
}

/**
 * Par contêiner/on-contêiner de [ProfitColors] para o tema atual (claro ou
 * escuro), usado no card de lucro do Dashboard — o único lugar da tela onde a
 * cor de fundo em si é o sinal financeiro, não só o texto.
 */
@Composable
fun ProfitColors.container(positive: Boolean): Color = when {
    isSystemInDarkTheme() && positive -> positiveContainerDark
    isSystemInDarkTheme() && !positive -> negativeContainerDark
    positive -> positiveContainerLight
    else -> negativeContainerLight
}

@Composable
fun ProfitColors.onContainer(positive: Boolean): Color = when {
    isSystemInDarkTheme() && positive -> onPositiveContainerDark
    isSystemInDarkTheme() && !positive -> onNegativeContainerDark
    positive -> onPositiveContainerLight
    else -> onNegativeContainerLight
}
