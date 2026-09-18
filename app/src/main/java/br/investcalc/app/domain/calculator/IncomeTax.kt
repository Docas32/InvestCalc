package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.MonetaryValue

/**
 * Imposto de Renda regressivo sobre rendimentos de renda fixa, baseado
 * em dias corridos de aplicação. A alíquota diminui quanto maior o prazo.
 *
 * Tabela regressiva:
 *  - até 180 dias ............ 22,5%
 *  - de 181 a 360 dias ....... 20,0%
 *  - de 361 a 720 dias ....... 17,5%
 *  - acima de 720 dias ....... 15,0%
 */
object IncomeTax {

    const val ALIQUOTA_ATE_180: Double = 0.225
    const val ALIQUOTA_181_360: Double = 0.20
    const val ALIQUOTA_361_720: Double = 0.175
    const val ALIQUOTA_ACIMA_720: Double = 0.15

    private const val DIAS_ATE_180 = 180
    private const val DIAS_ATE_360 = 360
    private const val DIAS_ATE_720 = 720

    /**
     * Retorna a alíquota de IR correspondente a [diasCorridos] de aplicação.
     */
    fun aliquotaPorDias(diasCorridos: Int): Double = when {
        diasCorridos <= 0 -> 0.0
        diasCorridos <= DIAS_ATE_180 -> ALIQUOTA_ATE_180
        diasCorridos <= DIAS_ATE_360 -> ALIQUOTA_181_360
        diasCorridos <= DIAS_ATE_720 -> ALIQUOTA_361_720
        else -> ALIQUOTA_ACIMA_720
    }

    /**
     * Alíquota regressiva por **meses civis** de aplicação (contrato selado do
     * CDB): até 6 meses 22,5%; 7..12 meses 20%; 13..24 meses 17,5%; acima de
     * 24 meses 15%. O IR incide sobre o ganho (juros), nunca sobre o principal.
     */
    fun aliquotaPorMeses(meses: Int): Double = when {
        meses <= 0 -> 0.0
        meses <= 6 -> ALIQUOTA_ATE_180
        meses <= 12 -> ALIQUOTA_181_360
        meses <= 24 -> ALIQUOTA_361_720
        else -> ALIQUOTA_ACIMA_720
    }

    /**
     * IR a pagar sobre o [rendimentoBruto], conforme a alíquota regressiva
     * calculada a partir de [diasCorridos].
     */
    fun calcularIrSobreDias(rendimentoBruto: MonetaryValue, diasCorridos: Int): MonetaryValue {
        val aliquota = aliquotaPorDias(diasCorridos)
        return rendimentoBruto.times(aliquota)
    }

    /**
     * IR a pagar usando uma alíquota explícita [aliquota] (ex.: 0.20 = 20%).
     *
     * Usada quando o chamador quer fixar a alíquota independentemente da
     * faixa por dias — caso canônico de regressão do CDB.
     */
    fun calcularIrSobreAliquota(rendimentoBruto: MonetaryValue, aliquota: Double): MonetaryValue =
        rendimentoBruto.times(aliquota)
}
