package br.investcalc.app.data.rates

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Implementação da camada de taxas alimentada pelo BCB (SGS: série **4389** =
 * CDI já anualizado em % a.a., **1178** = Selic anual, **13522** = IPCA 12
 * meses) com a cadeia de prioridade pactuada na UI:
 *
 * 1. **Cache DataStore (TTL 12h)** — nunca bloqueia o cold start;
 * 2. **API BCB** (último ponto de cada série, lido direto como % a.a.);
 * 3. **Fallback embutido** ([RatesFallback]: {cdi=10.90, selic=10.90,
 *    ipca=4.50}) quando não há rede, cache ou resposta válida.
 *
 * Contrato **nunca lança** e **nunca bloqueia a UI**: toda falha de rede,
 * timeout ou rate-limit do BCB degrada silenciosamente para o cache ou para
 * o [RatesFallback] — a badge "atualizada em dd/mm" usa a data do ponto.
 * Cada série é independente: uma falha numa série não derruba as outras.
 *
 * Timeout de 5s + retry único (2 tentativas) são garantidos aqui, sem jamais
 * interromper a renderização.
 */
class BcbRatesRepository(
    private val api: BcbApi,
    private val fallback: RatesFallback = RatesFallback,
    private val quantidadeBcb: Int = 13,
    private val ttlHoras: Long = 12L,
    private val timeoutSegundos: Long = 5L,
    private val cache: TaxaCache? = null,
) : RatesRepository {

    private val mutableState = MutableStateFlow(fallback.snapshot())

    override val state: StateFlow<RatesSnapshot> = mutableState.asStateFlow()

    /** Nunca lança: qualquer exceção degrada para cache/fallback. */
    override suspend fun atualizar() {
        val atual = taxaCdiAnual()   // path garantido: cache ou fallback
        mutableState.value = atual
    }

    /** Sem bloqueio de UI: retorna sempre um snapshot (cache → BCB → fallback). */
    override suspend fun taxaCdiAnual(): RatesSnapshot {
        // 1º: cache persistido (DataStore) com TTL 12h
        val persistido = cache?.carregar()
        if (persistido != null && cacheDeveServir(persistido)) {
            mutableState.value = persistido
            return persistido
        }

        // 2º: valor desta sessão já reconciliado
        if (cacheDeveServir(mutableState.value)) return mutableState.value

        // 3º: API do BCB (timeout 5s + retry único); falha degrada p/ fallback
        return try {
            val doBcb = withTimeout(timeoutSegundos * 1_000) {
                retryUnico { buscarDoBcb() }
            }
            cache?.salvar(doBcb)
            mutableState.value = doBcb
            doBcb
        } catch (_: TimeoutCancellationException) {
            mutableState.value
        } catch (_: Exception) {
            mutableState.value
        }
    }

    private fun cacheDeveServir(cache: RatesSnapshot): Boolean {
        val em = cache.atualizadaEm ?: return false
        return java.time.Duration.between(
            em.atStartOfDay(),
            LocalDate.now().atStartOfDay(),
        ).toHours() < ttlHoras
    }

    private suspend fun retryUnico(bloco: suspend () -> RatesSnapshot): RatesSnapshot {
        var ultima: Exception? = null
        try {
            return bloco()
        } catch (e: Exception) {
            ultima = e
        }
        delay(500)
        return try {
            bloco()
        } catch (e: Exception) {
            throw ultima ?: e
        }
    }

    private suspend fun buscarDoBcb(): RatesSnapshot {
        val base = fallback.snapshot()

        // Cada série é lida de forma independente: uma falha numa série não
        // derruba as demais (per-série degrade para o valor já conhecido).
        val cdi = obterTaxa(SERIE_CDI_ANUAL)
        val selic = obterTaxa(SERIE_SELIC_ANUAL)
        val ipca = obterTaxa(SERIE_IPCA_12M)

        // SGS 4389 = CDI já anualizado em % a.a.; 1178 = Selic anual; 13522 =
        // IPCA 12 meses. Todas lidas direto, com ponto OU vírgula decimal.
        val temAtualização = cdi != null || selic != null || ipca != null
        val data = listOfNotNull(cdi?.data, selic?.data, ipca?.data)
            .maxByOrNull { it }
            ?: base.atualizadaEm
        return RatesSnapshot(
            taxaCdiAnualPercentual = cdi?.taxa ?: base.taxaCdiAnualPercentual,
            taxaSelicAnualPercentual = selic?.taxa ?: base.taxaSelicAnualPercentual,
            variacaoIpca12MesesPercentual = ipca?.taxa ?: base.variacaoIpca12MesesPercentual,
            atualizadaEm = if (temAtualização) data else base.atualizadaEm,
        )
    }

    /** Maior taxa válida (> 0) da série; null quando indisponível. */
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
