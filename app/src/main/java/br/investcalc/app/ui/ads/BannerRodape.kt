package br.investcalc.app.ui.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.investcalc.app.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Banner adaptativo ancorado no rodapé das abas de cálculo (T2/P4).
 *
 * - [adsHabilitado] une Remote Config `ads_enabled` + consentimento do UMP;
 *   quando false a composable não desenha nada (sem quebrar o layout);
 * - tamanho adaptativo (largura da tela) definido ANTES do loadAd;
 * - altura reservada = altura do anúncio, evitando remanejamento de layout.
 */
@Composable
fun BannerRodape(
    adsHabilitado: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!adsHabilitado) return

    val contexto = LocalContext.current
    val larguraDp = LocalConfiguration.current.screenWidthDp
    val tamanhoAdaptativo = remember(contexto, larguraDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(contexto, larguraDp)
    }
    val alturaDp = tamanhoAdaptativo.height.dp

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(alturaDp),
        factory = {
            AdView(it).apply {
                setAdSize(tamanhoAdaptativo)
                adUnitId = BuildConfig.ADMOB_BANNER_AD_ID
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(erro: LoadAdError) { /* tenta de novo no próximo launch */ }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
