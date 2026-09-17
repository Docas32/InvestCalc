package br.investcalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import br.investcalc.app.ui.navigation.AppNavHost

/**
 * Entry point hermettico do InvestCalc (ASCII, M3, sem rede).
 *
 * O setContent rendereza unicamente a navegacao principal
 * ([AppNavHost]): bottom nav com 4 abas (Calculadora, Tesouro Direto,
 * Comparar, Ajustes). Todo o fluxo de simulacao vive dentro da aba
 * Calculadora; nenhum estado global e persistido aqui.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavHost()
            }
        }
    }
}