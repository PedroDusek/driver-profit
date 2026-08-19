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
    primary = BrandGreen40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = BrandGreen90,
    onPrimaryContainer = BrandGreen10,
    secondary = Slate40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Slate90,
    onSecondaryContainer = Slate10,
    tertiary = Amber40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Amber90,
    onTertiaryContainer = Amber10,
    error = Red40,
    onError = Color(0xFFFFFFFF),
    errorContainer = Red90,
    onErrorContainer = Red10,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreen80,
    onPrimary = BrandGreen20,
    primaryContainer = BrandGreen30,
    onPrimaryContainer = BrandGreen90,
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
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

/**
 * Tema do aplicativo.
 *
 * `dynamicColor` (Material You) desligado por padrão: um app com identidade
 * visual própria não deveria trocar de cor conforme o papel de parede do
 * aparelho. O parâmetro continua existindo para quem quiser ligar, e para
 * testes/previews pedirem um resultado determinístico.
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
 * Cor de texto de [ProfitColors] para o tema atual (claro ou escuro) — usada
 * no número de lucro do Dashboard, que segue em card branco/neutro
 * (`IMAGENS/Referencia Visual.jpeg` não pinta o card inteiro, só o número).
 */
@Composable
fun ProfitColors.content(positive: Boolean): Color = when {
    isSystemInDarkTheme() && positive -> positiveDark
    isSystemInDarkTheme() && !positive -> negativeDark
    positive -> positiveLight
    else -> negativeLight
}
