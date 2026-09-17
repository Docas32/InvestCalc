package br.investcalc.app.domain.rates

import java.time.LocalDate

/**
 * Série mensal do CDI — os inputs da anualização da camada de taxas.
 *
 * A API do BCB (SGS, série 12) entrega o CDI **acumulado mensal** em % a.m.;
 * para a calculadora usarmos taxa anual efetiva, os últimos 12 pontos são
 * anualizados pelo produtório de fatores `(1 + taxa_mês/100)`.
 */
data class CdiMonthlySeries(
    val pontos: List<CdiMonthlyPoint>,
) {
    /** Data mais recente da série (basquete da "atualizada em"). */
    val atualizadaEm: LocalDate? = pontos.maxOfOrNull { it.data }
}

/** Um ponto mensal da série: data e taxa efetiva em % a.m. (ex.: 0,98). */
data class CdiMonthlyPoint(
    val data: LocalDate,
    val taxaPercentualMensal: Double,
)

/**
 * Anualizador do CDI — domínio puro (JVM, sem Android, sem rede).
 *
 * - [annualizarCdi]: produtório dos fatores mensais da série → taxa **efetiva
 *   anual** em % a.a.. Ex.: 12 meses de 1,00% a.m. → 12,6825% a.a..
 * - [mensalParaAnual]: taxa mensal única → anual efetiva equivalente.
 *
 * Todos os cálculos em `Double`; série vazia/variante retorna 0.0 (nunca
 * lança; a camada de dados usa [RatesFallback] quando o BCB falha).
 */
object RateAnnualizer {

    fun annualizarCdi(serie: CdiMonthlySeries): Double {
        if (serie.pontos.isEmpty()) return 0.0
        val fator = serie.pontos.fold(1.0) { acumulado, ponto ->
            acumulado * (1.0 + ponto.taxaPercentualMensal / 100.0)
        }
        return (fator - 1.0) * 100.0
    }

    fun mensalParaAnual(taxaPercentualMensal: Double): Double {
        val fator = 1.0 + taxaPercentualMensal / 100.0
        var acumulado = 1.0
        repeat(12) { acumulado *= fator }
        return (acumulado - 1.0) * 100.0
    }
}
