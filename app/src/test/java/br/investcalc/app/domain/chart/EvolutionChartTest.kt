package br.investcalc.app.domain.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Testes herméticos (JVM pura) da camada de gráfico de evolução — o que
 * sustenta tanto o Canvas da tela de resultado quanto o card de destaque
 * "juros sobre juros" e a tabela mês a mês.
 *
 * Alinhados ao contrato selado em disco do [EvolutionChartBuilder]:
 * `construir(meses, totalInvestidoFimDoMes, saldoCompostoFimDoMes,
 * jurosSimplesBaselineFimDoMes, aportesPorMes)`.
 *
 * Tudo JVM, sem Android, sem rede, sem lançar.
 */
class EvolutionChartTest {

    @Test
    fun `serie vazia nunca passa adiante`() {
        val serie = EvolutionChartBuilder.construir(
            meses = emptyList(),
            totalInvestidoFimDoMes = emptyList(),
            saldoCompostoFimDoMes = emptyList(),
            jurosSimplesBaselineFimDoMes = emptyList(),
            aportesPorMes = emptyList(),
        )
        assertEquals(0, serie.pontos.size)
    }

    @Test
    fun `patrimonio final composto excede a baseline simples`() {
        // Sem aporte: 1.000,00 a 1,00% a.m. por 12 meses.
        val n = 12
        val meses = (1..n).map { m -> LocalDate.of(2026, 1, 1).plusMonths((m - 1).toLong()) }
        val saldos = (1..n).map { m -> 1000.0 * Math.pow(1.01, m.toDouble()) } // 1126.8250...
        val totais = List(n) { 1000.0 }
        val baselineSimples = (1..n).map { m -> 1000.0 * 0.01 * m } // 120.00 total
        val aportes = List(n) { 0.0 }

        val serie = EvolutionChartBuilder.construir(
            meses = meses,
            totalInvestidoFimDoMes = totais,
            saldoCompostoFimDoMes = saldos,
            jurosSimplesBaselineFimDoMes = baselineSimples,
            aportesPorMes = aportes,
        )

        val ultimo = serie.pontos.last()
        assertTrue(
            "composto deve exceder o total investido sem aporte",
            ultimo.saldoCompostoFimDoMes > ultimo.totalInvestidoFimDoMes,
        )
        assertTrue(
            "diferenca de juros sobre juros deve ser positiva",
            ultimo.diferencaJurosSobreJurosFimDoMes > 0.0,
        )
    }
}