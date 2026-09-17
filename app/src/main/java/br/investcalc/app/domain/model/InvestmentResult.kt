package br.investcalc.app.domain.model

import br.investcalc.app.domain.model.MonetaryValue

/**
 * Resultado consolidado de uma simulação de investimento.
 *
 * Todos os valores monetários são expressos em `MonetaryValue` (centavos).
 *
 * @property valorInvestido total aportado (inicial + aportes).
 * @property valorBruto montante final antes do IR.
 * @property rendimentoBruto ganho bruto antes do IR (bruto - investido).
 * @property aliquotaIr alíquota efetiva aplicada (ex.: 0.20 = 20%).
 * @property irPago IR retido (em centavos).
 * @property valorLiquido montante final após IR (bruto - IR).
 * @property rendimentoLiquido ganho líquido (líquido - investido).
 * @property diasCorridos prazo em dias usado na alíquota.
 */
data class InvestmentResult(
    val valorInvestido: MonetaryValue,
    val valorBruto: MonetaryValue,
    val rendimentoBruto: MonetaryValue,
    val aliquotaIr: Double,
    val irPago: MonetaryValue,
    val valorLiquido: MonetaryValue,
    val rendimentoLiquido: MonetaryValue,
    val diasCorridos: Int = 7,
) {
    val isento: Boolean get() = aliquotaIr == 0.0
}
