package br.investcalc.app.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.data.rates.RatesSnapshot
import br.investcalc.app.domain.calculator.ComparadorCalculator
import br.investcalc.app.domain.calculator.ComparadorResultado
import br.investcalc.app.domain.model.MonetaryValue
import br.investcalc.app.ui.chart.formatarMoeda
import br.investcalc.app.ui.input.TAXA_DI_ANUAL_FALLBACK_PERCENTUAL
import br.investcalc.app.ui.input.formatarTaxaUi
import br.investcalc.app.ui.input.parseDecimalBr
import kotlin.math.abs

/**
 * Aba Comparar — CDB × LCI/LCA lado a lado sobre o **mesmo** DI da camada de
 * taxas existente (não editável nesta tela).
 *
 * - CDB com IR regressivo; LCI/LCA isenta (alíquota 0);
 * - vencedor destacado + diferença em R$;
 * - linha educativa: "LCI a X% equivale a um CDB a Y% neste prazo", com
 *   Y = pctLCI / (1 − IR do prazo).
 */
@Composable
fun ComparadorScreen(
    taxas: RatesSnapshot,
    taxaOrigemLabel: String,
) {
    var valorInicialTexto by remember { mutableStateOf("1000") }
    var aporteMensalTexto by remember { mutableStateOf("0") }
    var mesesTexto by remember { mutableStateOf("12") }
    var percentualCdbTexto by remember { mutableStateOf("100") }
    var percentualLciTexto by remember { mutableStateOf("90") }
    var resultado by remember { mutableStateOf<ComparadorResultado?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    val taxaDiAutomatica = taxas.taxaCdiAnualPercentual.takeIf { it > 0.0 }
        ?: TAXA_DI_ANUAL_FALLBACK_PERCENTUAL

    val erroValorAporte = stringResource(R.string.erro_valor_aporte)
    val erroPercentual = stringResource(R.string.erro_percentual_cdi)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.comparar_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = valorInicialTexto,
            onValueChange = { valorInicialTexto = it },
            label = { Text(stringResource(R.string.campo_valor_inicial)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = aporteMensalTexto,
            onValueChange = { aporteMensalTexto = it },
            label = { Text(stringResource(R.string.campo_aporte_mensal)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = mesesTexto,
            onValueChange = { mesesTexto = it },
            label = { Text(stringResource(R.string.campo_meses)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.campo_taxa_di) + ": " + taxaDiAutomatica.formatarTaxaUi(),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = taxaOrigemLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = percentualCdbTexto,
            onValueChange = { percentualCdbTexto = it },
            label = { Text(stringResource(R.string.campo_percentual_cdb)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = percentualLciTexto,
            onValueChange = { percentualLciTexto = it },
            label = { Text(stringResource(R.string.campo_percentual_lci)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val valorInicial = valorInicialTexto.parseDecimalBr() ?: 0.0
                val aporteMensal = aporteMensalTexto.parseDecimalBr() ?: 0.0
                val meses = mesesTexto.trim().toIntOrNull()?.coerceAtLeast(1) ?: 12
                val pctCdb = percentualCdbTexto.parseDecimalBr() ?: 0.0
                val pctLci = percentualLciTexto.parseDecimalBr() ?: 0.0
                aviso = when {
                    valorInicial <= 0.0 && aporteMensal <= 0.0 -> erroValorAporte
                    pctCdb <= 0.0 || pctCdb > 1000.0 -> erroPercentual
                    pctLci <= 0.0 || pctLci > 1000.0 -> erroPercentual
                    else -> {
                        val calculado = ComparadorCalculator.comparar(
                            valorInicial = MonetaryValue.ofReais(valorInicial),
                            aporteMensal = MonetaryValue.ofReais(aporteMensal),
                            percentualCdb = pctCdb,
                            percentualLci = pctLci,
                            taxaDiAnualPercentual = taxaDiAutomatica,
                            meses = meses,
                        )
                        resultado = calculado
                        null
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.simular))
        }

        aviso?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        resultado?.let { ResultadoComparacao(it, percentualLciTexto.parseDecimalBr() ?: 90.0) }
    }
}

@Composable
private fun ResultadoComparacao(resultado: ComparadorResultado, percentualLci: Double) {
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.comparador_resultado_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(8.dp))

    val cdbReais = resultado.cdbLiquido.cents / 100.0
    val lciReais = resultado.lciLiquido.cents / 100.0
    val vencedorCdb = cdbReais > lciReais
    val vencedorLci = lciReais > cdbReais

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.comparador_cdb_liquido),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = formatarMoeda(cdbReais),
                    style = if (vencedorCdb) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (vencedorCdb) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Card(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.comparador_lci_liquido),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = formatarMoeda(lciReais),
                    style = if (vencedorLci) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                    color = if (vencedorLci) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }

    val diferenca = abs(lciReais - cdbReais)
    Spacer(Modifier.height(8.dp))
    Text(
        text = when {
            vencedorLci -> stringResource(
                R.string.comparador_vencedor_lci,
                formatarMoeda(diferenca),
            )
            vencedorCdb -> stringResource(
                R.string.comparador_vencedor_cdb,
                formatarMoeda(diferenca),
            )
            else -> stringResource(R.string.comparador_empate, formatarMoeda(diferenca))
        },
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(
            R.string.comparador_equilibrio,
            percentualLci.formatarTaxaUi(),
            resultado.percentualCdbEquivalente.formatarTaxaUi(),
        ),
        style = MaterialTheme.typography.bodySmall,
    )
}