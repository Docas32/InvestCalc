package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.roundToInt

/**
 * Calculadora de CDB (Certificado de Depósito Bancário).
 *
 * - A taxa efetiva é `taxaCdiAnual * (percentualCdi / 100)` — o percentual do
 *   CDI informado pelo usuário (ex.: 100% do CDI, 110% do CDI).
 * - Juros compostos mensais (aporte postecipado).
 * - IR regressivo: alíquota derivada de dias corridos, a menos que uma
 *   [aliquotaIrExplícita] seja fornecida (caso canônico de teste).
 */
object CdbCalculator {

    /**
     * Calcula a evolução de um CDB a partir do percentual do CDI.
     *
     * @param valorInicial valor inicialmente aplicado (centavos).
     * @param aporteMensal aporte mensal postecipado (centavos).
     * @param percentualCdi percentual do CDI (ex.: 100.0, 110.0).
     * @param taxaCdiAnual taxa anual efetiva do CDI (ex.: 10.75 = 10,75% a.a.).
     * @param meses prazo em meses.
     * @param diasCorridos prazo em dias corridos para a alíquota; quando nulo,
     *   derivado de `meses * 30.44`.
     * @param aliquotaIrExplícita alíquota fixa (ex.: 0.20); quando nula, usa a
     *   tabela regressiva.
     */
    fun calcularComCDI(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        percentualCdi: Double,
        taxaCdiAnual: Double,
        meses: Int,
        diasCorridos: Int? = null,
        aliquotaIrExplícita: Double? = null,
    ): InvestmentResult {
        val taxaEfetivaAnual = taxaCdiAnual * (percentualCdi / 100.0)
        val taxaMensal = RateConverter.anualParaMensal(taxaEfetivaAnual)
        return calcularComTaxaMensal(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            taxaMensalEfetiva = taxaMensal,
            meses = meses,
            diasCorridos = diasCorridos,
            aliquotaIrExplícita = aliquotaIrExplícita,
        )
    }

    /**
     * Calcula a evolução de um CDB a partir da **taxa DI anual em percentual**
     * (ex.: 10,90 = 10,90% a.a.) e do percentual do CDI (ex.: 100.0).
     *
     * É o caminho da tela Calculadora CDI/CDB: o campo "Taxa DI (% a.a.)" é
     * preenchido pelo BCB/fallback e o percentual vem dos chips; a taxa mensal
     * efetiva é `i = (1 + (DI * pct/100) / 100)^(1/12) - 1`. O IR usa a tabela
     * regressiva **por meses** (22,5/20/17,5/15%) sobre o ganho.
     */
    fun calcularComTaxaDiAnualPercentual(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        percentualCdi: Double,
        taxaDiAnualPercentual: Double,
        meses: Int,
    ): InvestmentResult {
        val taxaEfetivaAnual = taxaDiAnualPercentual * (percentualCdi / 100.0) / 100.0
        val taxaMensal = RateConverter.anualParaMensal(taxaEfetivaAnual)
        return calcularComTaxaMensal(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            taxaMensalEfetiva = taxaMensal,
            meses = meses,
        )
    }

    /**
     * Calcula a evolução de um CDB com taxa mensal efetiva já convertida.
     */
    fun calcularComTaxaMensal(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaMensalEfetiva: Double,
        meses: Int,
        diasCorridos: Int? = null,
        aliquotaIrExplícita: Double? = null,
    ): InvestmentResult {
        val totalInvestido = valorInicial.plus(aporteMensal.times(meses.toDouble()))
        val valorBruto = CompoundGrowth.montante(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            taxaMensal = taxaMensalEfetiva,
            meses = meses,
        )
        val rendimentoBruto = valorBruto.minus(totalInvestido)
        val dias = diasCorridos ?: (meses.toDouble() * 30.44).roundToInt()
        val aliquota = aliquotaIrExplícita ?: IncomeTax.aliquotaPorMeses(meses)
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
            diasCorridos = dias,
        )
    }
}
