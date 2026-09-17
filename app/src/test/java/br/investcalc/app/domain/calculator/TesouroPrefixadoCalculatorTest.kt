package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TesouroPrefixadoCalculatorTest {

    private val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
    private val zero = br.investcalc.app.domain.model.MonetaryValue.ZERO

    @Test
    fun `tesouro prefixado usa taxa contratada na compra`() {
        // Prefixado 12,6825% a.a. efetiva = 1% a.m. por 12 meses -> 1.126,83.
        val taxaPrefixadaAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = TesouroPrefixadoCalculator.calcular(inicial, zero, taxaPrefixadaAnual, 12)
        assertEquals(112_683L, resultado.valorBruto.cents)
        assertEquals(100_000L, resultado.valorInvestido.cents)
    }

    @Test
    fun `tesouro prefixado aplica IR regressivo`() {
        val taxaPrefixadaAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = TesouroPrefixadoCalculator.calcular(inicial, zero, taxaPrefixadaAnual, 12)
        assertTrue(resultado.irPago.cents > 0L)
        assertTrue(resultado.aliquotaIr in 0.15..0.225)
    }
}
