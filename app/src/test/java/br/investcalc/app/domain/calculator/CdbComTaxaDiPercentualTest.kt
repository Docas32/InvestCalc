package br.investcalc.app.domain.calculator

import br.investcalc.app.domain.model.MonetaryValue
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressão obrigatória P4 do CDB com IR regressivo e taxa DI em percentual.
 *
 * - Caso P4.1: DI 13,90% a.a., 100% do CDI, 1.000,00 sem aporte, 12 meses →
 *   bruto 1.139,00; ganho 139,00; IR 20% = 27,80; líquido 1.111,20
 *   (tolerância 0,02). Fallback do app é 13,90 — nenhum 10,90 remanescente.
 * - Brackets P4.2: 6m → 22,5%; 12m → 20%; 24m → 17,5%; 25m → 15% sobre ganho.
 */
class CdbComTaxaDiPercentualTest {

    private val TOL = 0.02 // em reais, tolerância P4.1
    private val inicial = MonetaryValue.ofReais(1_000.0)
    private val zero = MonetaryValue.ZERO

    @Test
    fun `P4 1 DI 13 90 pct 100 1000 sem aporte 12 meses`() {
        val r = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 100.0,
            taxaDiAnualPercentual = 13.90,
            meses = 12,
        )
        assertEquals(1_139.00, r.valorBruto.reais(), TOL)
        assertEquals(1_000.00, r.valorInvestido.reais(), TOL)
        assertEquals(139.00, r.rendimentoBruto.reais(), TOL)
        assertEquals(0.20, r.aliquotaIr, 1e-9)
        assertEquals(27.80, r.irPago.reais(), TOL)
        assertEquals(1_111.20, r.valorLiquido.reais(), TOL)
    }

    @Test
    fun `P4 2 brackets regressivos por meses sobre ganho 100`() {
        val ganho = MonetaryValue.ofReais(100.0)
        val casos = mapOf(
            6 to (0.225 to 22.50),
            12 to (0.20 to 20.00),
            24 to (0.175 to 17.50),
            25 to (0.15 to 15.00),
        )
        casos.forEach { (meses, esperado) ->
            val aliquota = IncomeTax.aliquotaPorMeses(meses)
            val ir = IncomeTax.calcularIrSobreAliquota(ganho, aliquota)
            assertEquals("aliquota ${meses}m", esperado.first, aliquota, 1e-9)
            assertEquals("IR ${meses}m", esperado.second, ir.reais(), 0.001)
        }
    }

    @Test
    fun `IR incide sobre o ganho nunca sobre o principal`() {
        val r = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 100.0,
            taxaDiAnualPercentual = 13.90,
            meses = 12,
        )
        // IR <= 30% do ganho e nunca toca o principal investido
        assertEquals(r.irPago.cents, (r.rendimentoBruto.cents * 0.20).toLong())
        assert(r.valorLiquido.cents >= r.valorInvestido.cents)
    }

    @Test
    fun `P4 3 modo avancado taxa mensal direta 1 por cento 1000 mais 200 12 meses bruto 3663 33`() {
        val r = CdbCalculator.calcularComTaxaMensal(
            valorInicial = MonetaryValue.ofReais(1_000.0),
            aporteMensal = MonetaryValue.ofReais(200.0),
            taxaMensalEfetiva = 0.01,
            meses = 12,
        )
        assertEquals(3_663.33, r.valorBruto.reais(), 0.01)
        assertEquals(3_400.00, r.valorInvestido.reais(), 0.01)
        assertEquals(263.33, r.rendimentoBruto.reais(), 0.01)
        assertEquals(0.20, r.aliquotaIr, 1e-9)
    }

    @Test
    fun `P1 1 pct 110 DI 13 90 1000 sem aporte 12 meses mes1 11 93 e bruto 1152 90`() {
        val r = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 110.0,
            taxaDiAnualPercentual = 13.90,
            meses = 12,
        )
        // O pct TEM de chegar ao motor: 13,90 x 1,10 = 15,29% a.a. -> 1.152,90.
        assertEquals(1_152.90, r.valorBruto.reais(), 0.05)
        val iMensal = RateConverter.anualParaMensal(0.139 * 1.10)
        assertEquals(11.93, 1_000.0 * iMensal, 0.02) // mes 1 = 1000 x i
    }

    @Test
    fun `P1 1 pct 92 5 decimal efetiva mensal 1 0131`() {
        val efetivaAnual = 13.90 * 0.925 / 100.0
        val iMensalPercent = RateConverter.anualParaMensal(efetivaAnual) * 100.0
        assertEquals(1.0131, iMensalPercent, 0.0005)

        val r = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = inicial,
            aporteMensal = zero,
            percentualCdi = 92.5,
            taxaDiAnualPercentual = 13.90,
            meses = 12,
        )
        assertEquals(1_000.0 * (1.0 + efetivaAnual), r.valorBruto.reais(), 0.05)
    }
}

private fun MonetaryValue.reais(): Double = cents / 100.0