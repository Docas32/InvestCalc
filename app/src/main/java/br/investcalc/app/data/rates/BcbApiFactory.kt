package br.investcalc.app.data.rates

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica do client HTTP do BCB (Retrofit + Gson). Timeout de 5s conforme o
 * contrato de taxas; o retry único (2s ± jitter) é responsabilidade de
 * [BcbRatesRepository]. O parâmetro [baseUrl] permite apontar para um
 * MockWebServer nos testes de resiliência sem mudar a pilha de rede real.
 */
object BcbApiFactory {

    private const val BASE_URL = "https://api.bcb.gov.br/"
    private const val TIMEOUT_SEGUNDOS = 5L

    fun criar(baseUrl: String = BASE_URL): BcbApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BcbApi::class.java)
    }
}