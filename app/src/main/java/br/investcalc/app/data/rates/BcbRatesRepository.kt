package br.investcalc.app.data.rates

import br.investcalc.app.data.analytics.AnalyticsRepository
import br.investcalc.app.data.remotecfg.RemoteConfigRates
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout

/**
 * Camada de taxas alimentada pelo BCB (SGS: série **4389** = CDI anualizado
 * em % a.a., **1178** = Selic anual, **13522** = IPCA 12 meses) com a cadeia
 * de resiliência do T1-v2:
 *
 * 1. **Cache DataStore fresco** (`atualizadaEm` de hoje, TTL [TTL_HORAS]h) —
 *    servido sem consultar o BCB;
 * 2. **API do BCB** — NO MÁXIMO 1 vez por processo do app ([bcbConsultado]),
 *    somente quando o cache expirou; 1 tentativa + retry com backoff de
 *    [RETRY_BASE_MS] ± jitter aleatório (jamais loop);
 * 3. **Cache stale** — serve-stale de qualquer idade;
 * 4. **Remote Config** ([RemoteConfigRates]);
 * 5. **XML default** ([XmlDefaultsRates]);
 * 6. **Fallback embutido** ([RatesFallback]).
 *
 * Resiliência:
 * - O cold start com cache expirado aplica jitter de pico 0–3s
 *   ([picoJitterMs]) antes do refetch (dissipação de picos de meia-noite;
 *   worker agendado fica para a V2).
 * - Nunca lança e nunca bloqueia a UI: cada série é independente (uma falha
 *   não derruba as outras) e a origem efetivamente usada é registrada via
 *   [AnalyticsRepository.taxaOrigemUsada].
 */
class BcbRatesRepository(
    private val api: BcbApi,
    private val fallback: RatesFallback = RatesFallback,
    private val quantidadeBcb: Int = 13,
    private val ttlHoras: Long = TTL_HORAS,
    private val timeoutSegundos: Long = 5L,
    private val cache: TaxaCache? = null,
    private val remoteConfig: RemoteConfigRates? = null,
    private val xmlDefaults: XmlDefaultsRates? = null,
    private val analyticsRepository: AnalyticsRepository? = null,
    private val picoJitterMs: () -> Long = { Random.nextLong(0L, PICO_JITTER_MAX_MS + 1L) },
    private val retryAtrasoMs: () -> Long = { RETRY_BASE_MS + Random.nextLong(-RETRY_JITTER_MS, RETRY_JITTER_MS + 1L) },
) : RatesRepository {

    private val mutableState = MutableStateFlow(fallback.snapshot())

    /** Garantia do contrato "máx. 1x por processo": já houve tentativa de BCB. */
    private var bcbConsultado = false

    override val state: StateFlow<RatesSnapshot> = mutableState.asStateFlow()

    /** Nunca lança: qualquer exceção degrada para cache/RC/XML/fallback. */
    override suspend fun atualizar() {
        val atual = taxaCdiAnual()   // path garantido da cadeia completa
        mutableState.value = atual
    }

    /**
     * Resolve a cadeia de prioridade sem bloquear a UI (tudo suspenso).
     * Sempre retorna um [RatesSnapshot] e registra a origem usada no Analytics.
     */
    override suspend fun taxaCdiAnual(): RatesSnapshot {
        // Serve-stale imediato: o valor persistido (qualquer idade) já é
        // publicado antes de qualquer rede — a UI nunca espera.
        val persistido = cache?.carregar()
        if (persistido != null) mutableState.value = persistido

        // Máx. 1 busca por processo: chamadas seguintes reutilizam o estado.
        if (bcbConsultado) return mutableState.value

        // 1º: cache fresco (< TTL) — o BCB não é consultado.
        if (persistido != null && cacheFresca(persistido)) {
            registrar(TaxSource.CACHE_FRESCA)
            return persistido
        }

        // 2º: cache expirado → refetch do cold start com jitter de pico
        // (0–3s, randomizado) para dissipar picos em dias úteis.
        if (persistido != null) delay(picoJitterMs())
        bcbConsultado = true

        val doBcb = tentarBcb()
        if (doBcb != null) {
            val comSelamento = doBcb.copy(salvoEm = LocalDate.now())
            cache?.salvar(comSelamento)
            mutableState.value = comSelamento
            registrar(TaxSource.BCB)
            return comSelamento
        }

        // 3º: BCB falhou → serve-stale, depois Remote Config e XML.
        if (persistido != null) {
            registrar(TaxSource.CACHE_STALE)
            return persistido
        }
        remoteConfig?.carregar()?.let {
            registrar(TaxSource.REMOTE_CONFIG)
            return it
        }
        xmlDefaults?.carregar()?.let {
            registrar(TaxSource.XML)
            return it
        }

        registrar(TaxSource.FALLBACK)
        return fallback.snapshot()
    }

    /** 1 tentativa + retry com backoff; jamais loop. `null` = BCB indisponível. */
    private suspend fun tentarBcb(): RatesSnapshot? {
        val primeira = tentativaComTimeout()
        if (primeira != null) return primeira
        delay(retryAtrasoMs())
        return tentativaComTimeout()
    }

    private suspend fun tentativaComTimeout(): RatesSnapshot? {
        return try {
            withTimeout(timeoutSegundos * 1_000) { buscarDoBcb() }
        } catch (_: TimeoutCancellationException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheFresca(candidato: RatesSnapshot): Boolean {
        // TTL medido pela data de PERSISTÊNCIA (salvoEm) — ver RatesSnapshot;
        // cache legado sem salvoEm usa a data do ponto como aproximação.
        val em = candidato.salvoEm ?: candidato.atualizadaEm ?: return false
        return java.time.Duration.between(
            em.atStartOfDay(),
            LocalDate.now().atStartOfDay(),
        ).toHours() < ttlHoras
    }

    private suspend fun buscarDoBcb(): RatesSnapshot {
        val base = fallback.snapshot()

        // Cada série é lida de forma independente: uma falha numa série não
        // derruba as demais (per-série degrade para o valor já conhecido).
        val cdi = obterTaxa(SERIE_CDI_ANUAL)
        val selic = obterTaxa(SERIE_SELIC_ANUAL)
        val ipca = obterTaxa(SERIE_IPCA_12M)

        // Falha completa = BCB indisponível (provoca o retry e a degradação
        // para cache stale / RC / XML da cadeia, e NÃO grava fallback no cache).
        if (cdi == null && selic == null && ipca == null) {
            throw IOException("BCB indisponível em todas as séries")
        }

        // SGS 4389 = CDI já anualizado em % a.a.; 1178 = Selic anual; 13522 =
        // IPCA 12 meses. Todas lidas direto, com ponto OU vírgula decimal.
        val data = listOfNotNull(cdi?.data, selic?.data, ipca?.data)
            .maxByOrNull { it }
            ?: base.atualizadaEm
        return RatesSnapshot(
            taxaCdiAnualPercentual = cdi?.taxa ?: base.taxaCdiAnualPercentual,
            taxaSelicAnualPercentual = selic?.taxa ?: base.taxaSelicAnualPercentual,
            variacaoIpca12MesesPercentual = ipca?.taxa ?: base.variacaoIpca12MesesPercentual,
            atualizadaEm = if (cdi != null || selic != null || ipca != null) data else base.atualizadaEm,
        )
    }

    /** Maior taxa válida (> 0) da série; null quando a série está indisponível. */
    private suspend fun obterTaxa(codigoSerie: Int): TaxaDaSerie? {
        return try {
            val pontosRaw = api.obterSerie(codigoSerie, quantidadeBcb)
            val maisRecente = pontosRaw.maxByOrNull { parsesData(it.data) }
                ?: return null
            val taxa = maisRecente.valor.parseDecimalPercentual()
            if (taxa <= 0.0) null else TaxaDaSerie(taxa, parsesData(maisRecente.data))
        } catch (_: Exception) {
            null
        }
    }

    private data class TaxaDaSerie(val taxa: Double, val data: LocalDate)

    private fun registrar(origem: TaxSource) {
        analyticsRepository?.taxaOrigemUsada(origem.analytics)
    }

    private fun parsesData(texto: String): LocalDate {
        return try {
            LocalDate.parse(texto, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
}

/**
 * Normaliza o decimal do BCB para `Double`. O `formato=json` pode vir com
 * ponto (`"13.90"`) ou vírgula (`"13,90"`); quando há ambos, o ponto é
 * separador de milhar e a vírgula é decimal.
 */
private fun String.parseDecimalPercentual(): Double {
    val texto = trim()
    val normalizado = when {
        texto.contains(',') && texto.contains('.') -> texto.replace(".", "").replace(',', '.')
        texto.contains(',') -> texto.replace(',', '.')
        else -> texto
    }
    return normalizado.toDoubleOrNull() ?: 0.0
}

internal const val SERIE_CDI_ANUAL = 4389
internal const val SERIE_SELIC_ANUAL = 1178
internal const val SERIE_IPCA_12M = 13522

/** TTL do cache de taxas (as taxas mudam no máximo 1x por dia útil). */
internal const val TTL_HORAS = 24L

/** Backoff do retry único: 2s ± [RETRY_JITTER_MS] de jitter aleatório. */
internal const val RETRY_BASE_MS = 2_000L
internal const val RETRY_JITTER_MS = 500L

/** Jitter de pico aplicado ao refetch do cold start com cache expirado. */
internal const val PICO_JITTER_MAX_MS = 3_000L