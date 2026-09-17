package br.investcalc.app.ui.calculator

import br.investcalc.app.core.format.CurrencyFormat
import br.investcalc.app.data.analytics.AnalyticsRepository
import br.investcalc.app.data.cdirate.CdiRateProvider
import br.investcalc.app.data.persistence.SimulationPersistence
import br.investcalc.app.data.persistence.UltimaSimulacao
import br.investcalc.app.domain.calculator.CdbCalculator
import br.investcalc.app.domain.calculator.RateConverter
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import br.investcalc.app.domain.validation.InvestmentInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado e comandos da tela **Calculadora CDI/CDB**.
 *
 * Mantido deliberadamente puro (sem Android/Compose): recebe por injeção as
 * dependências de [AnalyticsRepository], [CdiRateProvider], [SimulationPersistence]
 * e um lambda de conversão de moeda — os testes JVM passam fakes e verificam o
 * fluxo canônico, o evento de analytics, a validação e a persistência sem
 * emulador.
 */
class CdbCalculatorStateHolder(
    private val analytics: AnalyticsRepository,
    private val cdiRateProvider: CdiRateProvider,
    private val persistence: SimulationPersistence,
    private val taxaCdiAnualFixa: Double? = 12.6825,
) {

    data class UiState(
        val valorInicial: String = "",
        val aporteMensal: String = "",
        val meses: String = "",
        val unidadeTempo: String = "meses",
        val percentualCdi: String = "100",
        val taxaCdiAutomatica: Boolean = true,
        val taxaCdiAtualizadaEm: String? = null,
        val isLoading: Boolean = false,
        val mensagemErro: String? = null,
        val resultado: InvestmentResult? = null,
        val taxaMensalEfetivaUsada: Double? = null,
    )

    private val internalState = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = internalState.asStateFlow()

    suspend fun restaurarUltimaSessao() {
        val ultima = persistence.carregarUltimaSimulacao() ?: return
        internalState.update {
            it.copy(
                valorInicial = ultima.valorInicial.let { v -> v.formatarSemSimbolo() },
                aporteMensal = ultima.aporteMensal.let { v -> v.formatarSemSimbolo() },
                meses = ultima.meses.toString(),
                percentualCdi = formatarPercentual(ultima.percentualCdi),
            )
        }
    }

    fun onValorInicialChange(valor: String) {
        internalState.update { it.copy(valorInicial = valor, mensagemErro = null) }
    }

    fun onAporteMensalChange(valor: String) {
        internalState.update { it.copy(aporteMensal = valor, mensagemErro = null) }
    }

    fun onMesesChange(valor: String) {
        internalState.update { it.copy(meses = valor, mensagemErro = null) }
    }

    fun onUnidadeTempoChange(unidade: String) {
        internalState.update { it.copy(unidadeTempo = unidade, mensagemErro = null) }
    }

    fun onPercentualCdiChange(valor: String) {
        internalState.update { it.copy(percentualCdi = valor, mensagemErro = null) }
    }

    suspend fun calcular() {
        val atual = internalState.value
        internalState.update { it.copy(isLoading = true, mensagemErro = null) }

        val taxaAnual = taxaCdiAnualFixa ?: cdiRateProvider.taxaCdiAnualPercentual()
        if (taxaAnual == null) {
            internalState.update {
                it.copy(isLoading = false, mensagemErro = "Não foi possível obter a taxa do CDI. Tente novamente em instantes.")
            }
            return
        }

        val percenResult = InvestmentInputValidator.validarDuracao(atual.meses, atual.unidadeTempo)
        val mesesValidos = percenResult.getOrNull()
        if (mesesValidos == null) {
            internalState.update {
                it.copy(isLoading = false, mensagemErro = percenResult.exceptionOrNull()?.message)
            }
            return
        }

        val valorInicialCentavos = InvestmentInputValidator.validarValorMonetario(
            atual.valorInicial, "investimento inicial",
        ).getOrNull()
        val aporteCentavos = InvestmentInputValidator.validarValorMonetario(
            atual.aporteMensal, "investimento mensal",
        ).getOrNull()
        if (valorInicialCentavos == null || aporteCentavos == null) {
            val msg = listOf(
                valorInicialCentavos to "investimento inicial",
                aporteCentavos to "investimento mensal",
            ).firstOrNull { false }?.second
            internalState.update {
                it.copy(
                    isLoading = false,
                    mensagemErro = if (atual.valorInicial.isBlank()) "Informe o investimento inicial." else "Informe o investimento mensal.",
                )
            }
            return
        }

        val percentualCdi = atual.percentualCdi.toDoubleOrNull() ?: 100.0
        val resultado = CdbCalculator.calcularComCDI(
            valorInicial = MonetaryValue.ofReais(valorInicialCentavos / 100.0),
            aporteMensal = MonetaryValue.ofReais(aporteCentavos / 100.0),
            percentualCdi = percentualCdi,
            taxaCdiAnual = taxaAnual,
            meses = mesesValidos,
        )

        val taxaMensal = RateConverter.anualParaMensal(taxaAnual * (percentualCdi / 100.0))

        persistence.salvarUltimaSimulacao(
            UltimaSimulacao(
                valorInicial = MonetaryValue.ofReais(valorInicialCentavos / 100.0),
                aporteMensal = MonetaryValue.ofReais(aporteCentavos / 100.0),
                percentualCdi = percentualCdi,
                taxaCdiAnual = taxaAnual,
                meses = mesesValidos,
            ),
        )

        analytics.calculationPerformed(
            type = "cdi",
            months = mesesValidos,
            hasMonthlyContribution = aporteCentavos > 0L,
        )

        internalState.update {
            it.copy(
                isLoading = false,
                resultado = resultado,
                taxaMensalEfetivaUsada = taxaMensal,
                taxaCdiAtualizadaEm = null,
                mensagemErro = null,
            )
        }
    }

    private fun formatarPercentual(valor: Double): String =
        valor.toInt().toString()

    private fun MonetaryValue.formatarSemSimbolo(): String =
        CurrencyFormat.format(this.cents).replace(Regex("[^0-9,.\\-]"), "")
}
