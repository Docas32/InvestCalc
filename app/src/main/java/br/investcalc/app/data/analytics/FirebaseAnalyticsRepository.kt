package br.investcalc.app.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Implementação de produção de [AnalyticsRepository] sobre o Firebase
 * Analytics (BoM). Eventos do contrato (T1/P4):
 *
 *  - `calculation_performed` → type, months, has_aporte;
 *  - `rates_origin` → origem (cache_fresca|bcb|cache_stale|remote_config|xml|fallback);
 *  - `chart_tooltip_used` → tooltip do gráfico;
 *  - `screen_view` → `screen_name` nas 5 abas.
 *
 * Nenhum destes métodos bloqueia: o Firebase Analytics enfileira o evento via
 * binder do Google Play Services; a UI segue 100% livre.
 */
class FirebaseAnalyticsRepository(context: Context) : AnalyticsRepository {

    private val firebase: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun calculationPerformed(
        type: String,
        months: Int,
        hasMonthlyContribution: Boolean,
    ) {
        firebase.logEvent(
            EVENTO_CALCULO,
            Bundle().apply {
                putString(PARAM_TIPO, type)
                putInt(PARAM_MESES, months)
                putInt(PARAM_HAS_APORTE, if (hasMonthlyContribution) 1 else 0)
            },
        )
    }

    override fun taxaOrigemUsada(origem: String) {
        firebase.logEvent(
            EVENTO_ORIGEM_TAXAS,
            Bundle().apply {
                putString(PARAM_ORIGEM, origem)
            },
        )
    }

    override fun chartTooltipUsed() {
        firebase.logEvent(EVENTO_TOOLTIP, null)
    }

    override fun screenViewed(screen: String) {
        firebase.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, screen)
            },
        )
    }

    private companion object {
        const val EVENTO_CALCULO = "calculation_performed"
        const val EVENTO_ORIGEM_TAXAS = "rates_origin"
        const val EVENTO_TOOLTIP = "chart_tooltip_used"

        const val PARAM_TIPO = "type"
        const val PARAM_MESES = "months"
        const val PARAM_HAS_APORTE = "has_aporte"
        const val PARAM_ORIGEM = "origem"
    }
}