package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressão **obrigatória** (aceite do projeto): a calculadora de CDB com taxa
 * mensal efetiva deve reproduzir o caso canônico abaixo. Este teste **não pode
 * ser removido, renomeado ou ter os valores esperados alterados**.
 *
 * - 1.000,00 a 1% a.m. por 12 meses -> bruto **1.126,83**.
 * - IR de 20% sobre o ganho -> IR **25,37** e líquido **1.101,46**.
 */
class CdbCalculatorTest {

    private val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
    private val zero = br.investcalc.app.domain.model.MonetaryValue.ZERO

    @Test
    fun `regressao obrigatoria CDB com aliquota IR explicita de 20 por cento`() {
        val resultado = CdbCalculator.calcularComTaxaMensal(
            valorInicial = inicial,
            aporteMensal = zero,
            taxaMensalEfetiva = 0.01,
            meses = 12,
            aliquotaIrExplícita = 0.20,
        )
        assertEquals(112_683L, resultado.valorBruto.cents)      // 1.126,83
        assertEquals(12_683L, resultado.rendimentoBruto.cents)  // ganho bruto
        assertEquals(0.20, resultado.aliquotaIr, 1e-9)          // 20%
        assertEquals(2_537L, resultado.irPago.cents)            // 25,37
        assertEquals(110_146L, resultado.valorLiquido.cents)    // 1.101,46
        assertEquals(112_683L - 2_537L, resultado.valorLiquido.cents)
    }

    @Test
    fun `calcularComCDI converte percentual e capitaliza`() {
        // 100% do CDI, CDI anual efetivo = 12,6825% -> taxa mensal 1% -> mesmo canônico.
        val taxaCdiAnual = Math.pow(1.01, 12.0) - 1.0
        val resultado = CdbCalculator.calcularComCDI(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 100.0,
            taxaCdiAnual = taxaCdiAnual,
            meses = 12,
        )
        org.junit.Assert.assertTrue(
            "Bruto deve estar a menos de 1 centavo de 1.126,83, mas foi ${resultado.valorBruto.cents}",
            Math.abs(resultado.valorBruto.cents - 112_683L) <= 1L, // tolerância de centavo
        )
    }

    @Test
    fun `rendimento liquido e a diferenca bruta menos IR`() {
        val resultado = CdbCalculator.calcularComTaxaMensal(
            valorInicial = inicial,
            aporteMensal = zero,
            taxaMensalEfetiva = 0.01,
            meses = 12,
            aliquotaIrExplícita = 0.20,
        )
        assertEquals(resultado.valorLiquido.cents, resultado.valorBruto.cents - resultado.irPago.cents)
        assertEquals(resultado.rendimentoLiquido.cents, resultado.rendimentoBruto.cents - resultado.irPago.cents)
    }
}
