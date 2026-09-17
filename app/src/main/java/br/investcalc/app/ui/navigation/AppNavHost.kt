package br.investcalc.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.investcalc.app.R
import br.investcalc.app.domain.chart.EvolutionChartBuilder
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.ui.chart.EvolutionChartScreen
import br.investcalc.app.ui.input.SimulacaoScreen
import br.investcalc.app.ui.settings.AjustesScreen
import kotlin.math.pow

private const val ROTA_CALCULADORA = "calculadora"
private const val ROTA_TESOURO = "tesouro"
private const val ROTA_COMPARAR = "comparar"
private const val ROTA_AJUSTES = "ajustes"

/**
 * Navegacao principal do InvestCalc (ASCII puro, M3, sem rede, sem Firebase).
 *
 * Bottom nav com 4 abas (Calculadora, Tesouro Direto, Comparar, Ajustes).
 * A aba Calculadora mantem o fluxo local SimulacaoScreen -> EvolutionChartScreen
 * (estado em memoria, nunca persistido); a serie trazida pela simulacao e
 * convertida para a linha de juros compostos com baseline de juros simples.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val rotaAtual = backStack?.destination?.route

    Scaffold(
        modifier = modifier,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = rotaAtual == ROTA_CALCULADORA,
                    onClick = { navController.navigarPara(ROTA_CALCULADORA) },
                    icon = { Icon(Icons.Filled.Calculate, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_calculadora)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_TESOURO,
                    onClick = { navController.navigarPara(ROTA_TESOURO) },
                    icon = { Icon(Icons.Filled.AccountBalance, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_tesouro)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_COMPARAR,
                    onClick = { navController.navigarPara(ROTA_COMPARAR) },
                    icon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_comparar)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_AJUSTES,
                    onClick = { navController.navigarPara(ROTA_AJUSTES) },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text(stringResource(R.string.nav_ajustes)) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROTA_CALCULADORA,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            composable(ROTA_CALCULADORA) { AbaCalculadora() }
            composable(ROTA_TESOURO) {
                AbaTexto(
                    titulo = stringResource(R.string.tesouro_title),
                    descricao = stringResource(R.string.tesouro_desc),
                )
            }
            composable(ROTA_COMPARAR) {
                AbaTexto(
                    titulo = stringResource(R.string.comparar_title),
                    descricao = stringResource(R.string.comparar_desc),
                )
            }
            composable(ROTA_AJUSTES) {
                AjustesScreen(
                    consentimentoAdsAtivo = false,
                    notificacoesAtivas = false,
                    cacheTaxasOcupado = false,
                    onAlternativeConsentimento = {},
                    onAlternarNotificacoes = {},
                    onRecarregarTaxas = {},
                )
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigarPara(rota: String) {
    navigate(rota) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AbaCalculadora() {
    var serie by remember { mutableStateOf<PatrimonioSeries?>(null) }

    val chart = remember(serie) {
        serie?.let { s ->
            val pontos = s.pontos
            val saldos = pontos.map { it.patrimonioLiquidoFimDoMes }
            val jurosAcumulados = ArrayList<Double>(pontos.size)
            var jurosCorridos = 0.0
            pontos.forEach { p ->
                jurosCorridos += p.rendimentoNoMes
                jurosAcumulados.add(jurosCorridos)
            }
            val totais = saldos.indices.map { m -> saldos[m] - jurosAcumulados[m] }
            val n = pontos.size.coerceAtLeast(1)
            val totalFinal = totais.lastOrNull() ?: 0.0
            val saldoFinal = saldos.lastOrNull() ?: 0.0
            val taxaMensal = if (totalFinal > 0.0 && saldoFinal > 0.0) {
                (saldoFinal / totalFinal).pow(1.0 / n) - 1.0
            } else {
                0.0
            }
            val baseline = List(n) { m -> totais[m] * taxaMensal * (m + 1) }
            EvolutionChartBuilder.construir(
                meses = pontos.map { it.inicioDoMes },
                totalInvestidoFimDoMes = totais,
                saldoCompostoFimDoMes = saldos,
                jurosSimplesBaselineFimDoMes = baseline,
                aportesPorMes = pontos.map { it.aporteNoMes },
            )
        }
    }

    if (serie == null) {
        SimulacaoScreen(
            onSeriePronta = { serie = it },
            onVoltar = {},
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { serie = null }) { Text("Voltar") }
            EvolutionChartScreen(
                series = chart,
                onTooltipUsed = {},
                onTableOpened = {},
            )
        }
    }
}

@Composable
private fun AbaTexto(titulo: String, descricao: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(text = descricao, style = MaterialTheme.typography.bodyMedium)
    }
}