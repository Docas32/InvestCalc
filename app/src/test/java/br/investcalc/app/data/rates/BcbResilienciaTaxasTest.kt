package br.investcalc.app.data.rates

import br.investcalc.app.data.analytics.AnalyticsRepository
import br.investcalc.app.data.remotecfg.RemoteConfigRates
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * T1-v2 — RESILIÊNCIA DE TAXAS, provada contra o MockWebServer com a pilha
 * HTTP real (OkHttp + Retrofit via [BcbApiFactory.criar]).
 *
 * Contratos:
 * - Busca ao BCB NO MÁXIMO 1x por processo e somente com cache > 24h;
 * - 1 retry de 2s ± jitter, jamais loop;
 * - Serve-stale: falha de rede serve o cache de qualquer idade antes de RC/XML;
 * - Jitter de pico 0–3s apenas quando o cache expira no cold start;
 * - Cadeia: cache fresco → BCB → cache stale → RC → XML → fallback, com a
 *   origem sempre registrada em Analytics.
 */
class BcbResilienciaTaxasTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun apiReal() = BcbApiFactory.criar(server.url("/").toString())

    private val cacheComum = CacheEmMemoria()

    @Test
    fun `dois cold starts seguidos executam exatamente uma busca ao BCB`() = runBlocking {
        val tráfego = Tráfego()
        server.dispatcher = tráfego.ok()

        // Cold start #1: cache vazio → busca única (3 séries).
        val repo1 = BcbRatesRepository(api = apiReal(), cache = cacheComum)
        val s1 = repo1.taxaCdiAnual()
        assertEquals(13.90, s1.taxaCdiAnualPercentual, 1e-9)
        assertEquals(10.65, s1.taxaSelicAnualPercentual, 1e-9)
        assertEquals(4.50, s1.variacaoIpca12MesesPercentual, 1e-9)
        assertEquals(1, cacheComum.salvas)

        // Cold start #2: cache de hoje → sem nenhuma chamada ao BCB.
        val repo2 = BcbRatesRepository(api = apiReal(), cache = cacheComum)
        val s2 = repo2.taxaCdiAnual()
        assertEquals(s1, s2)

        assertEquals("uma busca = 1 request por série", 3, server.requestCount)
        assertEquals(listOf(SERIE_CDI_ANUAL, SERIE_SELIC_ANUAL, SERIE_IPCA_12M), tráfego.sériesChamadas())
    }

    @Test
    fun `cache com menos de 24h jamais consulta o BCB`() = runBlocking {
        val analítica = FakeAnalytics()
        cacheComum.seedar(RatesSnapshot(9.9, 9.9, 3.3, LocalDate.now(), LocalDate.now()))
        val tráfego = Tráfego()
        server.dispatcher = tráfego.explodir()

        val repo = BcbRatesRepository(api = apiReal(), cache = cacheComum, analyticsRepository = analítica)
        val s = repo.taxaCdiAnual()

        assertEquals(9.9, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(0, server.requestCount)
        assertEquals(listOf("cache_fresca"), analítica.origens)
    }

    @Test
    fun `429 depois 500 deixa o app funcional com cache stale e registra origem`() = runBlocking {
        val analítica = FakeAnalytics()
        cacheComum.seedar(RatesSnapshot(9.9, 9.9, 3.3, LocalDate.now().minusDays(2), LocalDate.now().minusDays(2)))
        val tráfego = Tráfego()
        server.dispatcher = tráfego.primeiroFetch429depois500()

        val repo = BcbRatesRepository(
            api = apiReal(),
            cache = cacheComum,
            analyticsRepository = analítica,
            picoJitterMs = { 0L },
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()

        // Serve-stale: valor do cache de qualquer idade, NUNCA o fallback.
        assertEquals("cache stale cdi", 9.9, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals("cache stale selic", 9.9, s.taxaSelicAnualPercentual, 1e-9)
        assertEquals("cache stale ipca", 3.3, s.variacaoIpca12MesesPercentual, 1e-9)
        assertEquals("falha não grava cache", 0, cacheComum.salvas)
        assertEquals("tentativa + retry = 2 buscas x 3 séries", 6, server.requestCount)
        assertEquals(listOf("cache_stale"), analítica.origens)
    }

    @Test
    fun `429 no retry recupera e atualiza o cache`() = runBlocking {
        val analítica = FakeAnalytics()
        val tráfego = Tráfego()
        server.dispatcher = tráfego.primeiroFetch429depoisOk()

        val repo = BcbRatesRepository(
            api = apiReal(),
            cache = cacheComum,
            analyticsRepository = analítica,
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()

        assertEquals(13.90, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(6, server.requestCount)
        assertEquals(1, cacheComum.salvas)
        assertEquals(listOf("bcb"), analítica.origens)
    }

    @Test
    fun `falha registrada uma unica vez por processo nao re consulta o BCB`() = runBlocking {
        val analítica = FakeAnalytics()
        val tráfego = Tráfego()
        server.dispatcher = tráfego.sempre500()

        val repo = BcbRatesRepository(
            api = apiReal(),
            analyticsRepository = analítica,
            retryAtrasoMs = { 0L },
        )
        repo.taxaCdiAnual()
        val totalApósPrimeira = server.requestCount
        repo.taxaCdiAnual()
        val totalApósSegunda = server.requestCount

        assertEquals("tentativa + retry", 6, totalApósPrimeira)
        assertEquals("sem loop de retry", totalApósPrimeira, totalApósSegunda)
        assertEquals(listOf("fallback"), analítica.origens)
    }

    @Test
    fun `sem cache stale usa remote config antes do xml`() = runBlocking {
        val analítica = FakeAnalytics()
        server.dispatcher = Tráfego().sempre500()

        val repo = BcbRatesRepository(
            api = apiReal(),
            remoteConfig = RcFixo(RatesSnapshot(12.0, 12.0, 4.2, null)),
            xmlDefaults = XmlFixo(RatesSnapshot(11.0, 11.0, 4.0, null)),
            analyticsRepository = analítica,
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()

        assertEquals(12.0, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(listOf("remote_config"), analítica.origens)
    }

    @Test
    fun `sem cache e sem remote config usa o xml default`() = runBlocking {
        val analítica = FakeAnalytics()
        server.dispatcher = Tráfego().sempre500()

        val repo = BcbRatesRepository(
            api = apiReal(),
            xmlDefaults = XmlFixo(RatesSnapshot(11.5, 11.5, 4.1, null)),
            analyticsRepository = analítica,
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()

        assertEquals(11.5, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(listOf("xml"), analítica.origens)
    }

    @Test
    fun `sem cache sem remote config e sem xml usa o fallback embutido`() = runBlocking {
        val analítica = FakeAnalytics()
        server.dispatcher = Tráfego().sempre500()

        val repo = BcbRatesRepository(
            api = apiReal(),
            analyticsRepository = analítica,
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()

        assertEquals(RatesFallback.TAXA_CDI_ANUAL_PERCENTUAL, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(listOf("fallback"), analítica.origens)
    }

    @Test
    fun `jitter de pico 0-3s so e aplicado quando o cache expira`() = runBlocking {
        var picos = 0
        cacheComum.seedar(RatesSnapshot(9.9, 9.9, 3.3, LocalDate.now().minusDays(2), LocalDate.now().minusDays(2)))
        server.dispatcher = Tráfego().ok()

        val repo = BcbRatesRepository(
            api = apiReal(),
            cache = cacheComum,
            picoJitterMs = { picos++; 0L },
        )
        repo.taxaCdiAnual()
        assertEquals("cache expirado → jitter de pico", 1, picos)
    }

    @Test
    fun `jitter de pico nao roda sem cache nem com cache fresco`() = runBlocking {
        server.dispatcher = Tráfego().ok()

        var picosSemCache = 0
        BcbRatesRepository(
            api = apiReal(),
            picoJitterMs = { picosSemCache++; 0L },
        ).taxaCdiAnual()
        assertEquals(0, picosSemCache)

        var picosFrescos = 0
        cacheComum.seedar(RatesSnapshot(9.9, 9.9, 3.3, LocalDate.now(), LocalDate.now()))
        BcbRatesRepository(
            api = apiReal(),
            cache = cacheComum,
            picoJitterMs = { picosFrescos++; 0L },
        ).taxaCdiAnual()
        assertEquals(0, picosFrescos)
    }

    @Test
    fun `backoff e aplicado exatamente uma vez entre tentativa e retry`() = runBlocking {
        var atrasos = 0
        server.dispatcher = Tráfego().primeiroFetch429depoisOk()

        val repo = BcbRatesRepository(
            api = apiReal(),
            retryAtrasoMs = { atrasos++; 0L },
        )
        val s = repo.taxaCdiAnual()

        assertEquals(13.90, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals("1 retry com backoff", 1, atrasos)
    }

    private class CacheEmMemoria : TaxaCache {
        var snapshot: RatesSnapshot? = null
        var salvas = 0
        override suspend fun carregar(): RatesSnapshot? = snapshot

        override suspend fun salvar(atual: RatesSnapshot) {
            snapshot = atual.copy(salvoEm = atual.salvoEm ?: LocalDate.now())
            salvas++
        }

        /** Semear um valor pré-existente sem contar como gravação. */
        fun seedar(pré: RatesSnapshot) {
            snapshot = pré
        }
    }

    private class FakeAnalytics : AnalyticsRepository {
        val origens = mutableListOf<String>()
        override fun calculationPerformed(type: String, months: Int, hasMonthlyContribution: Boolean) {}
        override fun taxaOrigemUsada(origem: String) {
            origens += origem
        }

        override fun chartTooltipUsed() {}
        override fun screenViewed(screen: String) {}
    }

    private class RcFixo(private val valor: RatesSnapshot?) : RemoteConfigRates {
        override suspend fun carregar(): RatesSnapshot? = valor
    }

    private class XmlFixo(private val valor: RatesSnapshot?) : XmlDefaultsRates {
        override fun carregar(): RatesSnapshot? = valor
    }

    /** Registra cada request (fetch = 3 séries) e roteia a resposta desejada. */
    private class Tráfego {

        private val pedidos = AtomicInteger()
        val séries = CopyOnWriteArrayList<Int>()

        private fun extrairSérie(path: String): Int? =
            Regex("bcdata\\.sgs\\.(\\d+)").find(path)?.groupValues?.get(1)?.toInt()

        private fun respostaPorSérie(codigo: Int): MockResponse {
            val hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            val valor = when (codigo) {
                SERIE_CDI_ANUAL -> "13.90"
                SERIE_SELIC_ANUAL -> "10.65"
                else -> "4.50"
            }
            return MockResponse().setResponseCode(200).setBody("""[{"data":"$hoje","valor":"$valor"}]""")
        }

        fun ok(): Dispatcher = dispatcher { _, codigo ->
            respostaPorSérie(codigo)
        }

        fun explodir(): Dispatcher = dispatcher { _, _ ->
            throw IllegalStateException("BCB não deveria ser consultado")
        }

        /** Fetch 1 (3 séries) → 429; fetch 2 (retry) → 500. */
        fun primeiroFetch429depois500(): Dispatcher = dispatcher { fetch, códigoSérie ->
            if (fetch == 0) MockResponse().setResponseCode(429).setBody("rate limit")
            else MockResponse().setResponseCode(500).setBody("erro interno")
        }

        /** Fetch 1 → 429; fetch 2 (retry) → 200 com a série. */
        fun primeiroFetch429depoisOk(): Dispatcher = dispatcher { fetch, códigoSérie ->
            if (fetch == 0) MockResponse().setResponseCode(429).setBody("rate limit")
            else respostaPorSérie(códigoSérie)
        }

        fun sempre500(): Dispatcher = dispatcher { _, _ ->
            MockResponse().setResponseCode(500).setBody("erro interno")
        }

        private fun dispatcher(resposta: (fetch: Int, série: Int) -> MockResponse): Dispatcher =
            object : Dispatcher() {
                @Synchronized
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val série = extrairSérie(request.path ?: "") ?: 0
                    séries += série
                    val fetch = pedidos.getAndIncrement() / 3
                    return resposta(fetch, série)
                }
            }

        fun sériesChamadas(): List<Int> = séries.toList()
    }
}