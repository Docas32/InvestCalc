package br.investcalc.app.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes da máscara monetária pura de entrada (fatia T1 + casos de edição T3).
 *
 * Cobre o "modo de edição": como o estado é sempre apenas os dígitos, apagar
 * no meio remove 1 dígito naturalmente (sem duplicar/crashar) e colar com
 * letras/símbolos é filtrado antes de reformatar.
 */
class CurrencyInputMaskTest {

    @Test
    fun `digito 5 vira R$ 0,05`() {
        assertEquals("R$ 0,05", CurrencyInputMask.apply("5"))
    }

    @Test
    fun `100000 vira mil reais`() {
        assertEquals("R$ 1.000,00", CurrencyInputMask.apply("100000"))
    }

    @Test
    fun `1000 vira R$ 10,00`() {
        assertEquals("R$ 10,00", CurrencyInputMask.apply("1000"))
    }

    @Test
    fun `10000000 vira cem mil reais`() {
        assertEquals("R$ 100.000,00", CurrencyInputMask.apply("10000000"))
    }

    @Test
    fun `vazio permanece vazio`() {
        assertEquals("", CurrencyInputMask.apply(""))
    }

    @Test
    fun `letras e simbolos sao filtrados, digitos mantidos`() {
        assertEquals("R$ 12,34", CurrencyInputMask.apply("12a3b4c"))
    }

    @Test
    fun `digitsOf extrai apenas digitos`() {
        assertEquals("1234", CurrencyInputMask.digitsOf("1a2.3,4"))
        assertEquals("", CurrencyInputMask.digitsOf(""))
    }

    @Test
    fun `edicao no meio remove 1 digito sem duplicar nem crashear`() {
        val digitos = CurrencyInputMask.digitsOf("R$ 1.000,00") // "100000"
        val sem4o = digitos.removeRange(3, 4)                    // "10000"
        assertEquals("R$ 100,00", CurrencyInputMask.apply(sem4o))
    }

    @Test
    fun `limites a 13 digitos`() {
        val quatorze = "1".repeat(14)
        assertEquals(CurrencyInputMask.MAX_DIGITS, CurrencyInputMask.digitsOf(quatorze).length)
        assertEquals("R$ 11.111.111.111,11", CurrencyInputMask.apply(quatorze))
    }

    @Test
    fun `parseToCents devolve centavos`() {
        assertEquals(110_146L, CurrencyInputMask.parseToCents("R$ 1.101,46"))
        assertEquals(5L, CurrencyInputMask.parseToCents("R$ 0,05"))
        assertEquals(0L, CurrencyInputMask.parseToCents(""))
        assertEquals(0L, CurrencyInputMask.parseToCents("abc"))
    }

    @Test
    fun `toReais devolve valor decimal`() {
        assertEquals(10.0, CurrencyInputMask.toReais("R$ 10,00"), 0.0)
        assertEquals(1.01, CurrencyInputMask.toReais("R$ 1,01"), 0.0)
    }
}
