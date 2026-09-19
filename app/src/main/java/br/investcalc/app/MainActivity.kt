package br.investcalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.investcalc.app.ui.navigation.AppNavHost
import br.investcalc.app.ui.theme.InvestCalcTheme

/**
 * Entry point hermettico do InvestCalc (M3, sem rede).
 *
 * O setContent rendereza unicamente a navegacao principal
 * ([AppNavHost]) sob o tema Material 3 do app ([InvestCalcTheme]): paleta
 * indigo escura de referencia do print + paleta clara, seguindo o modo do
 * sistema. Bottom nav com 4 abas (Calculadora, Tesouro Direto, Comparar,
 * Ajustes); o fluxo de simulacao vive dentro da aba Calculadora e nenhum
 * estado global e persistido aqui.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            InvestCalcTheme {
                AppNavHost()
            }
        }
    }
}