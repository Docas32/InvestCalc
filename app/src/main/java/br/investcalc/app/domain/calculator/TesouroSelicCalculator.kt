package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.roundToInt

/**
 * Calculadora de Tesouro Selic.
 *
 * - A taxa efetiva é a taxa Selic **anual** (pós-fixada), convertida para
 *   mensal por equivalência exponencial.
 * - Juros compostos mensais (aporte postecipado).
 * - IR regressivo pela tabela da renda fixa.
 *
 * ## Limitações (documentadas)
 * - Não modela o **deságio** ao vender o título antes do vencimento (marcação
 *   a mercado); assume carregamento até o vencimento.
 * - A Selic é tratada como constante durante todo o prazo (taxa vigente).
 */
object TesouroSelicCalculator {

    fun calcular(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaSelicAnual: Double,
        meses: Int,
    ): InvestmentResult {
        val taxaMensal = RateConverter.anualParaMensal(taxaSelicAnual)
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
