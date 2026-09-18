package br.investcalc.app.data.rates

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Fábrica do client HTTP do BCB (Retrofit + Gson). Timeout de 5s conforme o
 * contrato P3; o retry único é responsabilidade de [BcbRatesRepository].
 */
object BcbApiFactory {

    private const val BASE_URL = "https://api.bcb.gov.br/"
    private const val TIMEOUT_SEGUNDOS = 5L

    fun criar(): BcbApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BcbApi::class.java)
    }
}