package br.investcalc.app.domain.chart

import br.investcalc.app.domain.calculator.RateConverter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Auditoria do card "Bonus juros sobre juros".
 *
 * Definicao canonica: bonus(m) = jurosCompostosAcum(m) - jurosSimples(m),
 * com jurosSimples(m) = P*i*m + A*i*m*(m-1)/2.
 *
 * Formula ANTIGA (defeituosa, removida do AppNavHost): a UI derivava uma taxa
 * "efetiva" de (saldoFinal/totalInvestido)^(1/n)-1 e usava
 * `totalInvestido(m) * taxa * (m+1)`, resultando em 10,63 no cenario 100% em
 * vez de 13,51. Estes testes travam a formula correta.
 */
class JurosSimplesBaselineTest {

    @Test
    fun `referencia 1000 e 100 a 1 por cento 12 meses juros simples 186`() {
        val juros = JurosSimplesBaselineBuilder.jurosSimplesFechados(
            valorInicial = 1_000.0,
            aporteMensal = 100.0,
            mes = 12,
            taxaPercentualMensal = 1.0,
        )
        assertEquals(186.00, juros, 0.01)
    }

    @Test
    fun `bonus jsj pct 100 DI 13 90 1000 mais 200 12 meses aprox 13 51`() {
        val i = RateConverter.anualParaMensal(0.139) * 100.0
        val serie = PatrimonioSeriesBuilder.construir(1_000.0, 200.0, 12, i)
        val baseline = JurosSimplesBaselineBuilder.construir(1_000.0, 200.0, 12, i)

        val composto = serie.patrimonioLiquidoFinal -
            (serie.valorInicial + serie.aporteMensal * 12)
        val simples = baseline.jurosSimplesTotal

        assertEquals(288.31, composto, 0.05)
        assertEquals(274.80, simples, 0.05)
        assertEquals(13.51, composto - simples, 0.05)
    }

    @Test
    fun `bonus jsj pct 110 DI 13 90 1000 mais 200 12 meses aprox 16 20`() {
        val i = RateConverter.anualParaMensal(0.139 * 1.10) * 100.0
        val serie = PatrimonioSeriesBuilder.construir(1_000.0, 200.0, 12, i)
        val baseline = JurosSimplesBaselineBuilder.construir(1_000.0, 200.0, 12, i)

        val composto = serie.patrimonioLiquidoFinal -
            (serie.valorInicial + serie.aporteMensal * 12)
        val simples = baseline.jurosSimplesTotal

        assertEquals(316.77, composto, 0.05)
        assertEquals(300.57, simples, 0.05)
        assertEquals(16.20, composto - simples, 0.05)
    }
}