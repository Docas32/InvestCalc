package br.investcalc.app.domain.calculator

import kotlin.math.pow

/**
 * Conversões de taxas de juros entre períodos (anual <-> mensal) e
 * anualização de séries de taxas mensais (juros compostos).
 *
 * Todas as taxas são valores fracionários (ex.: 0.01 = 1%).
 * Convenção: equivalência exponencial — a taxa anual efetiva é o produto
 * de 12 capitalizações mensais.
 */
object RateConverter {

    /**
     * Converte uma taxa efetiva anual para a taxa efetiva mensal equivalente:
     * `mensal = (1 + anual)^(1/12) - 1`.
     */
    fun anualParaMensal(taxaAnual: Double): Double =
        (1.0 + taxaAnual).pow(1.0 / 12.0) - 1.0

    /**
     * Converte uma taxa efetiva mensal para a taxa efetiva anual equivalente:
     * `anual = (1 + mensal)^12 - 1`.
     */
    fun mensalParaAnual(taxaMensal: Double): Double =
        (1.0 + taxaMensal).pow(12.0) - 1.0

    /**
     * Anualiza uma série de taxas mensais (possivelmente variáveis) em uma
     * única taxa efetiva anual acumulada: `prod(1 + m_i) - 1`.
     */
    fun anualizar(taxasMensais: List<Double>): Double =
        taxasMensais.fold(1.0) { acumulado, taxa -> acumulado * (1.0 + taxa) } - 1.0
}
