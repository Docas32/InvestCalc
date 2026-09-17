package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LCI/LCA são **isentas de IR** para pessoa física. A calculadora deve
 * manter alíquota 0,0% e valor líquido igual ao bruto.
 */
class LciLcaCalculatorTest {

    private val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
    private val zero = br.investcalc.app.domain.model.MonetaryValue.ZERO

    @Test
    fun `LCI LCA aplicam aliquota zero e nao descontam IR`() {
        val taxaCdiAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = LciLcaCalculator.calcularComCDI(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 100.0,
            taxaCdiAnual = taxaCdiAnual,
            meses = 12,
        )
        assertEquals(0.0, resultado.aliquotaIr, 1e-9)
        assertEquals(0L, resultado.irPago.cents)
        assertEquals(resultado.valorBruto.cents, resultado.valorLiquido.cents)
    }

    @Test
    fun `LCI LCA com taxa mensal efetiva`() {
        val resultado = LciLcaCalculator.calcularComTaxaMensal(
            valorInicial = inicial,
            aporteMensal = zero,
            taxaMensalEfetiva = 0.01,
            meses = 12,
        )
        assertEquals(112_683L, resultado.valorBruto.cents) // R$ 1.126,83
        assertEquals(resultado.valorBruto.cents, resultado.valorLiquido.cents)
    }
}
