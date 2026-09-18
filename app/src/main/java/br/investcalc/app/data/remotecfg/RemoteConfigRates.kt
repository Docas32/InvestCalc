package br.investcalc.app.data.remotecfg

import br.investcalc.app.data.rates.RatesSnapshot
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

/**
 * Camada de "Remote Config" da cadeia de resiliência de taxas (T1-v2),
 * consultada pelo repositório somente quando o BCB falha e não há cache
 * stale. Retorna `null` quando indisponível — a cadeia segue para o XML.
 */
interface RemoteConfigRates {
    suspend fun carregar(): RatesSnapshot?
}

/**
 * Implementação de produção via Firebase Remote Config. Nunca lança: se o
 * Firebase estiver indisponível (sem Play Services, timeouts, valores
 * ausentes ou inválidos) retorna `null` e a cadeia degrada para o degradante
 * XML/falback. O fetch é ativo mas com curto prazo de frescor do cache
 * padrão do RC — o contrato "UI nunca bloqueia" vale por ser suspenso.
 */
class FirebaseRemoteConfigRates(
    private val cacheMaxAgeSegundos: Long = 600L,
) : RemoteConfigRates {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance()
    }

    override suspend fun carregar(): RatesSnapshot? {
        return try {
            val config = remoteConfig
            config.fetchAndActivate().await()
            val cdi = config.getDouble(CHAVE_CDI)
            val selic = config.getDouble(CHAVE_SELIC)
            val ipca = config.getDouble(CHAVE_IPCA)
            if (cdi <= 0.0 || selic <= 0.0 || ipca <= 0.0) null
            else RatesSnapshot(
                taxaCdiAnualPercentual = cdi,
                taxaSelicAnualPercentual = selic,
                variacaoIpca12MesesPercentual = ipca,
                atualizadaEm = null,
            )
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val CHAVE_CDI = "cdi_anual_percentual_default"
        const val CHAVE_SELIC = "selic_anual_percentual_default"
        const val CHAVE_IPCA = "ipca_12m_percentual_default"
    }
}