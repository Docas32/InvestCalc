package br.investcalc.app.ui.calculator

import br.investcalc.app.data.analytics.AnalyticsRepository
import br.investcalc.app.data.cdirate.CdiRateProvider
import br.investcalc.app.data.persistence.SimulationPersistence
import br.investcalc.app.data.persistence.UltimaSimulacao
import br.investcalc.app.domain.calculator.CdbCalculator
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import br.investcalc.app.domain.validation.InvestmentInputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Estado imutável da tela Calculadora CDI/CDB.
 *
 * Os campos monetários são mantidos como texto (edição), e o resultado
 * calculado carrega valores monetários em centavos.
 */
data class CdbScreenUiState(
    val valorInicial: String = "",
    val aporteMensal: String = "",
    val duracao: String = "",
    val unidadeTempo: String = UNIDADE_TEMPO_MESES,
    val percentualCdi: Int = PERCENTUAL_CDI_PADRAO,
    val taxaCdiAnual: String = "",
    val taxaCdiAutomatica: Boolean = true,
    val taxaCdiAtualizadaEm: String = "",
    val carregando: Boolean = false,
    val erroCampo: String? = null,
    val resultado: InvestmentResult? = null,
) {
    companion object {
        const val UNIDADE_TEMPO_MESES = "meses"
        const val UNIDADE_TEMPO_ANOS = "anos"
        const val PERCENTUAL_CDI_PADRAO = 100
        const val DIAS_POR_MES = 30.44
    }
}

/**
 * Detentor puro de estado JVM para a tela Calculadora CDI/CDB.
 *
 * É herméticamente testável (sem Android): injeta fakes de analytics, taxa do
 * CDI e persistência, e expõe um [StateFlow] com o estado atual da tela.
 */
class CdbScreenStateHolder(
    private val analyticsRepository: AnalyticsRepository,
    private val cdiRateProvider: CdiRateProvider,
    private val persistence: SimulationPersistence,
) {

    private val _state = MutableStateFlow(CdbScreenUiState())
    val state: StateFlow<CdbScreenUiState> = _state.asStateFlow()

    /**
     * Restaura a taxa automática do CDI (com badge "atualizada em dd/mm") e
     * a última simulação persistida, se houver.
     */
    suspend fun carregarUltimaSessao() {
        val taxaCdi = cdiRateProvider.taxaCdiAnualPercentual()
        val hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM"))

        val atual = _state.value
        val comTaxa = if (taxaCdi != null) {
            atual.copy(
                taxaCdiAtualizadaEm = hoje,
                taxaCdiAutomatica = true,
                taxaCdiAnual = formatarTaxa(taxaCdi),
            )
        } else {
            atual.copy(taxaCdiAtualizadaEm = "")
        }

        val ultima = persistence.carregarUltimaSimulacao()
        _state.value = if (ultima != null) {
            comTaxa.copy(
                valorInicial = formatarCentavos(ultima.valorInicial.cents),
                aporteMensal = formatarCentavos(ultima.aporteMensal.cents),
                duracao = ultima.meses.toString(),
                unidadeTempo = CdbScreenUiState.UNIDADE_TEMPO_MESES,
                percentualCdi = ultima.percentualCdi.roundToInt(),
                taxaCdiAnual = formatarTaxa(ultima.taxaCdiAnual),
            )
        } else {
            comTaxa
        }
    }

    fun onValorInicialChange(valor: String) {
        _state.value = _state.value.copy(valorInicial = valor, erroCampo = null)
    }

    fun onAporteMensalChange(valor: String) {
        _state.value = _state.value.copy(aporteMensal = valor, erroCampo = null)
    }

    fun onDuracaoChange(valor: String) {
        _state.value = _state.value.copy(duracao = valor, erroCampo = null)
    }

    fun onUnidadeTempoChange(valor: String) {
        _state.value = _state.value.copy(unidadeTempo = valor, erroCampo = null)
    }

    fun onPercentualCdiChange(percentual: Int) {
        _state.value = _state.value.copy(percentualCdi = percentual, erroCampo = null)
    }

    fun onTaxaCdiAnualChange(valor: String) {
        _state.value =
            _state.value.copy(taxaCdiAnual = valor, taxaCdiAutomatica = valor.isBlank(), erroCampo = null)
    }

    /** Executa validação, cálculo, analytics e persistência da última sessão. */
    suspend fun calcular() {
        val atual = _state.value

        val valorInicialCents = InvestmentInputValidator.validarValorMonetario(atual.valorInicial, "Valor inicial")
            .getOrElse {
                _state.value = atual.copy(erroCampo = "valorInicial")
                return
            }
        val aporteMensalCents = InvestmentInputValidator.validarValorMonetario(atual.aporteMensal, "Aporte mensal")
            .getOrElse {
                _state.value = atual.copy(erroCampo = "aporteMensal")
                return
            }
        val meses = InvestmentInputValidator.validarDuracao(atual.duracao, atual.unidadeTempo)
            .getOrElse {
                _state.value = atual.copy(erroCampo = "duracao")
                return
            }
        if (atual.percentualCdi <= 0) {
            _state.value = atual.copy(erroCampo = "percentualCdi")
            return
        }
        val taxaCdiAnual = cdiRateProvider.taxaCdiAnualPercentual()
        if (taxaCdiAnual == null || taxaCdiAnual <= 0.0) {
            _state.value = atual.copy(erroCampo = "taxaCdi")
            return
        }

        _state.value = atual.copy(carregando = true, erroCampo = null)

        val resultado = CdbCalculator.calcularComCDI(
            valorInicial = MonetaryValue(valorInicialCents),
            aporteMensal = MonetaryValue(aporteMensalCents),
            percentualCdi = atual.percentualCdi.toDouble(),
            taxaCdiAnual = taxaCdiAnual,
            meses = meses,
        )

        _state.value = atual.copy(
            carregando = false,
            erroCampo = null,
            resultado = resultado,
        )

        analyticsRepository.calculationPerformed(
            type = "cdi",
            months = meses,
            hasMonthlyContribution = aporteMensalCents > 0L,
        )

        persistence.salvarUltimaSimulacao(
            UltimaSimulacao(
                valorInicial = MonetaryValue(valorInicialCents),
                aporteMensal = MonetaryValue(aporteMensalCents),
                percentualCdi = atual.percentualCdi.toDouble(),
                taxaCdiAnual = taxaCdiAnual,
                meses = meses,
            ),
        )
    }

    private fun formatarTaxa(taxa: Double): String {
        val texto = "%.4f".format(taxa).trimEnd('0').trimEnd('.')
        return texto.replace('.', ',')
    }

    private fun formatarCentavos(cents: Long): String {
        val reais = cents / 100L
        val centavos = (cents % 100L).takeIf { it >= 0L } ?: -cents % 100L
        val negativo = cents < 0L
        val base = "$reais,${(centavos).toString().padStart(2, '0')}"
        return if (negativo) "-$base" else base
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
