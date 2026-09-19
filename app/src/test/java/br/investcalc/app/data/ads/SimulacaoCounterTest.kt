package br.investcalc.app.data.ads

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes do [MemoriaSimulacaoCounter] (T2/P4) — contador de simulações que
 * alimenta o [InterstitialGate].
 */
class SimulacaoCounterTest {

    @Test
    fun `incrementa a partir de zero`() = runBlocking {
        val contador = MemoriaSimulacaoCounter()
        assertEquals(1, contador.incrementar())
        assertEquals(2, contador.incrementar())
        assertEquals(2, contador.valor())
    }

    @Test
    fun `gera a partir de um valor incial`() = runBlocking {
        val contador = MemoriaSimulacaoCounter(initial = 10)
        assertEquals(10, contador.valor())
        assertEquals(11, contador.incrementar())
    }
}