package br.investcalc.app.ui.chart

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.investcalc.app.domain.chart.EvolutionChartSeries
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoMes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT)

private fun formatarValor(v: Double): String =
    String.format(Locale.ROOT, "%,.2f", v)

/**
 * Compose screen for the patrimony evolution chart (compound vs simple
 * baseline). Hermetic UI slice: receives an already-built
 * [EvolutionChartSeries] and renders the summary card plus the month
 * list. ASCII only, no accents, never throws.
 */
@Composable
fun EvolutionChartScreen(
    series: EvolutionChartSeries?,
    onTooltipUsed: () -> Unit = {},
    onTableOpened: () -> Unit = {},
) {
    val s = series ?: run {
        Text(
            text = "Simule primeiro para ver a evolucao do patrimonio.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
        )
        return@EvolutionChartScreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Patrimonio mensal (bruto, IR no saldo final)",
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = "Total investido: ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "R$ " + formatarValor(s.totalInvestidoFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Saldo composto: ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "R$ " + formatarValor(s.saldoCompostoFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Juros acumulados: ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "R$ " + formatarValor(s.jurosAcumuladosFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Bonus juros sobre juros: ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "R$ " + formatarValor(s.bonusJurosSobreJurosFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Mes a mes (toque para abrir a tabela)",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        ) {
            items(
                items = s.pontos,
                key = { it.mes },
            ) { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTableOpened() }
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        text = formatoMes.format(p.mes),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(72.dp),
                    )
                    Text(
                        text = "R$ " + formatarValor(p.saldoCompostoFimDoMes),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(108.dp),
                    )
                    Text(
                        text = "R$ " + formatarValor(p.jurosAcumuladosFimDoMes),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(108.dp),
                    )
                    Text(
                        text = "R$ " + formatarValor(p.diferencaJurosSobreJurosFimDoMes),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(108.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bonus advertido: "
                + "R$ " + formatarValor(s.bonusJurosSobreJurosFinal)
                + " vieram dos juros sobre juros.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
