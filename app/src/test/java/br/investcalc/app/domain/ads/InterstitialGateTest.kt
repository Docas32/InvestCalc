package br.investcalc.app.domain.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [InterstitialGate] (T2/P4) — lógica pura do "quando exibir o
 * intersticial", com frequência default 4 (Remote Config):
 *
 *  - ads desabilitado ⇒ nunca exibe;
 *  - nunca no 1º cálculo da sessão;
 *  - 1 exibição a cada N (múltiplo exato de `frequencia`);
 *  - nunca em sequência imediata (anterior exibiu).
 */
class InterstitialGateTest {

    private fun exibe(
        total: Int,
        frequencia: Int = 4,
        adsHabilitado: Boolean = true,
        anteriorExibiu: Boolean = false,
    ): Boolean = InterstitialGate.deveExibir(
        adsHabilitado = adsHabilitado,
        frequencia = frequencia,
        calculosJaInvocados = total,
        intersticialExibidoNoCalculoAnterior = anteriorExibiu,
    )

    @Test
    fun `primeiro calculo nunca exibe`() {
        assertFalse(exibe(total = 1))
    }

    @Test
    fun `segundo calculo com frequencia 2 exibe`() {
        assertTrue(exibe(total = 2, frequencia = 2))
    }

    @Test
    fun `quarto calculo com frequencia 4 exibe`() {
        assertTrue(exibe(total = 4))
    }

    @Test
    fun `quinto calculo nunca exibe por sequencia imediata`() {
        // O 4º exibiu ⇒ o 5º é bloqueado pela anti-sequência.
        assertFalse(exibe(total = 5, anteriorExibiu = true))
        // E também não é múltiplo de 4 sem o flag.
        assertFalse(exibe(total = 5))
    }

    @Test
    fun `oitavo calculo exibe de novo`() {
        // 8 % 4 == 0 e o 7º não exibiu.
        assertTrue(exibe(total = 8))
    }

    @Test
    fun `nao multiplo de frequencia nunca exibe`() {
        assertFalse(exibe(total = 2))
        assertFalse(exibe(total = 3))
        assertFalse(exibe(total = 6))
        assertFalse(exibe(total = 7))
    }

    @Test
    fun `ads desabilitado nunca exibe mesmo no multiplo`() {
        assertFalse(exibe(total = 4, adsHabilitado = false))
        assertFalse(exibe(total = 8, adsHabilitado = false))
    }

    @Test
    fun `frequencia invalida nunca exibe`() {
        assertFalse(exibe(total = 100, frequencia = 0))
        assertFalse(exibe(total = 100, frequencia = -1))
    }

    @Test
    fun `sequencia com frequencia 4 alterna corretamente`() {
        // Sessão inteira: a cada cálculo decidimos com o flag do anterior.
        var anteriorExibiu = false
        val exibidos = mutableListOf<Int>()
        for (total in 1..12) {
            val exibir = InterstitialGate.deveExibir(
                adsHabilitado = true,
                frequencia = 4,
                calculosJaInvocados = total,
                intersticialExibidoNoCalculoAnterior = anteriorExibiu,
            )
            if (exibir) exibidos += total
            anteriorExibiu = exibir
        }
        // Só os múltiplos de 4 exibem (nunca consecutivos): 4, 8 e 12.
        assertEquals(listOf(4, 8, 12), exibidos)
    }
}