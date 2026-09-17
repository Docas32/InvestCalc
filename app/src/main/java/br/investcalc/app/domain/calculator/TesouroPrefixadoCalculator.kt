package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.roundToInt

/**
 * Calculadora de Tesouro Prefixado.
 *
 * - A taxa efetiva é a taxa **prefixada anual** (definida na compra),
 *   convertida para mensal por equivalência exponencial.
 * - Juros compostos mensais (aporte postecipado).
 * - IR regressivo.
 *
 * ## Limitações (documentadas)
 * - Não modela o **deságio** por venda antecipada (marcação a mercado).
 * - Taxa contratada na compra é fixa; não há reajuste no prazo.
 */
object TesouroPrefixadoCalculator {

    fun calcular(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaPrefixadaAnual: Double,
        meses: Int,
    ): InvestmentResult {
        val taxaMensal = RateConverter.anualParaMensal(taxaPrefixadaAnual)
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
