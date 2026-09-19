package br.investcalc.app.data.remotecfg

import br.investcalc.app.R
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
 * Implementação de produção via Firebase Remote Config, com as chaves
 * `fallback_di_aa`, `fallback_selic_aa` e `fallback_ipca_aa` e os defaults do
 * XML (T1/P4: 13.90 / 13.90 / 4.50). Nunca lança: se o Firebase estiver
 * indisponível (sem Play Services, timeouts, valores ausentes ou inválidos)
 * retorna `null` e a cadeia degrada para o XML/fallback.
 */
class FirebaseRemoteConfigRates : RemoteConfigRates {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    override suspend fun carregar(): RatesSnapshot? {
        return try {
            val config = remoteConfig
            config.fetchAndActivate().await()
            val cdi = config.getDouble(CHAVE_DI)
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
        const val CHAVE_DI = "fallback_di_aa"
        const val CHAVE_SELIC = "fallback_selic_aa"
        const val CHAVE_IPCA = "fallback_ipca_aa"
    }
}