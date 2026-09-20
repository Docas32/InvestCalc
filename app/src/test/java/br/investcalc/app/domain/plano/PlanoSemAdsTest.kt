package br.investcalc.app.domain.plano

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contrato do plano sem ads (T4/P7): transição de estado sem Play / comprado /
 * não comprado e restauração de compras via Play.
 */
class PlanoSemAdsTest {

    private class FakePersistence(var comprado: Boolean = false) : PlanoSemAdsPersistence {
        override suspend fun ler(): Boolean = comprado
        override suspend fun gravar(comprado: Boolean) {
            this.comprado = comprado
        }
    }

    @Test
    fun `sem Play disponivel no aparelho o card esconde-se (indisponivel)`() {
        val estado = PlanoSemAds.estado(billingDisponivel = false, compradoPersistido = false)
        assertEquals(EstadoPlanoSemAds.INDISPONIVEL, estado)
    }

    @Test
    fun `compra persistida leva a comprado`() {
        val estado = PlanoSemAds.estado(billingDisponivel = true, compradoPersistido = true)
        assertEquals(EstadoPlanoSemAds.COMPRADO, estado)
    }

    @Test
    fun `sem compra persistida leva a nao comprado`() {
        val estado = PlanoSemAds.estado(billingDisponivel = true, compradoPersistido = false)
        assertEquals(EstadoPlanoSemAds.NAO_COMPRADO, estado)
    }

    @Test
    fun `instalacao limpa mesmo com Play disponivel aparece como nao comprado`() {
        val estado = PlanoSemAds.estado(billingDisponivel = true, compradoPersistido = false)
        assertEquals(EstadoPlanoSemAds.NAO_COMPRADO, estado)
    }

    @Test
    fun `restauracao confirmada no Play grava na persistencia`() = runBlocking {
        val fake = FakePersistence()
        val estado = PlanoSemAds.aposRestauracao(compradoNoPlay = true, persistencia = fake)
        assertEquals(EstadoPlanoSemAds.COMPRADO, estado)
        assertTrue(fake.comprado)
    }

    @Test
    fun `restauracao negada no Play nao grava nada`() = runBlocking {
        val fake = FakePersistence()
        val estado = PlanoSemAds.aposRestauracao(compradoNoPlay = false, persistencia = fake)
        assertEquals(EstadoPlanoSemAds.NAO_COMPRADO, estado)
        assertFalse(fake.comprado)
    }

    @Test
    fun `indisponivel - tentativa de compra e recusada`() {
        val tentativa = PlanoSemAds.tentarComprar(
            billingDisponivel = false,
            compradoPersistido = false,
            produtoEncontrado = false,
        )
        assertEquals(DesfechoCompra.RECUSADA_INDISPONIVEL, tentativa)
    }

    @Test
    fun `play conectado sem produto - tentativa de compra recusada`() {
        val tentativa = PlanoSemAds.tentarComprar(
            billingDisponivel = true,
            compradoPersistido = false,
            produtoEncontrado = false,
        )
        assertEquals(DesfechoCompra.RECUSADA_INDISPONIVEL, tentativa)
    }

    @Test
    fun `play conectado com produto - tentativa de compra abre o fluxo`() {
        val tentativa = PlanoSemAds.tentarComprar(
            billingDisponivel = true,
            compradoPersistido = false,
            produtoEncontrado = true,
        )
        assertEquals(DesfechoCompra.ABERTA, tentativa)
    }

    @Test
    fun `compra possivel exige o produto devolvido pelo play`() {
        assertTrue(PlanoSemAds.compraPossivel(billingDisponivel = true, produtoEncontrado = true))
        assertFalse(PlanoSemAds.compraPossivel(billingDisponivel = true, produtoEncontrado = false))
        assertFalse(PlanoSemAds.compraPossivel(billingDisponivel = false, produtoEncontrado = true))
    }

    @Test
    fun `restauracao encontrada vira feedback de compra encontrada`() {
        assertEquals(ResultadoRestauracao.COMPRA_ENCONTRADA, PlanoSemAds.resultadoDaRestauracao(true))
    }

    @Test
    fun `restauracao sem compra vira feedback de nenhuma compra`() {
        assertEquals(ResultadoRestauracao.NENHUMA_COMPRA, PlanoSemAds.resultadoDaRestauracao(false))
    }

    @Test
    fun `produto do plano e o cadastrado no Play Console`() {
        assertEquals("remover_anuncios", PlanoSemAds.PRODUTO_ID)
    }
}