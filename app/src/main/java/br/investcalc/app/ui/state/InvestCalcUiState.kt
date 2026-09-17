package br.investcalc.app.ui.state

import br.investcalc.app.domain.model.InvestmentResult

/**
 * Estado imutável da tela de calculadora de CDB/CDI (UI state).
 *
 * Os campos monetários são mantidos como texto digitado (ex.: "1.000,00") para
 * que a edição espelhe fielmente o teclado; a conversão para [MonetaryValue]
 * acontece apenas no momento do cálculo, onde também ocorrem as validações.
 */
data class InvestCalcUiState(
    val valorInicialTexto: String = "",
    val aporteMensalTexto: String = "",
    val duracaoTexto: String = "",
    val unidadeDuracao: String = "meses",
    val percentualCdi: Int = 100,
    val percentuaisRapidos: List<Int> = listOf(80, 90, 95, 100, 105, 110),
    val taxaCdiTexto: String = "",
    val taxaCdiAutomatica: Boolean = false,
    val taxaCdiAtualizadaEm: String? = null,
    val isCalculando: Boolean = false,
    val erroCampo: String? = null,
    val resultado: InvestmentResult? = null,
    val rendimentoPercentualPeriodo: Double = 0.0,
) {
    val temResultado: Boolean get() = resultado != null
    val temAporteMensal: Boolean get() = aporteMensalTexto.filter(Char::isDigit).isNotEmpty()
}
