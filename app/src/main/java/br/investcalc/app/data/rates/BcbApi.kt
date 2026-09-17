package br.investcalc.app.data.rates

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Interface Retrofit de acesso à SGS do Banco Central do Brasil.
 *
 * Endpoint real (documento de referência oficial da API do BCB — SGS):
 * `GET https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados/ultimos/{quantidade}?formato=json`
 *
 * Resposta (formato=json): array de `{"data":"dd/mm/aaaa","valor":"0,98"}`,
 * onde [valor] usa VÍRGULA decimal brasileira. A anualização dos 12 meses do
 * CDI fica a cargo de [RatesAnnualizer] (domínio), o fallback em
 * [RatesFallback] e o TTL de 12h em [RatesRepository].
 */
interface BcbApi {
    @GET("dados/serie/bcdata.sgs.{codigo}/dados/ultimos/{quantidade}?formato=json")
    suspend fun obterSerie(
        @Path("codigo") codigo: Int,
        @Path("quantidade") quantidade: Int = 13,
    ): List<BcbSeriePoint>
}
