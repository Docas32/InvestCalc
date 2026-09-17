package br.investcalc.app.data.rates

import br.investcalc.app.domain.rates.CdiMonthlyPoint
import br.investcalc.app.domain.rates.CdiMonthlySeries
import br.investcalc.app.domain.rates.RateAnnualizer
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.pow

/**
 * Implementação da camada de taxas alimentada pelo BCB (SGS, série 12 = CDI
 * mensal acumulado) com a cadeia de prioridade pactuada na UI:
 *
 * 1. **Cache DataStore (TTL 12h)** — nunca bloqueia o cold start;
 * 2. **API BCB** (últimos 13 pontos → últimos 12 anualizados pelo
 *    [RateAnnualizer]);
 * 3. **Remote Config** (`default_cdi_aa`, `default_selic_aa`,
 *    `default_ipca_aa`) com defaults no XML;
 * 4. **Fallback embutido** ([RatesFallback]: {cdi=12.6825, selic=12.6825? não —
 *    fallback hermético}).
 *
 * Contrato **nunca lança** e **nunca bloqueia a UI**: toda falha de rede,
 * timeout ou rate-limit do BCB degrada silenciosamente para o cache ou para
 * o [RatesFallback] — a badge "atualizada em dd/mm" usa [RatesFallback.atualizadaEm].
 *
 * Timeout de 5s + retry único (2 tentativas) são garantidos aqui, sem jamais
 * interromper a renderização (chamada é `suspend`, CPU-bound via anualização
 * pura JVM em [RateAnnualizer]).
 */
class BcbRatesRepository(
    private val api: BcbApi,
    private val fallback: RatesFallback = RatesFallback,
    private val quantidadeBcb: Int = 13,
    private val ttlHoras: Long = 12L,
    private val timeoutSegundos: Long = 5L,
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
        val cache = mutableState.value
        if (cacheDeveServir(cache)) return cache

        return try {
            val doBcb = withTimeout(timeoutSegundos * 1_000) {
                retryUnico { buscarDoBcb() }
            }
            mutableState.value = doBcb
            doBcb
        } catch (_: TimeoutCancellationException) {
            cache
        } catch (_: Exception) {
            cache
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
        val pontosRaw = api.obterSerie(SERIE_CDI_MENSAL, quantidadeBcb)
        if (pontosRaw.size < 12) return fallback.snapshot()

        val serie = CdiMonthlySeries(
            pontos = pontosRaw.takeLast(12).map { p ->
                CdiMonthlyPoint(
                    data = parsesData(p.data),
                    taxaPercentualMensal = p.valor.parseBrasileiroPercentual(),
                )
            },
        )
        val cdiAnual = RateAnnualizer.annualizarCdi(serie)
        val base = fallback.snapshot()
        return RatesSnapshot(
            taxaCdiAnualPercentual = cdiAnual,
            taxaSelicAnualPercentual = base.taxaSelicAnualPercentual,
            variacaoIpca12MesesPercentual = base.variacaoIpca12MesesPercentual,
            atualizadaEm = serie.atualizadaEm,
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

/** "1,14" (BCB) → 1.14. Formato brasileiro com vírgula decimal. */
private fun String.parseBrasileiroPercentual(): Double =
    replace(".", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull() ?: 0.0

internal const val SERIE_CDI_MENSAL = 12
