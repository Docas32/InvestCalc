package br.investcalc.app.data.rates

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * P3 — a camada de taxas passa a ler as séries 1178 (Selic anual) e 13522
 * (IPCA 12m) além da 4389 (CDI), cada uma degradando para o fallback por
 * campo. Reutiliza o BcbApi existente (só muda o código da série).
 */
class BcbSeriesTesouroTest {

    private class FakeBcbPorSerie : BcbApi {
        override suspend fun obterSerie(codigo: Int, quantidade: Int): List<BcbSeriePoint> =
            when (codigo) {
                SERIE_CDI_ANUAL -> listOf(BcbSeriePoint("16/09/2026", "13.90"))
                SERIE_SELIC_ANUAL -> listOf(BcbSeriePoint("15/09/2026", "10.65"))
                else -> listOf(BcbSeriePoint("14/09/2026", "4.50"))
            }
    }

    @Test
    fun `selic 1178 e ipca 13522 entram no snapshot`() = runBlocking {
        val repo = BcbRatesRepository(api = FakeBcbPorSerie())
        val s = repo.taxaCdiAnual()
        assertEquals(13.90, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(10.65, s.taxaSelicAnualPercentual, 1e-9)
        assertEquals(4.50, s.variacaoIpca12MesesPercentual, 1e-9)
    }

    @Test
    fun `falha total degrada para fallback selic 13 90 ipca 4 50`() = runBlocking {
        val repo = BcbRatesRepository(
            api = object : BcbApi {
                override suspend fun obterSerie(codigo: Int, quantidade: Int): List<BcbSeriePoint> =
                    throw RuntimeException("sem rede")
            },
            retryAtrasoMs = { 0L },
        )
        val s = repo.taxaCdiAnual()
        assertEquals(13.90, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(13.90, s.taxaSelicAnualPercentual, 1e-9)
        assertEquals(4.50, s.variacaoIpca12MesesPercentual, 1e-9)
    }

    @Test
    fun `falha de uma serie nao derruba as demais`() = runBlocking {
        val repo = BcbRatesRepository(api = object : BcbApi {
            override suspend fun obterSerie(codigo: Int, quantidade: Int): List<BcbSeriePoint> =
                if (codigo == SERIE_CDI_ANUAL) listOf(BcbSeriePoint("16/09/2026", "13.90"))
                else throw RuntimeException("indisponível")
        })
        val s = repo.taxaCdiAnual()
        assertEquals(13.90, s.taxaCdiAnualPercentual, 1e-9)
        assertEquals(13.90, s.taxaSelicAnualPercentual, 1e-9)
        assertEquals(4.50, s.variacaoIpca12MesesPercentual, 1e-9)
    }
}