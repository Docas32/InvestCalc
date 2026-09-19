package br.investcalc.app.data.remotecfg

import br.investcalc.app.R
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await

/**
 * Ajustes de monetização vindos do Remote Config (T1/T2):
 * `ads_enabled` e `interstitial_frequency`, com os defaults do XML
 * (`remote_config_defaults.xml`), consultados no cold start.
 */
data class AppSettings(
    /** Remote Config `ads_enabled` (false ⇒ banners/intersticial desligados). */
    val adsEnabled: Boolean = true,
    /** Remote Config `interstitial_frequency` (N ≥ 1): 4 = 1 a cada 4. */
    val interstitialFrequency: Int = 4,
)

/** Fonte de ajustes de monetização — interface para os testes JVM. */
interface RemoteConfigAds {
    suspend fun carregar(): AppSettings
}

/**
 * Implementação de produção: lê `ads_enabled` e `interstitial_frequency` do
 * Firebase Remote Config e faz o fetch/activate no cold start. Nunca lança:
 * qualquer falha devolve [AppSettings] com os defaults de fábrica.
 */
class FirebaseRemoteConfigAds : RemoteConfigAds {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }

    override suspend fun carregar(): AppSettings {
        return try {
            val config = remoteConfig
            config.fetchAndActivate().await()
            AppSettings(
                adsEnabled = config.getBoolean(CHAVE_ADS),
                interstitialFrequency = config.getLong(CHAVE_FREQUENCIA).toInt().coerceAtLeast(1),
            )
        } catch (_: Exception) {
            AppSettings()
        }
    }

    private companion object {
        const val CHAVE_ADS = "ads_enabled"
        const val CHAVE_FREQUENCIA = "interstitial_frequency"
    }
}