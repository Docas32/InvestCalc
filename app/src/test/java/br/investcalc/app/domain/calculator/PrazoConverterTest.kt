package br.investcalc.app.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato de conversão de prazo (T6/P7): dias → meses por 30,4375 com
 * mínimo 1; anos → meses (×12); faixas de validação por unidade. O motor de
 * cálculo permanece em meses.
 */
class PrazoConverterTest {

    @Test
    fun `180 dias equivalem a 6 meses`() {
        assertEquals(6, PrazoConverter.paraMeses(180, UnidadePrazo.DIAS))
    }

    @Test
    fun `45 dias equivalem a 2 meses`() {
        assertEquals(2, PrazoConverter.paraMeses(45, UnidadePrazo.DIAS))
    }

    @Test
    fun `5 anos equivalem a 60 meses`() {
        assertEquals(60, PrazoConverter.paraMeses(5, UnidadePrazo.ANOS))
    }

    @Test
    fun `1 dia converte para 1 mes (minimo) e a tabela exibe 1 linha`() {
        assertEquals(1, PrazoConverter.paraMeses(1, UnidadePrazo.DIAS))
        assertEquals(1, PrazoConverter.paraMeses(29, UnidadePrazo.DIAS))
    }

    @Test
    fun `meses permanece inalterado`() {
        assertEquals(12, PrazoConverter.paraMeses(12, UnidadePrazo.MESES))
        assertEquals(9999, PrazoConverter.paraMeses(9999, UnidadePrazo.MESES))
    }

    @Test
    fun `limites de conversao dias`() {
        // ceil(18250/30,4375) = ceil(599,59...) = 600 — mesma régua de 50 anos.
        assertEquals(600, PrazoConverter.paraMeses(18_250, UnidadePrazo.DIAS))
        assertEquals(600, PrazoConverter.paraMeses(50, UnidadePrazo.ANOS))
    }

    @Test
    fun `validacao por unidade`() {
        assertTrue(PrazoConverter.valido(1, UnidadePrazo.DIAS))
        assertTrue(PrazoConverter.valido(18_250, UnidadePrazo.DIAS))
        assertFalse(PrazoConverter.valido(0, UnidadePrazo.DIAS))
        assertFalse(PrazoConverter.valido(18_251, UnidadePrazo.DIAS))

        assertTrue(PrazoConverter.valido(1, UnidadePrazo.MESES))
        assertTrue(PrazoConverter.valido(9_999, UnidadePrazo.MESES))
        assertFalse(PrazoConverter.valido(0, UnidadePrazo.MESES))
        assertFalse(PrazoConverter.valido(10_000, UnidadePrazo.MESES))

        assertTrue(PrazoConverter.valido(1, UnidadePrazo.ANOS))
        assertTrue(PrazoConverter.valido(50, UnidadePrazo.ANOS))
        assertFalse(PrazoConverter.valido(0, UnidadePrazo.ANOS))
        assertFalse(PrazoConverter.valido(51, UnidadePrazo.ANOS))
    }

    @Test
    fun `valores nao inteiros de dias sobem para o mes seguinte`() {
        // 300 dias → ceil(9,856) = 10 meses.
        assertEquals(10, PrazoConverter.paraMeses(300, UnidadePrazo.DIAS))
    }
}