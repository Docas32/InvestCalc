package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.roundToLong

/**
 * Regressão **obrigatória** (aceite do projeto): o cálculo de juros compostos
 * deve reproduzir com exatidão o caso canônico abaixo. Este teste **não pode
 * ser removido, renomeado ou ter o valor esperado alterado**.
 *
 * - 1.000,00 a 1% a.m. por 12 meses (sem aportes) = **1.126,83**.
 */
class CompoundGrowthTest {

    private fun cents(reais: Double): Long = (reais * 100).roundToLong()

    private fun centsDe(reais: Double): Long = (reais * 100).roundToLong()

    @Test
    fun `regressao obrigatoria montante 1000 a 1 por cento a m por 12 meses`() {
        val montante = CompoundGrowth.montante(
            valorInicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0),
            taxaMensal = 0.01,
            meses = 12,
        )
        assertEquals(112_683L, montante.cents) // R$ 1.126,83
    }

    @Test
    fun `montanteComAportes com aporte zero iguala montante simples`() {
        val inicial = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
        val semAporte = CompoundGrowth.montanteComAportes(
            valorInicial = inicial,
            aporteMensal = br.investcalc.app.domain.model.MonetaryValue.ZERO,
            taxaMensal = 0.01,
            meses = 16,
        )
        val passivo = CompoundGrowth.montante(inicial, 0.01, 16)
        assertEquals(passivo.cents, semAporte.cents)
    }

    @Test
    fun `montanteComAportes com aporte postecipado`() {
        val inicial = br.investcalc.app.domain.model.MonetaryValue.ZERO
        val aporte = br.investcalc.app.domain.model.MonetaryValue.ofReais(1_000.0)
        val resultado = CompoundGrowth.montanteComAportes(inicial, aporte, 0.01, 3)
        // 1.000 * ((1,01^3 - 1)/0,01) = 1.000 * 3,0301 = 3.030,10
        assertEquals(303_010L + 0L, resultado.cents)
    }

    @Test
    fun `montante sem capitalizacao retorna o valor inicial arredondado`() {
        val montante = CompoundGrowth.montante(
            br.investcalc.app.domain.model.MonetaryValue.ofReais(750.0),
            0.0,
            0,
        )
        assertEquals(75_000L, montante.cents)
    }
}
