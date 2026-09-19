package br.investcalc.app.data.analytics

import android.util.Log

/**
 * Implementação de [AnalyticsRepository] que registra o evento via log do
 * Android — usada como apoio de depuração e para manter a camada de
 * UI/domínio testável. Em produção o contrato é servido por
 * [FirebaseAnalyticsRepository].
 */
class AndroidLogAnalyticsRepository(private val tag: String = "InvestCalc-Analytics") :
    AnalyticsRepository {

    override fun calculationPerformed(
        type: String,
        months: Int,
        hasMonthlyContribution: Boolean,
    ) {
        Log.i(
            tag,
            "calculation_performed " +
                "type=$type months=$months " +
                "hasMonthlyContribution=$hasMonthlyContribution",
        )
    }

    override fun taxaOrigemUsada(origem: String) {
        Log.i(tag, "rates_origin origem=$origem")
    }

    override fun chartTooltipUsed() {
        Log.i(tag, "chart_tooltip_used")
    }

    override fun screenViewed(screen: String) {
        Log.i(tag, "screen_view screen=$screen")
    }
}