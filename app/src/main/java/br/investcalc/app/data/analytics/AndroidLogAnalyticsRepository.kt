package br.investcalc.app.data.analytics

import android.util.Log

/**
 * Implementação de [AnalyticsRepository] que registra o evento via log do
 * Android. Em produção o mesmo contrato se conecta ao Firebase Analytics;
 * o uso por trás da interface garante que a camada de UI/domínio permanece
 * testável em JVM puro.
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
}
