package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.roundToInt

/**
 * Calculadora de LCI/LCA (Letras de Crédito Imobiliário/do Agronegócio).
 *
 * - A taxa efetiva é `taxaCdiAnual * (percentualCdi / 100)`.
 * - Juros compostos mensais (aporte postecipado).
 * - **Isento de IR** para pessoa física — alíquota sempre 0,0%.
 */
object LciLcaCalculator {

    private const val ISENTO = 0.0

    fun calcularComCDI(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        percentualCdi: Double,
        taxaCdiAnual: Double,
        meses: Int,
    ): InvestmentResult {
        val taxaEfetivaAnual = taxaCdiAnual * (percentualCdi / 100.0)
        val taxaMensal = RateConverter.anualParaMensal(taxaEfetivaAnual)
        return calcularComTaxaMensal(valorInicial, aporteMensal, taxaMensal, meses)
    }

    fun calcularComTaxaMensal(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaMensalEfetiva: Double,
        meses: Int,
    ): InvestmentResult {
        val totalInvestido = valorInicial.plus(aporteMensal.times(meses.toDouble()))
        val valorBruto = CompoundGrowth.montante(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            taxaMensal = taxaMensalEfetiva,
            meses = meses,
        )
        val rendimentoBruto = valorBruto.minus(totalInvestido)
        return InvestmentResult(
            valorInvestido = totalInvestido,
            valorBruto = valorBruto,
            rendimentoBruto = rendimentoBruto,
            aliquotaIr = ISENTO,
            irPago = MonetaryValue.ZERO,
            valorLiquido = valorBruto,
            rendimentoLiquido = rendimentoBruto,
            diasCorridos = (meses.toDouble() * 30.44).roundToInt(),
        )
    }
}
