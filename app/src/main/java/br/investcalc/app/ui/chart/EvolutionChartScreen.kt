package br.investcalc.app.ui.chart

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.domain.chart.EvolutionChartSeries
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoMes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT)

/**
 * Tela de resultado do InvestCalc: gráfico de evolução (2 linhas no mesmo
 * eixo + área de juros + tooltip) acima do card "Patrimônio mensal", seguido
 * da tabela mês a mês. Hermética (JVM pura + recursos i18n), nunca lança.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EvolutionChartScreen(
    series: EvolutionChartSeries?,
    onTooltipUsed: () -> Unit = {},
    onTableOpened: () -> Unit = {},
) {
    val s = series ?: run {
        Text(
            text = stringResource(R.string.chart_vazio),
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
        Text(
            text = stringResource(R.string.chart_titulo),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(8.dp))

        EvolutionChartCanvas(
            series = s,
            onTooltipUsed = onTooltipUsed,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Legenda das duas séries do mesmo eixo — FlowRow (T2/P7): o texto
        // longo de "Patrimônio (juros compostos)" nunca é cortado na borda.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ItemLegenda(
                cor = MaterialTheme.colorScheme.onSurfaceVariant,
                rotulo = stringResource(R.string.chart_total_investido),
            )
            ItemLegenda(
                cor = MaterialTheme.colorScheme.primary,
                rotulo = stringResource(R.string.chart_saldo_composto),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.chart_patrimonio_mensal),
                    style = MaterialTheme.typography.titleMedium,
                )
                Row(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = stringResource(R.string.chart_total_investido) + ": ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatarMoeda(s.totalInvestidoFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = stringResource(R.string.chart_saldo_composto_label) + ": ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatarMoeda(s.saldoCompostoFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = stringResource(R.string.chart_juros_acumulados_label) + ": ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatarMoeda(s.jurosAcumuladosFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = stringResource(R.string.chart_bonus_juros_label) + ": ",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = formatarMoeda(s.bonusJurosSobreJurosFinal),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabela mês a mês expandível: começa RECOLHIDA; toque no cabeçalho
        // expande/recolhe (cumpre o "toque para abrir a tabela" do label).
        // O bug P2 (header estático + onTableOpened no-op) foi corrigido aqui.
        var tabelaAberta by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    tabelaAberta = !tabelaAberta
                    if (tabelaAberta) onTableOpened()
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    if (tabelaAberta) R.string.chart_mes_a_mes_fechar else R.string.chart_mes_a_mes,
                ),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (tabelaAberta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        AnimatedVisibility(visible = tabelaAberta) {
            Column {
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
                                text = formatarMoeda(p.saldoCompostoFimDoMes),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(118.dp),
                            )
                            Text(
                                text = formatarMoeda(p.jurosAcumuladosFimDoMes),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(118.dp),
                            )
                            Text(
                                text = formatarMoeda(p.diferencaJurosSobreJurosFimDoMes),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(118.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(
                        R.string.chart_bonus_advertido,
                        formatarMoeda(s.bonusJurosSobreJurosFinal),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ItemLegenda(
    cor: androidx.compose.ui.graphics.Color,
    rotulo: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(width = 20.dp, height = 4.dp)
                .background(cor),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = rotulo,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}
