package br.investcalc.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Paleta WhatsApp (P7/T3) — única fonte de cores do app (nenhum hex fora daqui,
 * exceto recursos XML do sistema/splash e os ativos do ícone, fora do escopo
 * até a aprovação do tema).
 *
 * Light:  primary #008069, secondary #25D366, background #F6F6F6, surface #FFFFFF.
 * Dark:   primary #00A884, secondary #25D366, background #0B141A, surface #1F2C34.
 */
private val VerdePrimariaClaro = Color(0xFF008069)
private val VerdeSecundaria = Color(0xFF25D366)
private val CinzaErro = Color(0xFFBA1A1A)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF00A884),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF005441),
    onPrimaryContainer = Color(0xFF9FF5DB),
    secondary = VerdeSecundaria,
    onSecondary = Color(0xFF002E14),
    secondaryContainer = Color(0xFF004D24),
    onSecondaryContainer = Color(0xFF9FF0B8),
    tertiary = Color(0xFF00A884),
    onTertiary = Color(0xFF00382C),
    background = Color(0xFF0B141A),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF1F2C34),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF38474E),
    onSurfaceVariant = Color(0xFFBFC9CC),
    outline = Color(0xFF899396),
    outlineVariant = Color(0xFF39474D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColors = lightColorScheme(
    primary = VerdePrimariaClaro,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA8E6D5),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = VerdeSecundaria,
    onSecondary = Color(0xFF002E14),
    secondaryContainer = Color(0xFFB7F6C7),
    onSecondaryContainer = Color(0xFF002E0F),
    tertiary = Color(0xFF006C5A),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFDCE5E1),
    onSurfaceVariant = Color(0xFF404944),
    outline = Color(0xFF6F7974),
    outlineVariant = Color(0xFFBEC9C4),
    error = CinzaErro,
    onError = Color(0xFFFFFFFF),
)

/**
 * Tema Material 3 do InvestCalc: paleta WhatsApp (P7/T3), seguindo sempre o
 * modo do sistema ([isSystemInDarkTheme]). Nenhuma cor é hardcoded nos
 * composables — tudo é lido via MaterialTheme.colorScheme.
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