package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.chart.EvolutionChartBuilder
import br.investcalc.app.domain.chart.PatrimonioSeriesBuilder
import br.investcalc.app.domain.model.MonetaryValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contratos numéricos obrigatórios do P3 — Tesouro Direto e Comparador.
 * Todos pinam a taxa (o mérito é o motor + conversões), sem rede:
 */
class InvestimentoRegressaoP3Test {

    private val inicial = MonetaryValue.ofReais(1_000.0)
    private val zero = MonetaryValue.ZERO

    private fun reais(mv: MonetaryValue): Double = mv.cents / 100.0

    @Test
    fun `P3 1 selic 13 90 a a 1000 sem aporte 12 meses bruto 1139 ir 27 80 liquido 1111 20`() {
        val r = TesouroSelicCalculator.calcular(inicial, zero, 0.139, 12)
        assertEquals("bruto", 1_139.00, reais(r.valorBruto), 0.02)
        assertEquals("IR", 27.80, reais(r.irPago), 0.02)
        assertEquals("líquido", 1_111.20, reais(r.valorLiquido), 0.02)
        // Consistência gráfico: último ponto da série == bruto do motor.
        assertEquals(1_139.00, ultimoPontoDoGrafico(0.139), 0.01)
    }

    @Test
    fun `P3 2 ipca mais 4 50 real 6 00 efetiva 10 77 bruto 1107 70 liquido 1086 16`() {
        val efetiva = TesouroIpcaPlusCalculator.taxaAnualComposta(0.045, 0.06)
        assertEquals("efetiva", 0.1077, efetiva, 1e-9)

        val r = TesouroIpcaPlusCalculator.calcular(inicial, zero, 0.045, 0.06, 12)
        assertEquals("bruto", 1_107.70, reais(r.valorBruto), 0.10)
        assertEquals("líquido", 1_086.16, reais(r.valorLiquido), 0.10)
        assertEquals(1_107.70, ultimoPontoDoGrafico(efetiva), 0.10)
    }

    @Test
    fun `P3 3 prefixado 12 a a 1000 12 meses bruto 1120 liquido 1096`() {
        val r = TesouroPrefixadoCalculator.calcular(inicial, zero, 0.12, 12)
        assertEquals("bruto", 1_120.00, reais(r.valorBruto), 0.02)
        assertEquals("líquido", 1_096.00, reais(r.valorLiquido), 0.02)
        assertEquals(1_120.00, ultimoPontoDoGrafico(0.12), 0.01)
    }

    @Test
    fun `P3 4 comparador DI 13 90 cdb 100 vs lci 90 12 meses`() {
        val r = ComparadorCalculator.comparar(inicial, zero, 100.0, 90.0, 13.90, 12)

        // CDB com IR regressivo; LCI/LCA isenta.
        assertEquals(1_111.20, reais(r.cdbLiquido), 0.02)
        assertEquals(1_125.10, reais(r.lciLiquido), 0.02)
        assertTrue("LCI deve vencer", r.lciLiquido > r.cdbLiquido)
        assertEquals("diferença", 13.90, (r.lciLiquido.cents - r.cdbLiquido.cents) / 100.0, 0.02)

        // Equilíbrio: 90 / (1 - 20%) = 112,5% neste prazo.
        assertEquals("equilíbrio", 112.5, r.percentualCdbEquivalente, 1e-9)
    }

    /** Último ponto da linha do gráfico (mesmo pipeline da tela), em reais. */
    private fun ultimoPontoDoGrafico(taxaAnualFracao: Double): Double {
        val mensalPercentual = RateConverter.anualParaMensal(taxaAnualFracao) * 100.0
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1_000.0,
            aporteMensal = 0.0,
            meses = 12,
            taxaMensalPercentual = mensalPercentual,
        )
        return EvolutionChartBuilder.dePatrimonio(serie).saldoCompostoFinal
    }
}