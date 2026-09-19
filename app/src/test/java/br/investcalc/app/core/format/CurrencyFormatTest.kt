package br.investcalc.app.core.format

import java.util.Locale
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

    // -- T0/P4: saída por locale com prefixo "R$" fixo -----------------------

    @Test
    fun `pt-BR formata centavos com ponto de milhar e virgula decimal`() {
        val ptBr = Locale("pt", "BR")
        assertEquals("R$ 3.688,31", CurrencyFormat.format(368_831L, ptBr))
        assertEquals("R$ 12.345,67", CurrencyFormat.format(1_234_567L, ptBr))
        assertEquals("R$ 0,00", CurrencyFormat.format(0L, ptBr))
        assertEquals("R$ 99,99", CurrencyFormat.format(9_999L, ptBr))
    }

    @Test
    fun `en formata centavos com virgula de milhar e ponto decimal`() {
        val en = Locale.US
        assertEquals("R$ 3,688.31", CurrencyFormat.format(368_831L, en))
        assertEquals("R$ 12,345.67", CurrencyFormat.format(1_234_567L, en))
        assertEquals("R$ 0.00", CurrencyFormat.format(0L, en))
        assertEquals("R$ 99.99", CurrencyFormat.format(9_999L, en))
    }

    @Test
    fun `formatReais pt-BR usa ponto de milhar`() {
        assertEquals("R$ 3.688,31", CurrencyFormat.formatReais(3_688.31, Locale("pt", "BR")))
        assertEquals("R$ 1.000,00", CurrencyFormat.formatReais(1_000.0, Locale("pt", "BR")))
    }

    @Test
    fun `formatReais en usa virgula de milhar`() {
        assertEquals("R$ 3,688.31", CurrencyFormat.formatReais(3_688.31, Locale.US))
        assertEquals("R$ 1,000.00", CurrencyFormat.formatReais(1_000.0, Locale.US))
    }

    @Test
    fun `formatReais arredonda meio para cima como o String format`() {
        assertEquals("R$ 1,234.57", CurrencyFormat.formatReais(1_234.565, Locale.US))
        assertEquals("R$ 1.234,57", CurrencyFormat.formatReais(1_234.565, Locale("pt", "BR")))
    }

    @Test
    fun `formato default usa o locale atual do processo`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("pt", "BR"))
            assertEquals("R$ 3.688,31", CurrencyFormat.format(368_831L))
            assertEquals("R$ 3.688,31", CurrencyFormat.formatReais(3_688.31))
            Locale.setDefault(Locale.US)
            assertEquals("R$ 3,688.31", CurrencyFormat.format(368_831L))
            assertEquals("R$ 3,688.31", CurrencyFormat.formatReais(3_688.31))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `valores negativos nao vazam para a saida`() {
        assertEquals("R$ 0,00", CurrencyFormat.format(-5L, Locale("pt", "BR")))
        assertEquals("R$ 0.00", CurrencyFormat.formatReais(-1.0, Locale.US))
    }
}
