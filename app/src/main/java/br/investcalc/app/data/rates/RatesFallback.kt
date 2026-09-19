package br.investcalc.app.data.rates

import java.time.LocalDate

/**
 * Fallback determinístico da camada de taxas — valores canônicos de exemplo,
 * usados quando não há rede, cache ou ajuste remoto. Nunca lança.
 *
 * Contrato P4 (cadência cache → API → remote config → XML → fallback, sem
 * qualquer 10,90 remanescente): CDI 13,90% a.a., Selic 13,90% a.a. (série
 * 1178) e IPCA 12m 4,50% (série 13522).
 */
object RatesFallback {

    const val TAXA_CDI_ANUAL_PERCENTUAL = 13.90
    const val TAXA_SELIC_ANUAL_PERCENTUAL = 13.90
    const val IPCA_12_MESES_PERCENTUAL = 4.50

    fun snapshot(): RatesSnapshot = RatesSnapshot(
        taxaCdiAnualPercentual = TAXA_CDI_ANUAL_PERCENTUAL,
        taxaSelicAnualPercentual = TAXA_SELIC_ANUAL_PERCENTUAL,
        variacaoIpca12MesesPercentual = IPCA_12_MESES_PERCENTUAL,
        atualizadaEm = null,
    )
}
