package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TesouroIpcaPlusCalculatorTest {

    private val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
    private val zero = br.investcalc.app.domain.model.MonetaryValue.ZERO

    @Test
    fun `taxaAnualComposta combina IPCA e taxa real`() {
        val composta = TesouroIpcaPlusCalculator.taxaAnualComposta(0.10, 0.05)
        assertEquals(0.155, composta, 1e-12) // (1,10 * 1,05) - 1
    }

    @Test
    fun `calcular capitaliza taxa anual composta`() {
        val ipca = 0.03
        val taxaReal = 0.0
        // (1,03)^(12)-1 == Msg: tem que ser consistente com taxaAnualComposta.
        val resultado = TesouroIpcaPlusCalculator.calcular(inicial, zero, ipca, taxaReal, 60)
        // Bruto >= investido e IR regressivo aplicado.
        assertTrue(resultado.valorBruto.cents >= resultado.valorInvestido.cents)
        assertEquals(100_000L, resultado.valorInvestido.cents)
        assertTrue(resultado.irPago.cents >= 0L)
        assertEquals(0.15, resultado.aliquotaIr, 1e-9) // 60 meses -> >720 dias
    }
}
