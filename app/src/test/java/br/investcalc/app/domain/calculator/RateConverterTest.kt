package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class RateConverterTest {

    private val TOLERANCIA = 1e-12

    @Test
    fun `anualParaMensal inverte equivalencia exponencial`() {
        // anual = (1,01^12)-1 = 12,6825% a.a. efetiva -> mensal = 1% exato.
        val taxaAnual = Math.pow(1.01, 12.0) - 1.0
        assertEquals(0.01, RateConverter.anualParaMensal(taxaAnual), TOLERANCIA)
    }

    @Test
    fun `mensalParaAnual aplica 12 capitalizacoes`() {
        val taxaAnual = RateConverter.mensalParaAnual(0.01)
        assertEquals(Math.pow(1.01, 12.0) - 1.0, taxaAnual, TOLERANCIA)
    }

    @Test
    fun `anualizar acumula juros compostos de serie mensal`() {
        val anual = RateConverter.anualizar(listOf(0.01, 0.01, 0.01))
        assertEquals(Math.pow(1.01, 3.0) - 1.0, anual, TOLERANCIA)
    }

    @Test
    fun `anualizar lista vazia resulta em taxa zero`() {
        assertEquals(0.0, RateConverter.anualizar(emptyList()), TOLERANCIA)
    }

    @Test
    fun `conversoes sao inversas entre si`() {
        val mensal = 0.005
        val portaDeIda = RateConverter.anualParaMensal(RateConverter.mensalParaAnual(mensal))
        assertEquals(mensal, portaDeIda, TOLERANCIA)
    }
}
