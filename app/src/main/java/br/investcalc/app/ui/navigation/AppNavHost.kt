package br.investcalc.app.ui.navigation

import androidx.activity.compose.BackHandler
import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import br.investcalc.app.R
import br.investcalc.app.data.ads.DataStoreSimulacaoCounter
import br.investcalc.app.data.ads.IntersticialCarregador
import br.investcalc.app.data.ads.UmConsent
import br.investcalc.app.data.analytics.FirebaseAnalyticsRepository
import br.investcalc.app.data.plano.DataStorePlanoSemAdsPersistence
import br.investcalc.app.data.plano.PlanoSemAdsRepositorio
import br.investcalc.app.data.rates.BcbApiFactory
import br.investcalc.app.data.rates.BcbRatesRepository
import br.investcalc.app.data.rates.DataStoreTaxaCache
import br.investcalc.app.data.rates.RatesSnapshot
import br.investcalc.app.data.rates.RawXmlDefaultsRates
import br.investcalc.app.data.remotecfg.AppSettings
import br.investcalc.app.data.remotecfg.FirebaseRemoteConfigAds
import br.investcalc.app.data.remotecfg.FirebaseRemoteConfigRates
import br.investcalc.app.domain.ads.InterstitialGate
import br.investcalc.app.domain.chart.EvolutionChartBuilder
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.plano.EstadoPlanoSemAds
import br.investcalc.app.domain.plano.ResultadoRestauracao
import br.investcalc.app.ui.ads.BannerRodape
import br.investcalc.app.ui.chart.EvolutionChartScreen
import br.investcalc.app.ui.compare.ComparadorScreen
import br.investcalc.app.ui.guia.GuiaScreen
import br.investcalc.app.ui.input.SimulacaoScreen
import br.investcalc.app.ui.input.TAXA_DI_ANUAL_FALLBACK_PERCENTUAL
import br.investcalc.app.ui.result.ResumoResultado
import br.investcalc.app.ui.settings.AjustesScreen
import br.investcalc.app.ui.tesouro.TesouroScreen
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val ROTA_CALCULADORA = "calculadora"
private const val ROTA_TESOURO = "tesouro"
private const val ROTA_COMPARAR = "comparar"
private const val ROTA_GUIA = "guia"
private const val ROTA_AJUSTES = "ajustes"

/** Abas de cálculo que exibem o banner adaptativo no rodapé (T2/P4). */
private val rotasComBanner = setOf(ROTA_CALCULADORA, ROTA_TESOURO, ROTA_COMPARAR)

/**
 * Navegação principal do InvestCalc (Material 3).
 *
 * Bottom nav com 4 abas (Calculadora, Tesouro Direto, Comparar, Ajustes) e a
 * tela do Guia agora EMPILHADA (aberta de dentro de Ajustes, não é mais aba),
 * camada de taxas compartilhada ([BcbRatesRepository]) e a pasta de
 * monetização/fidelidade (T2/P4), tudo fio-a-fio aqui (sem Hilt):
 *
 *  - Analytics (Firebase): `calculation_performed`, `rates_origin`,
 *    `chart_tooltip_used` e `screen_view` nas 4 abas + Guia (rota "guia");
 *  - Remote Config de ads (`ads_enabled`, `interstitial_frequency`) buscado
 *    no cold start;
 *  - UMP ([UmConsent]): formulário na primeira abertura e reaberto pelo toggle
 *    de Ajustes; sem consentimento não há banner nem intersticial;
 *  - banner adaptativo no rodapé das 3 abas de cálculo (P8: Guia está fora do
 *    banner, como antes);
 *  - intersticial após `interstitial_frequency` cálculos (nunca o 1º, nunca em
 *    sequência), pré-carregado quando consentido e recarregado ao fechar.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val rotaAtual = backStack?.destination?.route

    val contexto = LocalContext.current
    val scope = rememberCoroutineScope()

    val analyticsRepository = remember { FirebaseAnalyticsRepository(contexto) }
    val repositorioTaxas = remember {
        BcbRatesRepository(
            api = BcbApiFactory.criar(),
            cache = DataStoreTaxaCache(contexto),
            remoteConfig = FirebaseRemoteConfigRates(),
            xmlDefaults = RawXmlDefaultsRates(contexto),
            analyticsRepository = analyticsRepository,
        )
    }
    val remoteConfigAds = remember { FirebaseRemoteConfigAds() }
    val consentimento = remember { UmConsent(contexto) }
    val simulacaoCounter = remember { DataStoreSimulacaoCounter(contexto) }
    val intersticial = remember { IntersticialCarregador(contexto) }
    val planoSemAds = remember {
        PlanoSemAdsRepositorio(
            context = contexto,
            persistencia = DataStorePlanoSemAdsPersistence(contexto),
        )
    }

    var appSettings by remember { mutableStateOf(AppSettings()) }
    val consentimentoAdsLiberado by consentimento.adsLiberados.collectAsState(
        initial = consentimento.adsLiberados.value,
    )
    val estadoPlano by planoSemAds.estado.collectAsState()
    val planoResolvido by planoSemAds.resolvido.collectAsState()
    val precoPlano by planoSemAds.preco.collectAsState()
    val produtoDisponivel by planoSemAds.produtoDisponivel.collectAsState()
    val feedbackRestauracao by planoSemAds.feedback.collectAsState()
    var intersticialNoCalculoAnterior by remember { mutableStateOf(false) }
    var umpInicializado by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Cold start: taxas + RC de ads + plano sem ads (conecta ao Billing).
    LaunchedEffect(Unit) {
        repositorioTaxas.atualizar()
        appSettings = remoteConfigAds.carregar()
        planoSemAds.inicializar()
    }
    LaunchedEffect(rotaAtual) {
        if (rotaAtual != null) analyticsRepository.screenViewed(rotaAtual)
    }

    // "Restaurar compras" (P7.3): sempre informa o resultado num snackbar.
    LaunchedEffect(feedbackRestauracao) {
        when (feedbackRestauracao) {
            ResultadoRestauracao.COMPRA_ENCONTRADA ->
                snackbarHostState.showSnackbar(
                    contexto.getString(R.string.plano_sem_ads_restaurado),
                )
            ResultadoRestauracao.NENHUMA_COMPRA ->
                snackbarHostState.showSnackbar(
                    contexto.getString(R.string.plano_sem_ads_restaurar_sem_compra),
                )
            null -> Unit
        }
        planoSemAds.consumirFeedback()
    }

    // UMP só é pedido a quem não comprou o plano ainda — comprador a frio nunca
    // vê formulário de consentimento (T4). Aparelho sem Play (INDISPONIVEL)
    // tenta mesmo assim, pois billing fora do ar não implica consentimento.
    LaunchedEffect(estadoPlano, planoResolvido, umpInicializado) {
        if (planoResolvido && !umpInicializado &&
            estadoPlano != EstadoPlanoSemAds.COMPRADO
        ) {
            umpInicializado = true
            (contexto as? Activity)?.let { consentimento.inicializar(it) }
        }
    }

    val planoComprado = estadoPlano == EstadoPlanoSemAds.COMPRADO

    // Pré-carrega o intersticial quando houver consentimento + ads ativos e o
    // usuário ainda não comprou o plano sem ads (comprador não vê anúncio).
    LaunchedEffect(appSettings.adsEnabled, consentimentoAdsLiberado, planoComprado) {
        if (appSettings.adsEnabled && consentimentoAdsLiberado && !planoComprado) {
            intersticial.carregar()
        }
    }

    val adsHabilitado = appSettings.adsEnabled && consentimentoAdsLiberado && !planoComprado

    val registrarSimulacao: (String, Int, Boolean) -> Unit = { tipo, meses, temAporte ->
        analyticsRepository.calculationPerformed(tipo, meses, temAporte)
        scope.launch {
            val total = simulacaoCounter.incrementar()
            val exibir = InterstitialGate.deveExibir(
                adsHabilitado = adsHabilitado,
                frequencia = appSettings.interstitialFrequency,
                calculosJaInvocados = total,
                intersticialExibidoNoCalculoAnterior = intersticialNoCalculoAnterior,
            )
            intersticialNoCalculoAnterior = if (exibir) {
                val atividade = contexto as? Activity
                atividade != null && intersticial.exibirEm(atividade)
            } else {
                false
            }
        }
    }

    val taxaSnapshot by repositorioTaxas.state.collectAsState()
    val origemLabel = rotuloOrigemTaxa(taxaSnapshot)

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = rotaAtual == ROTA_CALCULADORA,
                    onClick = { navController.navigarPara(ROTA_CALCULADORA) },
                    icon = { Icon(Icons.Filled.Calculate, stringResource(R.string.nav_calculadora)) },
                    label = { RotuloNav(stringResource(R.string.nav_calculadora)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_TESOURO,
                    onClick = { navController.navigarPara(ROTA_TESOURO) },
                    icon = { Icon(Icons.Filled.AccountBalance, stringResource(R.string.nav_tesouro)) },
                    label = { RotuloNav(stringResource(R.string.nav_tesouro)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_COMPARAR,
                    onClick = { navController.navigarPara(ROTA_COMPARAR) },
                    icon = { Icon(Icons.AutoMirrored.Filled.CompareArrows, stringResource(R.string.nav_comparar)) },
                    label = { RotuloNav(stringResource(R.string.nav_comparar)) },
                )
                NavigationBarItem(
                    selected = rotaAtual == ROTA_AJUSTES,
                    onClick = { navController.navigarPara(ROTA_AJUSTES) },
                    icon = { Icon(Icons.Filled.Settings, stringResource(R.string.nav_ajustes)) },
                    label = { RotuloNav(stringResource(R.string.nav_ajustes)) },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = ROTA_CALCULADORA,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                composable(ROTA_CALCULADORA) {
                    AbaCalculadora(
                        taxaSnapshot = taxaSnapshot,
                        origemLabel = origemLabel,
                        onSimulacaoRealizada = registrarSimulacao,
                        onTooltipUsado = { analyticsRepository.chartTooltipUsed() },
                    )
                }
                composable(ROTA_TESOURO) {
                    TesouroScreen(
                        taxas = taxaSnapshot,
                        taxaOrigemLabel = origemLabel,
                        onSimulacaoRealizada = registrarSimulacao,
                        onTooltipUsado = { analyticsRepository.chartTooltipUsed() },
                    )
                }
                composable(ROTA_COMPARAR) {
                    ComparadorScreen(
                        taxas = taxaSnapshot,
                        taxaOrigemLabel = origemLabel,
                        onSimulacaoRealizada = registrarSimulacao,
                    )
                }
                composable(ROTA_GUIA) {
                    GuiaScreen(
                        onVoltar = { navController.popBackStack() },
                    )
                }
                composable(ROTA_AJUSTES) {
                    AjustesScreen(
                        consentimentoAtivo = consentimentoAdsLiberado,
                        onAlternarConsentimento = {
                            (contexto as? Activity)?.let { consentimento.abrirFormulario(it) }
                        },
                        estadoPlano = estadoPlano,
                        precoPlano = precoPlano,
                        produtoDisponivel = produtoDisponivel,
                        onComprarPlano = {
                            // P7.3: sem Play/produto o fluxo não abre — explica
                            // no snackbar em vez de "botão que não faz nada".
                            val aberto = (contexto as? Activity)
                                ?.let { planoSemAds.comprar(it) } ?: false
                            if (!aberto) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        contexto.getString(R.string.plano_sem_ads_aviso_play_store),
                                    )
                                }
                            }
                        },
                        onRestaurarCompras = {
                            planoSemAds.restaurar()
                        },
                        onAbrirGuia = {
                            // Guia é empilhada (P8): navegação comum, sem
                            // popUpTo/saveState de aba — back (seta OU gesto)
                            // retorna a Ajustes.
                            navController.navigate(ROTA_GUIA) { launchSingleTop = true }
                        },
                        onForcarCrashTeste = {
                            throw RuntimeException("Crash forçado (5 toques na versão) — P5/Crashlytics")
                        },
                    )
                }
            }
            if (rotaAtual in rotasComBanner) {
                BannerRodape(adsHabilitado = adsHabilitado)
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

/**
 * Label da bottom nav (P7.3): 11sp em TODAS as 4 abas, `maxLines=1` e
 * centralização real ([TextAlign.Center] + largura total da coluna) — a
 * "Calculadora" não transborda nem clipa na margem esquerda (P8: com 4 colunas
 * a largura por aba cresceu e o texto cabe com folga).
 */
@Composable
private fun RotuloNav(texto: String) {
    Text(
        text = texto,
        fontSize = 11.sp,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AbaCalculadora(
    taxaSnapshot: RatesSnapshot,
    origemLabel: String,
    onSimulacaoRealizada: (String, Int, Boolean) -> Unit,
    onTooltipUsado: () -> Unit,
) {
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

    val estadoFormulario = rememberSaveableStateHolder()
    estadoFormulario.SaveableStateProvider(ROTA_CALCULADORA) {
        if (serie == null) {
            SimulacaoScreen(
                onSeriePronta = { serie = it },
                onResultadoPronto = { resultado = it },
                taxaDiAnualAutomatica = taxaDiAutomatica,
                taxaDiOrigemLabel = origemLabel,
                onSimulacaoRealizada = onSimulacaoRealizada,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Back do sistema na tela de resultado: volta ao formulário
                // (mesmo efeito do botão "Voltar") em vez de sair do app —
                // a Calculadora é o start destination e o gesto sumiria.
                BackHandler {
                    serie = null
                    resultado = null
                }
                TextButton(onClick = { serie = null; resultado = null }) {
                    Text(stringResource(R.string.voltar))
                }
                resultado?.let { ResumoResultado(it) }
                EvolutionChartScreen(
                    series = chart,
                    onTooltipUsed = onTooltipUsado,
                )
            }
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