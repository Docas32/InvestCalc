package br.investcalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VerdeCalc = Color(0xFF00695C)
private val VerdeCalcEscuro = Color(0xFF80CBC4)
private val CianoClaro = Color(0xFFB2DFDB)
private val Teal = Color(0xFF004D40)

private val LightColors = lightColorScheme(
    primary = VerdeCalc,
    secondary = Teal,
    tertiary = Color(0xFF00695C),
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = VerdeCalcEscuro,
    secondary = CianoClaro,
    tertiary = Color(0xFF80CBC4),
    background = Color(0xFF121212),
    surface = Color(0xFF1C1C1C),
)

/**
 * Tema Material 3 do InvestCalc, com paleta verde/teal e suporte a tema
 * claro/escuro seguindo a preferência do sistema.
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
