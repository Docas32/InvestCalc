package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue

/**
 * Comparador CDB × LCI/LCA sobre o **mesmo** DI anual percentual (ex.: 13,90):
 *
 * - **CDB**: [CdbCalculator] com IR regressivo por meses;
 * - **LCI/LCA**: [LciLcaCalculator] isenta (alíquota 0) a um percentual do DI;
 * - **equivalência educativa**: `pctLCI / (1 - IR do prazo)` — o percentual de
 *   CDB que, sob o IR regressivo, se iguala a uma LCI àquele pct do DI.
 *
 * Hermética (JVM pura, sem rede, nunca lança); os dois cálculos reutilizam o
 * motor composto [CompoundGrowth] existente.
 */
data class ComparadorResultado(
    val cdb: InvestmentResult,
    val lci: InvestmentResult,
    /** Percentual de CDB que empata com a LCI no prazo (ex.: 112.5 = 112,5%). */
    val percentualCdbEquivalente: Double,
) {
    val cdbLiquido: MonetaryValue get() = cdb.valorLiquido
    val lciLiquido: MonetaryValue get() = lci.valorLiquido
}

object ComparadorCalculator {

    fun comparar(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        percentualCdb: Double,
        percentualLci: Double,
        taxaDiAnualPercentual: Double,
        meses: Int,
    ): ComparadorResultado {
        val cdb = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            percentualCdi = percentualCdb,
            taxaDiAnualPercentual = taxaDiAnualPercentual,
            meses = meses,
        )
        val lci = LciLcaCalculator.calcularComCDI(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            percentualCdi = percentualLci,
            taxaCdiAnual = taxaDiAnualPercentual / 100.0,
            meses = meses,
        )
        // LCI é isenta: pctCDB * (1 - IR) == pctLCI → pctCDB == pctLCI / (1 - IR).
        val cdbEquivalente = percentualLci / (1.0 - cdb.aliquotaIr)
        return ComparadorResultado(cdb = cdb, lci = lci, percentualCdbEquivalente = cdbEquivalente)
    }
}