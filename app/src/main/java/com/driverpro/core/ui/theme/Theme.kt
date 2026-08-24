package com.driverpro.core.ui.theme

import android.os.Build
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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
    primary = BrandGreen60,
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
 * Se o tema **do app** está escuro.
 *
 * Existe porque `isSystemInDarkTheme()` responde pelo aparelho, não pelo app,
 * e desde que o [DriverProTheme] passou a fixar o escuro as duas respostas
 * divergem em todo aparelho configurado no claro. Quem precisa escolher uma
 * cor por tema — [content], abaixo — tem que perguntar ao tema que está
 * realmente pintando a tela, senão devolve a cor clara sobre fundo escuro.
 */
internal val LocalIsDarkTheme = staticCompositionLocalOf { true }

/**
 * Tema do aplicativo.
 *
 * **Escuro por padrão, e não conforme o aparelho.** A identidade visual do
 * DriverPro é o navy com verde da referência do Figma; seguir o claro/escuro
 * do sistema faria a maioria dos motoristas nunca ver o app que foi
 * desenhado. O parâmetro [darkTheme] continua existindo — e `LightColors`
 * continua no código, íntegro — para quem quiser reverter isso, e para
 * previews pedirem o claro explicitamente.
 *
 * `dynamicColor` (Material You) desligado por padrão: um app com identidade
 * visual própria não deveria trocar de cor conforme o papel de parede do
 * aparelho. O parâmetro continua existindo para quem quiser ligar, e para
 * testes/previews pedirem um resultado determinístico.
 */
@Composable
fun DriverProTheme(
    darkTheme: Boolean = true,
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

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DriverProTypography,
            shapes = DriverProShapes,
            content = content,
        )
    }
}

/**
 * Cores da barra de título de todas as telas.
 *
 * O padrão do Material 3 pinta a `TopAppBar` com `surface`, que no tema
 * escuro é a cor dos **cards** — o resultado é uma faixa mais clara no topo,
 * separada do resto da página. A referência do Figma não tem essa faixa: o
 * título vive no mesmo plano do conteúdo. Usar `background` funde a barra com
 * a página e devolve esse plano único.
 *
 * Vive aqui, e não repetida em cada tela, porque são doze telas: espalhar a
 * configuração garantiria que uma delas ficasse para trás na próxima
 * mudança de paleta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun driverProTopAppBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.background,
    scrolledContainerColor = MaterialTheme.colorScheme.background,
)

/**
 * Cor de texto de [ProfitColors] para o tema atual (claro ou escuro) — usada
 * no número de lucro do Dashboard, que segue em card neutro (a referência não
 * pinta o card inteiro, só o número).
 */
@Composable
fun ProfitColors.content(positive: Boolean): Color {
    val dark = LocalIsDarkTheme.current
    return when {
        dark && positive -> positiveDark
        dark && !positive -> negativeDark
        positive -> positiveLight
        else -> negativeLight
    }
}
