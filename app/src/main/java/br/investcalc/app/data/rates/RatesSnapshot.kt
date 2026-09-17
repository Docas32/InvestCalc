package br.investcalc.app.data.rates

import br.investcalc.app.domain.model.MonetaryValue
import java.time.LocalDate

/**
 * Retrato imutável das taxas de referência usadas pela calculadora.
 *
 * A prioridade de origem (DataStore com TTL → API do BCB → Remote Config →
 * fallback embutido) é resolvida pela implementação de [RatesRepository];
 * este modelo apenas carrega o resultado, que a UI exibe sem jamais bloquear.
 */
data class RatesSnapshot(
    /** CDI acumulado anualizado (%). Ex.: 12,6825 = 12,6825% a.a.. */
    val taxaCdiAnualPercentual: Double,
    /** Selic anual (%). Ex.: 13,75 = 13,75% a.a.. */
    val taxaSelicAnualPercentual: Double,
    /** IPCA acumulado em 12 meses (%). Ex.: 4,50 = 4,50%.. */
    val variacaoIpca12MesesPercentual: Double,
    /** Data em que as taxas foram obtidas (BCB ou cache). */
    val atualizadaEm: LocalDate?,
)
