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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.chart.PatrimonioSeriesBuilder
import java.util.Locale

/**
 * Fatia S3 - formulario de simulacao (hermetico, ASCII puro, sem rede,
 * sem Firebase). Coleta entradas no celular e constroi a seriom patrimonio
 * via [PatrimonioSeriesBuilder.construir], casando exatamente o contrato
 * selado em disco. Nunca lanca: entradas invalidas viram 0.0/serie vazia
 * sinalizada para a tela, que sempre decide o que renderizar.
 *
 * A taxa mensal e obrigatoria: vazia ou zero bloqueia o Simular (nunca se
 * calcula silenciosamente com 0%).
 */
@Composable
fun SimulacaoScreen(
    onSeriePronta: (PatrimonioSeries) -> Unit,
    onVoltar: () -> Unit,
) {
    val taxaMensalDefault = TAXA_CDI_ANUAL_PERCENTUAL.taxaAnualParaMensal()

    var valorInicialTexto by remember { mutableStateOf("1000") }
    var aporteMensalTexto by remember { mutableStateOf("200") }
    var mesesTexto by remember { mutableStateOf("12") }
    var taxaMensalTexto by remember { mutableStateOf(taxaMensalDefault.formatarTaxa()) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val lerDouble = { texto: String -> texto.trim().replace(',', '.').toDoubleOrNull() ?: 0.0 }
    val meses = mesesTexto.trim().toIntOrNull()?.coerceAtLeast(1) ?: 12

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Simulacao (juros compostos)",
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
            value = taxaMensalTexto,
            onValueChange = { taxaMensalTexto = it },
            label = { Text("Taxa de juros (% ao mes)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Taxa em uso: " + lerDouble(taxaMensalTexto).formatarTaxa() + "% a.m. (equiv. "
                + TAXA_CDI_ANUAL_PERCENTUAL.formatarTaxa() + "% a.a.)",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    val valorInicial = lerDouble(valorInicialTexto)
                    val aporteMensal = lerDouble(aporteMensalTexto)
                    val taxaMensal = lerDouble(taxaMensalTexto)
                    if (taxaMensal <= 0.0) {
                        aviso = "Informe a taxa de juros mensal > 0."
                    } else if (valorInicial <= 0.0 && aporteMensal <= 0.0) {
                        aviso = "Informe valor inicial ou aporte mensal > 0."
                    } else {
                        aviso = null
                        val serie = PatrimonioSeriesBuilder.construir(
                            valorInicial = valorInicial,
                            aporteMensal = aporteMensal,
                            meses = meses,
                            taxaMensalPercentual = taxaMensal,
                        )
                        onSeriePronta(serie)
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

/** Taxa anual herdada do CDI usada como default na tela (10,90% a.a.). */
private const val TAXA_CDI_ANUAL_PERCENTUAL = 10.90

private fun Double.taxaAnualParaMensal(): Double =
    (Math.pow(1.0 + this / 100.0, 1.0 / 12.0) - 1.0) * 100.0

private fun Double.formatarTaxa(): String =
    String.format(Locale.ROOT, "%.2f", this).replace('.', ',')