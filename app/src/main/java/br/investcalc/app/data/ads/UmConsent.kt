package br.investcalc.app.data.ads

import android.app.Activity
import android.content.Context
import br.investcalc.app.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.ConsentStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado do consentimento de anúncios (T2/P4).
 */
enum class Consentimento {
    /** Ainda sem resposta do UMP (primeiro launch / status UNKNOWN). */
    NAO_RESOLVIDO,

    /** Consentimento obtido via formulário do UMP. */
    OBTIDO,

    /** Formulário é obrigatório e ainda não foi respondido / foi recusado. */
    NAO_OBTIDO,

    /** Região fora do EEA — consentimento não exigido (ads liberados). */
    NAO_EXIGIDO,
}

/**
 * Wrapper fino do Google User Messaging Platform (UMP). Responsável pelo
 * ciclo de vida do consentimento:
 *
 *  - [inicializar] no cold start: pede a atualização do status e, se o
 *    formulário for obrigatório, o exibe na primeira abertura;
 *  - [abrirFormulario] no toggle de Ajustes: recarrega e sempre reexibe o
 *    formulário (fluxo de troca de decisão);
 *  - [adsLiberados] expõe `canRequestAds()` do UMP: **sem consentimento não
 *    há banner nem intersticial** (regra T2).
 *
 * Em build DEBUG injeta [BuildConfig.ADMOB_TEST_DEVICE_HASHED_ID] e força a
 * geografia EEA via [ConsentDebugSettings] — jamais em RELEASE.
 */
class UmConsent(private val context: Context) {

    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)

    private val _adsLiberados = MutableStateFlow(consentInformation.canRequestAds())
    private val _estado = MutableStateFlow(lerEstado())

    /** true quando o UMP permite exibir anúncios (canRequestAds). */
    val adsLiberados: StateFlow<Boolean> = _adsLiberados

    /** Estado observável para a UI (descrição do toggle em Ajustes). */
    val estado: StateFlow<Consentimento> = _estado

    /** Cold start: atualiza status e mostra o formulário se exigido. */
    fun inicializar(activity: Activity) {
        val parametros = ConsentRequestParameters.Builder()
            .setConsentDebugSettings(settingsDepuracao())
            .build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            parametros,
            ConsentInformation.OnConsentInfoUpdateSuccessListener { sincronizar() },
            ConsentInformation.OnConsentInfoUpdateFailureListener { sincronizar() },
        )
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
            activity,
            ConsentForm.OnConsentFormDismissedListener { sincronizar() },
        )
    }

    /** Toggle de Ajustes: recarrega o formulário e sempre o reexibe. */
    fun abrirFormulario(activity: Activity) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            UserMessagingPlatform.OnConsentFormLoadSuccessListener { formulario ->
                formulario.show(
                    activity,
                    ConsentForm.OnConsentFormDismissedListener { sincronizar() },
                )
            },
            UserMessagingPlatform.OnConsentFormLoadFailureListener { sincronizar() },
        )
    }

    private fun sincronizar() {
        _adsLiberados.value = consentInformation.canRequestAds()
        _estado.value = lerEstado()
    }

    private fun lerEstado(): Consentimento = when (consentInformation.consentStatus) {
        ConsentStatus.OBTAINED -> Consentimento.OBTIDO
        ConsentStatus.NOT_REQUIRED -> Consentimento.NAO_EXIGIDO
        ConsentStatus.REQUIRED -> Consentimento.NAO_OBTIDO
        else -> Consentimento.NAO_RESOLVIDO
    }

    private fun settingsDepuracao(): ConsentDebugSettings {
        val builder = ConsentDebugSettings.Builder(context)
        if (BuildConfig.DEBUG) {
            builder.setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            builder.addTestDeviceHashedId(BuildConfig.ADMOB_TEST_DEVICE_HASHED_ID)
        }
        return builder.build()
    }
}