package br.investcalc.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MonetaryValueTest {

    @Test
    fun `ofReais converte reais em centavos arredondando`() {
        assertEquals(1_000L, MonetaryValue.ofReais(10.0).cents)
        assertEquals(1_126_83L, MonetaryValue.ofReais(1_126.825_030_131_969_8).cents)
    }

    @Test
    fun `plus soma centavos`() {
        assertEquals(212L, MonetaryValue(100L).plus(MonetaryValue(112L)).cents)
    }

    @Test
    fun `minus subtrai centavos`() {
        assertEquals(12L, MonetaryValue(112L).minus(MonetaryValue(100L)).cents)
    }

    @Test
    fun `times aplica fator arredondando`() {
        assertEquals(253L, MonetaryValue(1_267L).times(0.20).cents)
    }

    @Test
    fun `ZERO e zero`() {
        assertEquals(0L, MonetaryValue.ZERO.cents)
    }

    @Test
    fun `Comparable compara por centavos`() {
        assertEquals(-1, MonetaryValue(100L).compareTo(MonetaryValue(101L)))
        assertEquals(0, MonetaryValue(100L).compareTo(MonetaryValue(100L)))
    }

    @Test
    fun `ofReais de zero produz zero`() {
        assertEquals(0L, MonetaryValue.ofReais(0.0).cents)
    }
}
