package br.investcalc.app.domain.chart

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Testes herméticos (JVM pura) da série de juros compostos [PatrimonioSeriesBuilder].
 *
 * Contratos regressivos selados em disco:
 * 1) 1000,00 sem aporte, 12 meses, 0,7% a.m. → saldo final 1.087,31
 * 2) 1000,00 + 200,00/mês, 12 meses, 1% a.m. → bruto 3.663,33,
 *    investido 3.400,00, juros 263,33 (mês 1: 10,00/1.210,00; mês 2: 12,10/1.422,10)
 * 3) taxa 0% → saldo final = 3.400,00 (nunca 0, nunca 1.000)
 *
 * Convenção: saldo(m) = saldo(m-1)*(1+i)+aporte; rendimento(m) = saldo(m-1)*i;
 * saldo(0) = investimento inicial; aporte creditado no fim do mês.
 */
class PatrimonioSeriesBuilderTest {

    private val inicio = LocalDate.of(2026, 1, 1)

    @Test
    fun `1000 sem aporte 12 meses 0 7 por cento a-m saldo final 1087 31`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 0.0,
            meses = 12,
            taxaMensalPercentual = 0.7,
            inicio = inicio,
        )

        assertEquals(12, serie.pontos.size)
        assertEquals(1087.31, serie.pontos.last().patrimonioLiquidoFimDoMes, 0.01)
        assertEquals(0.0, serie.aporteTotal, 0.001)
    }

    @Test
    fun `1000 e 200 por mes 12 meses 1 por cento bruto 3663 33 investido 3400 juros 263 33`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 200.0,
            meses = 12,
            taxaMensalPercentual = 1.0,
            inicio = inicio,
        )

        val ultimo = serie.pontos.last()
        assertEquals(3663.33, ultimo.patrimonioLiquidoFimDoMes, 0.01)

        val investido = 1000.0 + serie.aporteTotal
        assertEquals(3400.0, investido, 0.01)
        assertEquals(263.33, ultimo.patrimonioLiquidoFimDoMes - investido, 0.01)

        assertEquals(10.00, serie.pontos[0].rendimentoNoMes, 0.01)
        assertEquals(1210.00, serie.pontos[0].patrimonioLiquidoFimDoMes, 0.01)

        assertEquals(12.10, serie.pontos[1].rendimentoNoMes, 0.01)
        assertEquals(1422.10, serie.pontos[1].patrimonioLiquidoFimDoMes, 0.01)
    }

    @Test
    fun `taxa zero saldo final igual a investido acumulado nunca 0 nunca 1000`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 200.0,
            meses = 12,
            taxaMensalPercentual = 0.0,
            inicio = inicio,
        )

        val ultimo = serie.pontos.last().patrimonioLiquidoFimDoMes
        assertEquals(3400.0, ultimo, 0.001)
        assertNotEquals(0.0, ultimo, 0.001)
        assertNotEquals(1000.0, ultimo, 0.001)
    }

    @Test
    fun `meses comecam no mes seguinte ao inicio injetado`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 0.0,
            meses = 3,
            taxaMensalPercentual = 1.0,
            inicio = inicio,
        )

        assertEquals(LocalDate.of(2026, 2, 1), serie.pontos[0].inicioDoMes)
        assertEquals(LocalDate.of(2026, 3, 1), serie.pontos[1].inicioDoMes)
        assertEquals(LocalDate.of(2026, 4, 1), serie.pontos[2].inicioDoMes)
        assertTrue(serie.pontos[0].inicioDoMes.isAfter(inicio))
    }

    @Test
    fun `meses nunca saem zerados na ultima linha`() {
        val serie = PatrimonioSeriesBuilder.construir(
            valorInicial = 1000.0,
            aporteMensal = 0.0,
            meses = 12,
            taxaMensalPercentual = 0.7,
            inicio = inicio,
        )

        for (ponto in serie.pontos) {
            assertNotEquals(0.0, ponto.patrimonioLiquidoFimDoMes, 0.001)
        }
        assertNotEquals(0.0, serie.pontos.last().patrimonioLiquidoFimDoMes, 0.001)
    }
}