package br.investcalc.app.data.analytics

/**
 * Fake em memória do [AnalyticsRepository] para os testes JVM — grava todos
 * os eventos no estado observável sem depender de Android/Firebase.
 */
class FakeAnalyticsRepository : AnalyticsRepository {

    val calculos = mutableListOf<Triple<String, Int, Boolean>>()
    val origensTaxas = mutableListOf<String>()
    var tooltips = 0
    val telasVistas = mutableListOf<String>()

    override fun calculationPerformed(type: String, months: Int, hasMonthlyContribution: Boolean) {
        calculos += Triple(type, months, hasMonthlyContribution)
    }

    override fun taxaOrigemUsada(origem: String) {
        origensTaxas += origem
    }

    override fun chartTooltipUsed() {
        tooltips++
    }

    override fun screenViewed(screen: String) {
        telasVistas += screen
    }
}