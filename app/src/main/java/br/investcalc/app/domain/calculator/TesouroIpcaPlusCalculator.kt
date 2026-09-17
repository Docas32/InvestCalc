package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.roundToInt

/**
 * Calculadora de Tesouro IPCA+.
 *
 * - A taxa efetiva anual é **composta**: `(1 + ipca) * (1 + taxaReal) - 1`,
 *   onde `taxaReal` é o cupom contratado e `ipca` a inflação projetada.
 * - Convertida para mensal por equivalência exponencial.
 * - Juros compostos mensais (aporte postecipado).
 * - IR regressivo.
 *
 * ## Limitações (documentadas)
 * - O IPCA é tratado como constante (projeção) durante todo o prazo.
 * - Não modela deságio por venda antecipada.
 */
object TesouroIpcaPlusCalculator {

    /**
     * Taxa anual composta a partir do IPCA e da taxa real contratada.
     */
    fun taxaAnualComposta(ipcaAnual: Double, taxaRealAnual: Double): Double =
        (1.0 + ipcaAnual) * (1.0 + taxaRealAnual) - 1.0

    fun calcular(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        ipcaAnual: Double,
        taxaRealAnual: Double,
        meses: Int,
    ): InvestmentResult {
        val taxaEfetivaAnual = taxaAnualComposta(ipcaAnual, taxaRealAnual)
        val taxaMensal = RateConverter.anualParaMensal(taxaEfetivaAnual)
        val diasCorridos = (meses.toDouble() * 30.44).roundToInt()
        val aliquota = IncomeTax.aliquotaPorDias(diasCorridos)
        val totalInvestido = valorInicial.plus(aporteMensal.times(meses.toDouble()))
        val valorBruto = CompoundGrowth.montante(valorInicial, aporteMensal, taxaMensal, meses)
        val rendimentoBruto = valorBruto.minus(totalInvestido)
        val ir = IncomeTax.calcularIrSobreAliquota(rendimentoBruto, aliquota)
        val valorLiquido = valorBruto.minus(ir)
        return InvestmentResult(
            valorInvestido = totalInvestido,
            valorBruto = valorBruto,
            rendimentoBruto = rendimentoBruto,
            aliquotaIr = aliquota,
            irPago = ir,
            valorLiquido = valorLiquido,
            rendimentoLiquido = valorLiquido.minus(totalInvestido),
            diasCorridos = diasCorridos,
        )
    }
}
