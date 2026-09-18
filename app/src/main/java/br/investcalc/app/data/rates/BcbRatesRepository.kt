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
 * Implementação da camada de taxas alimentada pelo BCB (SGS, série **4389** =
 * CDI já anualizado em % a.a.) com a cadeia de prioridade pactuada na UI:
 *
 * 1. **Cache DataStore (TTL 12h)** — nunca bloqueia o cold start;
 * 2. **API BCB** (último ponto da série 4389, lido direto como % a.a.);
 * 3. **Fallback embutido** ([RatesFallback]: {cdi=10.90, selic=11.25,
 *    ipca=4.50}) quando não há rede, cache ou resposta válida.
 *
 * Contrato **nunca lança** e **nunca bloqueia a UI**: toda falha de rede,
 * timeout ou rate-limit do BCB degrada silenciosamente para o cache ou para
 * o [RatesFallback] — a badge "atualizada em dd/mm" usa a data do ponto.
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
        val pontosRaw = api.obterSerie(SERIE_CDI_ANUAL, quantidadeBcb)
        val maisRecente = pontosRaw.maxByOrNull { parsesData(it.data) }
            ?: return fallback.snapshot()

        // SGS 4389 = CDI já anualizado em % a.a. (ex.: "13.90"), lido direto.
        val cdiAnual = maisRecente.valor.parseDecimalPercentual()
        if (cdiAnual <= 0.0) return fallback.snapshot()

        val base = fallback.snapshot()
        return RatesSnapshot(
            taxaCdiAnualPercentual = cdiAnual,
            taxaSelicAnualPercentual = base.taxaSelicAnualPercentual,
            variacaoIpca12MesesPercentual = base.variacaoIpca12MesesPercentual,
            atualizadaEm = parsesData(maisRecente.data),
        )
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
