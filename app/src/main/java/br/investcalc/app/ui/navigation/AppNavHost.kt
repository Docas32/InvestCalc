package br.investcalc.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.investcalc.app.R
import br.investcalc.app.data.rates.BcbApiFactory
import br.investcalc.app.data.rates.BcbRatesRepository
import br.investcalc.app.data.rates.DataStoreTaxaCache
import br.investcalc.app.data.rates.RatesSnapshot
import br.investcalc.app.domain.chart.EvolutionChartBuilder
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.ui.chart.EvolutionChartScreen
import br.investcalc.app.ui.compare.ComparadorScreen
import br.investcalc.app.ui.input.SimulacaoScreen
import br.investcalc.app.ui.input.TAXA_DI_ANUAL_FALLBACK_PERCENTUAL
import br.investcalc.app.ui.result.ResumoResultado
import br.investcalc.app.ui.settings.AjustesScreen
import br.investcalc.app.ui.tesouro.TesouroScreen
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ROTA_CALCULADORA = "calculadora"
private const val ROTA_TESOURO = "tesouro"
private const val ROTA_COMPARAR = "comparar"
private const val ROTA_AJUSTES = "ajustes"

/**
 * Navegação principal do InvestCalc (Material 3, sem redes fora do BCB).
 *
 * Bottom nav com 4 abas (Calculadora, Tesouro Direto, Comparar, Ajustes).
 * Uma única camada de taxas ([BcbRatesRepository]) é compartilhada pelas três
 * abas de cálculo — nada de infra paralela: refresh em segundo plano no cold
 * start e badge "BCB · dd/mm" / "fallback" derivadas do mesmo snapshot.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val rotaAtual = backStack?.destination?.route

    val contexto = LocalContext.current
    val repositorioTaxas = remember {
        BcbRatesRepository(
            api = BcbApiFactory.criar(),
            cache = DataStoreTaxaCache(contexto),
        )
    }

    LaunchedEffect(Unit) {
        repositorioTaxas.atualizar()
    }
    val taxaSnapshot by repositorioTaxas.state.collectAsState()
    val origemLabel = rotuloOrigemTaxa(taxaSnapshot)

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
            composable(ROTA_CALCULADORA) { AbaCalculadora(taxaSnapshot, origemLabel) }
            composable(ROTA_TESOURO) { TesouroScreen(taxaSnapshot, origemLabel) }
            composable(ROTA_COMPARAR) { ComparadorScreen(taxaSnapshot, origemLabel) }
            composable(ROTA_AJUSTES) {
                AjustesScreen(
                    consentimentoAdsAtivo = false,
                    onAlternativeConsentimento = {},
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
private fun AbaCalculadora(taxaSnapshot: RatesSnapshot, origemLabel: String) {
    var serie by remember { mutableStateOf<PatrimonioSeries?>(null) }
    var resultado by remember { mutableStateOf<InvestmentResult?>(null) }

    val taxaDiAutomatica = taxaSnapshot.taxaCdiAnualPercentual
        .takeIf { it > 0.0 }
        ?: TAXA_DI_ANUAL_FALLBACK_PERCENTUAL

    // Gráfico derivado da série mensal já existente (mesma taxa efetiva da
    // simulação) — pipeline único compartilhado com os testes de consistência.
    val chart = remember(serie) {
        serie?.let { EvolutionChartBuilder.dePatrimonio(it) }
    }

    if (serie == null) {
        SimulacaoScreen(
            onSeriePronta = { serie = it },
            onResultadoPronto = { resultado = it },
            onVoltar = {},
            taxaDiAnualAutomatica = taxaDiAutomatica,
            taxaDiOrigemLabel = origemLabel,
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { serie = null; resultado = null }) {
                Text(stringResource(R.string.voltar))
            }
            resultado?.let { ResumoResultado(it) }
            EvolutionChartScreen(
                series = chart,
                onTooltipUsed = {},
                onTableOpened = {},
            )
        }
    }
}

/** "BCB · dd/mm" quando há data de atualização; senão "fallback". */
@Composable
private fun rotuloOrigemTaxa(snapshot: RatesSnapshot): String {
    val data = snapshot.atualizadaEm ?: return stringResource(R.string.origem_taxa_fallback)
    return stringResource(
        R.string.origem_taxa_bcb,
        data.format(DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT)),
    )
}