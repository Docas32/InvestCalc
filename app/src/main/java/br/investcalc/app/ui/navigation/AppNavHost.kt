package br.investcalc.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import br.investcalc.app.domain.chart.JurosSimplesBaselineBuilder
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.ui.chart.EvolutionChartScreen
import br.investcalc.app.ui.input.SimulacaoScreen
import br.investcalc.app.ui.input.TAXA_DI_ANUAL_FALLBACK_PERCENTUAL
import br.investcalc.app.ui.settings.AjustesScreen
import java.time.format.DateTimeFormatter
import java.util.Locale

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
                    onAlternativeConsentimento = {},
                    onAlternarNotificacoes = {},
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
    var resultado by remember { mutableStateOf<InvestmentResult?>(null) }

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
    val taxaDiAutomatica = taxaSnapshot.taxaCdiAnualPercentual
        .takeIf { it > 0.0 }
        ?: TAXA_DI_ANUAL_FALLBACK_PERCENTUAL

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
            // Baseline de juros simples com a taxa efetiva real usada na
            // simulacao: P*i*m + A*i*m*(m-1)/2 (ver JurosSimplesBaselineBuilder).
            val baseline = JurosSimplesBaselineBuilder.construir(
                valorInicial = s.valorInicial,
                aporteMensal = s.aporteMensal,
                meses = n,
                taxaPercentualMensal = s.taxaMensalPercentual,
            ).pontos.map { it.jurosSimplesAcumuladoFimDoMes }
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
            onResultadoPronto = { resultado = it },
            onVoltar = {},
            taxaDiAnualAutomatica = taxaDiAutomatica,
            taxaDiOrigemLabel = origemLabel,
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { serie = null; resultado = null }) { Text("Voltar") }
            resultado?.let { ir ->
                ResumoResultado(ir)
            }
            EvolutionChartScreen(
                series = chart,
                onTooltipUsed = {},
                onTableOpened = {},
            )
        }
    }
}

/** "BCB · dd/mm" quando há data de atualização; senão "fallback". */
private fun rotuloOrigemTaxa(snapshot: RatesSnapshot): String {
    val data = snapshot.atualizadaEm ?: return "fallback"
    return "BCB · " + data.format(DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT))
}

@Composable
private fun ResumoResultado(resultado: InvestmentResult) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Resultado (com IR)",
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        LinhaResumo("Valor bruto", resultado.valorBruto.cents)
        LinhaResumo("Total investido", resultado.valorInvestido.cents)
        LinhaResumo("Rendimento bruto", resultado.rendimentoBruto.cents)
        LinhaResumo("IR (" + "%.1f".format(Locale.ROOT, resultado.aliquotaIr * 100.0) + "%)", resultado.irPago.cents)
        LinhaResumo(
            "Valor liquido",
            resultado.valorLiquido.cents,
            destaque = true,
        )
    }
}

@Composable
private fun LinhaResumo(rotulo: String, centavos: Long, destaque: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = rotulo + ": ",
            modifier = Modifier.weight(1f),
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "R$ " + formatarCentavos(centavos),
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatarCentavos(centavos: Long): String =
    String.format(Locale.ROOT, "%,.2f", centavos / 100.0)

@Composable
private fun AbaTexto(titulo: String, descricao: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = titulo, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(text = descricao, style = MaterialTheme.typography.bodyMedium)
    }
}