package br.investcalc.app.ui.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Task 1: percentual do CDI em texto livre aceita virgula OU ponto. */
class PercentualCdiParsingTest {

    @Test
    fun `aceita decimal com virgula e com ponto`() {
        assertEquals(92.5, "92,5".parseDecimalBr()!!, 1e-9)
        assertEquals(92.5, "92.5".parseDecimalBr()!!, 1e-9)
        assertEquals(100.0, "100".parseDecimalBr()!!, 1e-9)
        assertEquals(110.0, " 110 ".parseDecimalBr()!!, 1e-9)
    }

    @Test
    fun `entradas invalidas viram null`() {
        assertNull("".parseDecimalBr())
        assertNull("abc".parseDecimalBr())
        assertNull("1,2,3".parseDecimalBr())
    }
}