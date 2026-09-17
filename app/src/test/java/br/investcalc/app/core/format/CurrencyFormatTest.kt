package br.investcalc.app.core.format

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatTest {

    @Test
    fun `parseToCents extrai digitos em reais`() {
        assertEquals(100_000L, CurrencyFormat.parseToCents("1.000,00"))
        assertEquals(112_683L, CurrencyFormat.parseToCents("1.126,83"))
    }

    @Test
    fun `parseToCents aceita texto com simbolo monetario`() {
        assertEquals(110_146L, CurrencyFormat.parseToCents("R$ 1.101,46"))
        assertEquals(25_37L, CurrencyFormat.parseToCents("R$25,37"))
    }

    @Test
    fun `parseToCents trata entrada nao numerica`() {
        assertEquals(0L, CurrencyFormat.parseToCents(""))
        assertEquals(0L, CurrencyFormat.parseToCents("abc"))
    }

    @Test
    fun `parseToCents usa somente os ultimos 13 digitos`() {
        // Limita quantos dígitos são interpretados (evita estouro de Long).
        val milhares = "1".repeat(14) // 14 dígitos — ignorar o primeiro
        val esperado = ("1".repeat(13)).toLong()
        assertEquals(esperado, CurrencyFormat.parseToCents(milhares))
    }

    @Test
    fun `parseToCents valores negativos nao estouram`() {
        assertEquals(0L, CurrencyFormat.parseToCents("-R$ 0,00"))
    }
}
