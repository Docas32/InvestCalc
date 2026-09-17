package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.MonetaryValue
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Cálculo de juros compostos com aportes mensais postecipados (fim do mês).
 *
 * Fórmula do montante bruto:
 *   M = P * (1 + i)^n + A * (((1 + i)^n - 1) / i)
 * sendo P o valor inicial, A o aporte mensal, i a taxa mensal efetiva e
 * n o número de meses.
 *
 * Trabalha em centavos (Long), arredondando apenas no resultado final,
 * garantindo determinismo e tolerância de centavos nos testes.
 */
object CompoundGrowth {

    /**
     * Montante bruto com [valorInicial] capitalizando à [taxaMensal] por
     * [meses], somando [aporteMensal] postecipado ao fim de cada mês.
     *
     * Exemplo canônico: 100000 centavos (R$ 1.000,00) a 1% a.m. por 12 meses,
     * sem aportes, resulta em 112683 centavos (R$ 1.126,83).
     */
    fun montanteComAportes(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaMensal: Double,
        meses: Int,
    ): MonetaryValue {
        val fatorJuros = (1.0 + taxaMensal).pow(meses)
        var valorBrutoCentavos = valorInicial.cents * fatorJuros

        val fatorAportes = if (taxaMensal != 0.0) {
            aporteMensal.cents * ((fatorJuros - 1.0) / taxaMensal)
        } else {
            aporteMensal.cents * meses.toDouble()
        }
        valorBrutoCentavos += fatorAportes

        return MonetaryValue(valorBrutoCentavos.roundToLong())
    }

    /**
     * Montante bruto de um valor único capitalizado, sem aportes.
     * Equivale a [montanteComAportes] com aporte zero.
     */
    fun montante(
        valorInicial: MonetaryValue,
        taxaMensal: Double,
        meses: Int,
    ): MonetaryValue = montanteComAportes(valorInicial, MonetaryValue.ZERO, taxaMensal, meses)

    /**
     * Montante bruto com aportes mensais — mesmo cálculo de [montanteComAportes],
     * disponibilizado sob o nome canônico `montante` para os calculators.
     */
    fun montante(
        valorInicial: MonetaryValue,
        aporteMensal: MonetaryValue,
        taxaMensal: Double,
        meses: Int,
    ): MonetaryValue = montanteComAportes(valorInicial, aporteMensal, taxaMensal, meses)
}
