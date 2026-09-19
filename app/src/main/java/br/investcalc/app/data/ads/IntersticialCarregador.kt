package br.investcalc.app.data.ads

import android.app.Activity
import android.content.Context
import br.investcalc.app.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Pré-carrega e exibe o anúncio intersticial (IDs de TESTE em BuildConfig).
 *
 * - [carregar]: pré-carrega; fica a cargo da UI (AppNavHost) chamá-lo quando o
 *   consentimento do UMP liberar e após cada fechamento;
 * - [exibirEm]: mostra o anúncio se houver um carregado e devolve true;
 *   senão devolve false — quem decide QUANDO é o [InterstitialGate], aqui só
 *   o SDK.
 */
class IntersticialCarregador(context: Context) {

    private val contexto = context.applicationContext
    private var intersticial: InterstitialAd? = null
    private var carregando = false

    /** Solicita o pré-carregamento (no-op se já houver anúncio ou request ativa). */
    fun carregar() {
        if (intersticial != null || carregando) return
        carregando = true
        InterstitialAd.load(
            contexto,
            BuildConfig.ADMOB_INTERSTITIAL_AD_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    carregando = false
                    intersticial = ad
                }

                override fun onAdFailedToLoad(erroLoad: LoadAdError) {
                    carregando = false
                    intersticial = null
                }
            },
        )
    }

    /** Exibe se disponível; devolve true quando exibido. Recarrega ao fechar. */
    fun exibirEm(activity: Activity): Boolean {
        val ad = intersticial ?: return false
        intersticial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                carregar()
            }

            override fun onAdFailedToShowFullScreenContent(erroShow: AdError) {
                intersticial = null
            }
        }
        ad.show(activity)
        return true
    }
}