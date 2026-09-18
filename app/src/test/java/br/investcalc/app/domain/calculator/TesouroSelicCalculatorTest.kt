package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TesouroSelicCalculatorTest {

    private val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
    private val zero = br.investcalc.app.domain.model.MonetaryValue.ZERO

    @Test
    fun `tesouro selic capitaliza pela taxa selic anual`() {
        // Selic anual efetiva = 1% mensal equivalente -> canônico 1.126,83.
        val taxaSelicAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = TesouroSelicCalculator.calcular(inicial, zero, taxaSelicAnual, 24)
        assertTrue(resultado.valorBruto.cents > 120_000L)
        // P3: IR por meses civis (mesma tabela do CDB) — 24 meses → 17,5%.
        assertEquals(0.175, resultado.aliquotaIr, 1e-9)
    }

    @Test
    fun `tesouro selic mantem consistencia investido e liquido`() {
        val taxaSelicAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = TesouroSelicCalculator.calcular(inicial, zero, taxaSelicAnual, 24)
        assertEquals(100_000L, resultado.valorInvestido.cents)
        assertEquals(
            resultado.valorLiquido.cents,
            resultado.valorBruto.cents - resultado.irPago.cents,
        )
    }
}
