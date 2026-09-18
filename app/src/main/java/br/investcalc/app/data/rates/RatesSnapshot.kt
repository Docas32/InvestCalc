package br.investcalc.app.data.rates

import br.investcalc.app.domain.model.MonetaryValue
import java.time.LocalDate

/**
 * Retrato imutável das taxas de referência usadas pela calculadora.
 *
 * A prioridade de origem (DataStore fresco < 24h → API do BCB → cache stale
 * → Remote Config → XML → fallback embutido) é resolvida pela implementação
 * de [RatesRepository]; este modelo apenas carrega o resultado, que a UI
 * exibe sem jamais bloquear.
 */
data class RatesSnapshot(
    /** CDI acumulado anualizado (%). Ex.: 12,6825 = 12,6825% a.a.. */
    val taxaCdiAnualPercentual: Double,
    /** Selic anual (%). Ex.: 13,75 = 13,75% a.a.. */
    val taxaSelicAnualPercentual: Double,
    /** IPCA acumulado em 12 meses (%). Ex.: 4,50 = 4,50%.. */
    val variacaoIpca12MesesPercentual: Double,
    /** Data em que as taxas foram obtidas (ponto BCB ou cache). */
    val atualizadaEm: LocalDate?,
    /**
     * Data em que o snapshot foi PERSISTIDO no cache — o TTL de 24h é medido
     * por aqui, não pela data do ponto: no fim de semana o ponto do BCB fica
     * na sexta e o cache continua fresco, sem refetch desnecessário.
     */
    val salvoEm: LocalDate? = null,
)
