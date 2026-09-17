package br.investcalc.app.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.investcalc.app.data.analytics.AndroidLogAnalyticsRepository
import br.investcalc.app.data.cdirate.StaticCdiRateProvider
import br.investcalc.app.data.persistence.InMemorySimulationPersistence
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel fino da tela Calculadora CDI/CDB.
 *
 * Contém apenas a lógica de ciclo de vida Android (coroutine + exposição do
 * [StateFlow]); toda a regra vive no [CdbScreenStateHolder], herméticamente
 * testável na JVM sem dependências do framework.
 */
class CdbViewModel(
    private val holder: CdbScreenStateHolder,
) : ViewModel() {

    val state: StateFlow<CdbScreenUiState> = holder.state

    init {
        viewModelScope.launch {
            holder.carregarUltimaSessao()
        }
    }

    fun onValorInicialChange(valor: String) {
        viewModelScope.launch { holder.onValorInicialChange(valor) }
    }

    fun onAporteMensalChange(valor: String) {
        viewModelScope.launch { holder.onAporteMensalChange(valor) }
    }

    fun onDuracaoChange(valor: String) {
        viewModelScope.launch { holder.onDuracaoChange(valor) }
    }

    fun onUnidadeTempoChange(valor: String) {
        viewModelScope.launch { holder.onUnidadeTempoChange(valor) }
    }

    fun onPercentualCdiChange(percentual: Int) {
        viewModelScope.launch { holder.onPercentualCdiChange(percentual) }
    }

    fun onTaxaCdiAnualChange(valor: String) {
        viewModelScope.launch { holder.onTaxaCdiAnualChange(valor) }
    }

    fun calcular() {
        viewModelScope.launch { holder.calcular() }
    }

    companion object {
        /** Taxa do CDI herdada no StaticCdiRateProvider (12,6825% a.a.). */
        const val TAXA_CDI_ANUAL_PERCENTUAL = 12.6825

        /**
         * Fábrica do ViewModel que monta as dependências reais da tela:
         * analytics via log, taxa do CDI estática e persistência em memória.
         */
        fun factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val holder = CdbScreenStateHolder(
                    analyticsRepository = AndroidLogAnalyticsRepository(),
                    cdiRateProvider = StaticCdiRateProvider(taxaCdiAnualPercentual = TAXA_CDI_ANUAL_PERCENTUAL),
                    persistence = InMemorySimulationPersistence(),
                )
                return CdbViewModel(holder) as T
            }
        }
    }
}
