package br.investcalc.app.domain.model.rates

import java.time.LocalDate

/**
 * Retrato imutável das taxas de referência usadas na calculadora.
 *
 * Em produção os valores vêm da API do BCB (séries 12 = CDI mensal,
 * 1178 = Selic anualizada, 13522 = IPCA 12 meses); o [percentualCdi] é a
 * preferência do usuário (ex.: 100 = 100% do CDI). A priidade da fonte
 * (cache DataStore → BCB → Remote Config → fallback) é resolvida na camada
 * de dados, nunca aqui.
 */
data class RatesSnapshot(
    /** Taxa do CDI acumulada nos últimos 12 meses, efetiva a.a. (12,6825 = 12,6825%). */
    val cdiAnualPercentual: Double,
    /** Taxa Selic anualizada (11,25 = 11,25% a.a.). */
    val selicAnualPercentual: Double,
    /** IPCA acumulado em 12 meses (4,50 = 4,50%). */
    val ipca12MesesPercentual: Double,
    /** Data da última atualização dos valores (não nula quando vieram do BCB). */
    val atualizadaEm: LocalDate?,
    /** Fonte da qual o valor foi lido (bcb, cache, remote_config ou fallback). */
    val fonte: FonteTaxas,
)

/** Fonte de onde o retrato foi obtido, preservando transparência na UI. */
enum class FonteTaxas {
    /** Taxas obtidas em tempo real da API do BCB. */
    BCB,

    /** Taxas cristalizadas no cache local (DataStore, TTL 12h). */
    CACHE,

    /** Taxas vindas do Remote Config (valores de exemplo ajustáveis). */
    REMOTE_CONFIG,

    /** Taxas padrão embutidas quando todas as fontes acima falham. */
    FALLBACK,
}

/** Valores padrão embutidos, usados quando nenhuma fonte dinâmica responde. */
object TaxasFallback {
    const val CDI_ANUAL_PERCENTUAL = 12.6825
    const val SELIC_ANUAL_PERCENTUAL = 11.25
    const val IPCA_12M_PERCENTUAL = 4.50

    fun snapshot(): RatesSnapshot = RatesSnapshot(
        cdiAnualPercentual = CDI_ANUAL_PERCENTUAL,
        selicAnualPercentual = SELIC_ANUAL_PERCENTUAL,
        ipca12MesesPercentual = IPCA_12M_PERCENTUAL,
        atualizadaEm = null,
        fonte = FonteTaxas.FALLBACK,
    )
}
