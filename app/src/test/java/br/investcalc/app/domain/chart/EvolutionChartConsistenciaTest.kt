package br.investcalc.app.domain.chart

import br.investcalc.app.domain.calculator.CdbCalculator
import br.investcalc.app.domain.model.MonetaryValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.pow

/**
 * Testes herméticos (JVM pura) da consistência do gráfico com o resultado
 * agregado — o pipeline exato da tela:
 *
 * ```
 * PatrimonioSeriesBuilder.construir(...)   -> série mensal (chart)
 * EvolutionChartBuilder.dePatrimonio(...)  -> série de evolução (2 linhas)
 * CdbCalculator.calcularComTaxaDiAnualPercentual(...) -> valorBruto agregado
 * ```
 *
 * Contrato verificado: o ÚLTIMO ponto do gráfico (saldo composto) coincide
 * com o `valorBruto` do resultado agregado — tolerância de 1 centavo (IEEE),
 * já que o gráfico compõe mês a mês em Double e o agregado usa a fórmula
 * fechada em centavos.
 */
class EvolutionChartConsistenciaTest {

    private val inicio = LocalDate.of(2026, 1, 1)

    private fun taxaMensalEfetiva(taxaDiAnualPercentual: Double, percentualCdi: Double): Double {
        val fatorAnual = 1.0 + (taxaDiAnualPercentual / 100.0) * (percentualCdi / 100.0)
        return (fatorAnual.pow(1.0 / 12.0) - 1.0) * 100.0
    }

    @Test
    fun `ultimo ponto do grafico coincide com o valorBruto agregado`() {
        val valorInicial = 1000.0
        val aporteMensal = 200.0
        val meses = 12
        val percentualCdi = 110.0
        val taxaDi = 10.90

        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            meses = meses,
            taxaMensalPercentual = taxaMensalEfetiva(taxaDi, percentualCdi),
            inicio = inicio,
        )
        val grafico = EvolutionChartBuilder.dePatrimonio(serie)
        val resultado = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = MonetaryValue.ofReais(valorInicial),
            aporteMensal = MonetaryValue.ofReais(aporteMensal),
            percentualCdi = percentualCdi,
            taxaDiAnualPercentual = taxaDi,
            meses = meses,
        )

        val ultimoPonto = grafico.pontos.last()
        val diferenca = abs(ultimoPonto.saldoCompostoFimDoMes - resultado.valorBruto.cents / 100.0)
        assertTrue(
            "ultimo saldo ${ultimoPonto.saldoCompostoFimDoMes} diverge do valorBruto " +
                "${resultado.valorBruto.cents / 100.0} por $diferenca",
            diferenca <= 0.01,
        )
    }

    @Test
    fun `serie de 999 pontos mantem consistencia no ultimo ponto`() {
        val valorInicial = 500.0
        val aporteMensal = 100.0
        val meses = 999
        val percentualCdi = 100.0
        val taxaDi = 10.90

        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = valorInicial,
            aporteMensal = aporteMensal,
            meses = meses,
            taxaMensalPercentual = taxaMensalEfetiva(taxaDi, percentualCdi),
            inicio = inicio,
        )
        val grafico = EvolutionChartBuilder.dePatrimonio(serie)
        val resultado = CdbCalculator.calcularComTaxaDiAnualPercentual(
            valorInicial = MonetaryValue.ofReais(valorInicial),
            aporteMensal = MonetaryValue.ofReais(aporteMensal),
            percentualCdi = percentualCdi,
            taxaDiAnualPercentual = taxaDi,
            meses = meses,
        )

        assertEquals(999, grafico.pontos.size)
        val totalEsperado = valorInicial + aporteMensal * meses
        val ultimo = grafico.pontos.last()
        assertEquals(totalEsperado, ultimo.totalInvestidoFimDoMes, 0.01)
        assertEquals(resultado.valorBruto.cents / 100.0, ultimo.saldoCompostoFimDoMes, 0.01)
    }

    @Test
    fun `linha de total investido e sempre reta e nunca supera o saldo composto`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 200.0,
            meses = 60,
            taxaMensalPercentual = 1.0,
            inicio = inicio,
        )
        val grafico = EvolutionChartBuilder.dePatrimonio(serie)

        grafico.pontos.forEachIndexed { m, p ->
            assertEquals(
                "aporte constante deve render reta de total investido",
                p.totalInvestidoFimDoMes,
                1000.0 + 200.0 * (m + 1),
                0.001,
            )
            assertTrue(
                "juros nunca negativos: saldo >= total investido no mes $m",
                p.saldoCompostoFimDoMes >= p.totalInvestidoFimDoMes,
            )
        }
    }
}