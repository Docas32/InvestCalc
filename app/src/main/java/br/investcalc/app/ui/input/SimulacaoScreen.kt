package br.investcalc.app.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.investcalc.app.domain.calculator.CdbCalculator
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.chart.PatrimonioSeriesBuilder
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import java.util.Locale

/** Taxa DI anual padrão quando ainda não há taxa do BCB (fallback P3). */
const val TAXA_DI_ANUAL_FALLBACK_PERCENTUAL = 10.90

private const val PERCENTUAL_CDI_MIN = 0.0
private const val PERCENTUAL_CDI_MAX = 1000.0

/**
 * Fatia S3 - formulario de simulacao CDI/CDB (hermetico, ASCII puro,
 * sem rede nesta tela; a taxa do BCB chega pronta por parametro).
 *
 * Campos:
 *  - "Percentual do CDI (%)": texto livre (default 100) aceitando decimais
 *    com virgula ou ponto ("92,5" e "92.5" -> 92.5); valida 0 < pct <= 1000;
 *  - "Taxa DI (% a.a.)": preenchida automaticamente pela camada de taxas
 *    (BCB · dd/mm ou "fallback"), editavel pelo usuario;
 *  - taxa mensal efetiva = (1 + (DI/100 * pct/100))^(1/12) - 1, recalculada
 *    AO VIVO a cada digitação (percentual ou DI), sem tocar em Simular;
 *  - "Taxa de juros (% ao mes)" (heranca) fica em **modo avancado**
 *    recolhido; quando preenchido e > 0, sobrescreve a taxa derivada do CDI.
 *
 * O percentual usado no calculo e sempre o lido do campo no momento do toque
 * em Simular (fonte unica de verdade — ver análise da causa raiz P1.1).
 *
 * Fluxo de resultado: devolve [InvestmentResult] (bruto/investido/
 * rendimento/IR/liquido) alem da [PatrimonioSeries] para o grafico.
 */
@Composable
fun SimulacaoScreen(
    onSeriePronta: (PatrimonioSeries) -> Unit,
    onResultadoPronto: (InvestmentResult) -> Unit,
    onVoltar: () -> Unit,
    taxaDiAnualAutomatica: Double,
    taxaDiOrigemLabel: String,
) {
    var valorInicialTexto by remember { mutableStateOf("1000") }
    var aporteMensalTexto by remember { mutableStateOf("200") }
    var mesesTexto by remember { mutableStateOf("12") }
    var percentualCdiTexto by remember { mutableStateOf("100") }
    var taxaDiTexto by remember { mutableStateOf(taxaDiAnualAutomatica.formatarTaxa()) }
    var taxaDiEditadaManualmente by remember { mutableStateOf(false) }
    var taxaMensalManualTexto by remember { mutableStateOf("") }
    var modoAvancado by remember { mutableStateOf(false) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // P3: a taxa do BCB chega de forma assíncrona; quando ela muda, atualiza o
    // campo — a menos que o usuário já o tenha editado à mão.
    LaunchedEffect(taxaDiAnualAutomatica) {
        if (!taxaDiEditadaManualmente) {
            taxaDiTexto = taxaDiAnualAutomatica.formatarTaxa()
        }
    }

    val meses = mesesTexto.trim().toIntOrNull()?.coerceAtLeast(1) ?: 12

    val percentualCdi = percentualCdiTexto.parseDecimalBr()
    val percentualValido = percentualCdi != null &&
        percentualCdi > PERCENTUAL_CDI_MIN &&
        percentualCdi <= PERCENTUAL_CDI_MAX

    val taxaDi = taxaDiTexto.parseDecimalBr() ?: 0.0
    val taxaMensalDerivada = if (percentualValido) {
        taxaMensalEfetiva(taxaDi, percentualCdi!!)
    } else {
        0.0
    }
    val taxaMensalManual = taxaMensalManualTexto.parseDecimalBr() ?: 0.0
    val taxaMensalUsada = if (modoAvancado && taxaMensalManual > 0.0) {
        taxaMensalManual
    } else {
        taxaMensalDerivada
    }
    val linhaTaxaMensal = if (percentualValido || (modoAvancado && taxaMensalManual > 0.0)) {
        "Taxa mensal efetiva: " + taxaMensalUsada.formatarTaxa() +
            "% a.m. (equiv. " +
            (taxaMensalUsada / 100.0).mensalParaAnualPercentual().formatarTaxa() +
            "% a.a.)"
    } else {
        "Taxa mensal efetiva: -- (informe o percentual do CDI)"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Calculadora CDI/CDB",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = valorInicialTexto,
            onValueChange = { valorInicialTexto = it },
            label = { Text("Valor inicial (R$)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = aporteMensalTexto,
            onValueChange = { aporteMensalTexto = it },
            label = { Text("Aporte mensal (R$)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = mesesTexto,
            onValueChange = { mesesTexto = it },
            label = { Text("Meses (1..9999)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = percentualCdiTexto,
            onValueChange = {
                percentualCdiTexto = it
                aviso = null
            },
            label = { Text("Percentual do CDI (%)") },
            singleLine = true,
            isError = !percentualValido,
            supportingText = if (!percentualValido) {
                { Text("Informe um numero maior que 0 e no maximo 1000.") }
            } else {
                null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = taxaDiTexto,
            onValueChange = {
                taxaDiTexto = it
                taxaDiEditadaManualmente = true
            },
            label = { Text("Taxa DI (% a.a.)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = taxaDiOrigemLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = linhaTaxaMensal,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))

        TextButton(onClick = { modoAvancado = !modoAvancado }) {
            Text(if (modoAvancado) "Ocultar modo avancado" else "Modo avancado (taxa mensal direta)")
        }
        if (modoAvancado) {
            OutlinedTextField(
                value = taxaMensalManualTexto,
                onValueChange = { taxaMensalManualTexto = it },
                label = { Text("Taxa de juros (% ao mes)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val valorInicial = valorInicialTexto.parseDecimalBr() ?: 0.0
                    val aporteMensal = aporteMensalTexto.parseDecimalBr() ?: 0.0
                    val usarManual = modoAvancado && taxaMensalManual > 0.0
                    when {
                        !percentualValido ->
                            aviso = "Percentual do CDI deve ser maior que 0 e no maximo 1000."
                        modoAvancado && taxaMensalManual <= 0.0 ->
                            aviso = "Informe a taxa mensal manual > 0 ou use a taxa do CDI."
                        taxaMensalUsada <= 0.0 ->
                            aviso = "Informe a Taxa DI (% a.a.) > 0."
                        valorInicial <= 0.0 && aporteMensal <= 0.0 ->
                            aviso = "Informe valor inicial ou aporte mensal > 0."
                        else -> {
                            aviso = null
                            val serie = PatrimonioSeriesBuilder.construir(
                                valorInicial = valorInicial,
                                aporteMensal = aporteMensal,
                                meses = meses,
                                taxaMensalPercentual = taxaMensalUsada,
                            )
                            val resultado = if (usarManual) {
                                CdbCalculator.calcularComTaxaMensal(
                                    valorInicial = MonetaryValue.ofReais(valorInicial),
                                    aporteMensal = MonetaryValue.ofReais(aporteMensal),
                                    taxaMensalEfetiva = taxaMensalManual / 100.0,
                                    meses = meses,
                                )
                            } else {
                                // pct vem do MESMO texto do campo no instante do clique.
                                CdbCalculator.calcularComTaxaDiAnualPercentual(
                                    valorInicial = MonetaryValue.ofReais(valorInicial),
                                    aporteMensal = MonetaryValue.ofReais(aporteMensal),
                                    percentualCdi = percentualCdi ?: 0.0,
                                    taxaDiAnualPercentual = taxaDi,
                                    meses = meses,
                                )
                            }
                            onSeriePronta(serie)
                            onResultadoPronto(resultado)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Simular")
            }
            Spacer(Modifier.height(0.dp).weight(0.2f))
            TextButton(onClick = onVoltar) {
                Text("Voltar")
            }
        }

        aviso?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** (1 + (DI/100 * pct/100))^(1/12) - 1, em percentual a.m. */
private fun taxaMensalEfetiva(taxaDiAnualPercentual: Double, percentualCdi: Double): Double {
    val diFracao = taxaDiAnualPercentual / 100.0
    val pctFracao = percentualCdi / 100.0
    val fatorAnual = 1.0 + diFracao * pctFracao
    return (Math.pow(fatorAnual, 1.0 / 12.0) - 1.0) * 100.0
}

private fun Double.mensalParaAnualPercentual(): Double =
    (Math.pow(1.0 + this, 12.0) - 1.0) * 100.0

private fun Double.formatarTaxa(): String =
    String.format(Locale.ROOT, "%.4f", this).replace('.', ',').trimEnd('0').trimEnd(',')

/** Aceita decimal com virgula OU ponto ("92,5" e "92.5"); null se invalido. */
internal fun String.parseDecimalBr(): Double? =
    trim().replace(',', '.').toDoubleOrNull()