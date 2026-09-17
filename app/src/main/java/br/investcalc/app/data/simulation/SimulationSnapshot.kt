package br.investcalc.app.data.simulation

import br.investcalc.app.domain.model.MonetaryValue

/**
 * Última simulação persistida (API "retomar de onde parei"): valores usados
 * na CCB da sessão anterior, restaurados na próxima abertura da tela via
 * DataStore. Todo o modelo é expresso em centavos para viagem determinística.
 */
data class UltimateSimulationSnapshot(
    val valorInicial: MonetaryValue,
    val aporteMensal: MonetaryValue,
    val percentualCdi: Int,
    val meses: Int,
    val taxaCdiAnualPercentual: Double,
    val taxaCdiAutomatica: Boolean,
)
