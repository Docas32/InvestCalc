package br.investcalc.app.data.rates

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regressão do bug encontrado em campo: a série 4389 do BCB vem com PONTO
 * decimal (`"13.90"`), e o parser antigo removia o ponto como se fosse
 * separador de milhar, anualizando para ~3.7e34. Também cobre a vírgula.
 */
class BcbRatesRepositoryTest {

    private class FakeBcbApi(
        private val pontos: List<BcbSeriePoint> = emptyList(),
        private val erro: Boolean = false,
    ) : BcbApi {
        override suspend fun obterSerie(codigo: Int, quantidade: Int): List<BcbSeriePoint> {
            if (erro) throw RuntimeException("sem rede")
            return pontos
        }
    }

    @Test
    fun `valor com ponto decimal vira taxa anual correta`() = runBlocking {
        val repo = BcbRatesRepository(
            api = FakeBcbApi(listOf(BcbSeriePoint("16/09/2026", "13.90"))),
        )
        assertEquals(13.90, repo.taxaCdiAnual().taxaCdiAnualPercentual, 1e-9)
    }

    @Test
    fun `valor com virgula decimal vira taxa anual correta`() = runBlocking {
        val repo = BcbRatesRepository(
            api = FakeBcbApi(listOf(BcbSeriePoint("16/09/2026", "13,90"))),
        )
        assertEquals(13.90, repo.taxaCdiAnual().taxaCdiAnualPercentual, 1e-9)
    }

    @Test
    fun `escolhe o ponto mais recente independente da ordem`() = runBlocking {
        val repo = BcbRatesRepository(
            api = FakeBcbApi(
                listOf(
                    BcbSeriePoint("16/09/2026", "13.90"),
                    BcbSeriePoint("14/09/2026", "12.50"),
                ),
            ),
        )
        assertEquals(13.90, repo.taxaCdiAnual().taxaCdiAnualPercentual, 1e-9)
    }

    @Test
    fun `falha de rede degrada para o fallback 10 90`() = runBlocking {
        val repo = BcbRatesRepository(
            api = FakeBcbApi(erro = true),
            retryAtrasoMs = { 0L },
        )
        assertEquals(
            RatesFallback.TAXA_CDI_ANUAL_PERCENTUAL,
            repo.taxaCdiAnual().taxaCdiAnualPercentual,
            1e-9,
        )
    }
}