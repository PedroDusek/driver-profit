package com.driverpro.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Tipografia do aplicativo.
 *
 * Usa a fonte do sistema em vez de embarcar uma família própria: menos peso no
 * APK e respeito às preferências de acessibilidade do aparelho. Os tamanhos
 * ficam em `sp` para acompanhar o ajuste de fonte do usuário.
 */
internal val DriverProTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
)

/**
 * Feature OpenType de algarismo tabular (largura fixa por dígito).
 *
 * Aplicada aos números financeiros grandes (lucro, valores de [StatTile])
 * para que dígitos alinhem em coluna quando o valor muda — sem isso, "1" e
 * "8" têm larguras diferentes na fonte do sistema e o número "pula" ao
 * recompor.
 */
internal const val TabularFigures = "tnum"
