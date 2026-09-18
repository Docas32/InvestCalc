package br.investcalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta escura de referência do print (console do Google): fundo ~#2B2B2B,
 * primária índigo ~#5C6BC0, superfícies um pouco mais claras que o fundo.
 */
private val IndigoPrimaria = Color(0xFF5C6BC0)
private val IndigoContainer = Color(0xFF3949AB)
private val IndigoSecundaria = Color(0xFF7986CB)
private val IndigoTernaria = Color(0xFF9FA8DA)
private val CinzaErro = Color(0xFFEF5350)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaria,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = IndigoContainer,
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = IndigoSecundaria,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF28304F),
    onSecondaryContainer = Color(0xFFD7DCFF),
    tertiary = IndigoTernaria,
    onTertiary = Color(0xFF202040),
    background = Color(0xFF2B2B2B),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF333333),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF3D3D3D),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF8E8E8E),
    outlineVariant = Color(0xFF494949),
    error = CinzaErro,
    onError = Color(0xFFFFFFFF),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC5CAE9),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF5C6BC0),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC5CAE9),
    onSecondaryContainer = Color(0xFF28304F),
    tertiary = Color(0xFF7986CB),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF7F7F7),
    onBackground = Color(0xFF1F1F1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFE3E4EB),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFF9E9E9E),
    outlineVariant = Color(0xFFCBCBCB),
    error = Color(0xFFD32F2F),
    onError = Color(0xFFFFFFFF),
)

/**
 * Tema Material 3 do InvestCalc: paleta índigo escura (referência do print)
 * e paleta clara correspondente, seguindo sempre o modo do sistema
 * ([isSystemInDarkTheme]). Nenhuma cor é hardcoded nos composables — tudo é
 * lido via MaterialTheme.colorScheme.
 */
@Composable
fun InvestCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
