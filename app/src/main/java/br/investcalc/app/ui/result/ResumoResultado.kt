package br.investcalc.app.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.domain.model.InvestmentResult
import java.util.Locale

/**
 * Card de resumo numérico do resultado (com IR), compartilhado pelas abas
 * Calculadora CDI/CDB e Tesouro Direto. Hermético (recursos i18n + JVM puro).
 */
@Composable
fun ResumoResultado(resultado: InvestmentResult) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.resultado_title),
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        LinhaResumo(
            rotulo = stringResource(R.string.resultado_valor_bruto),
            centavos = resultado.valorBruto.cents,
        )
        LinhaResumo(
            rotulo = stringResource(R.string.resultado_total_investido),
            centavos = resultado.valorInvestido.cents,
        )
        LinhaResumo(
            rotulo = stringResource(R.string.resultado_rendimento_bruto),
            centavos = resultado.rendimentoBruto.cents,
        )
        LinhaResumo(
            rotulo = stringResource(
                R.string.resultado_ir,
                "%.1f".format(Locale.ROOT, resultado.aliquotaIr * 100.0) + "%",
            ),
            centavos = resultado.irPago.cents,
        )
        LinhaResumo(
            rotulo = stringResource(R.string.resultado_valor_liquido),
            centavos = resultado.valorLiquido.cents,
            destaque = true,
        )
    }
}

@Composable
private fun LinhaResumo(rotulo: String, centavos: Long, destaque: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = rotulo + ": ",
            modifier = Modifier.weight(1f),
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "R$ " + formatarCentavos(centavos),
            style = if (destaque) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (destaque) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun formatarCentavos(centavos: Long): String =
    String.format(Locale.ROOT, "%,.2f", centavos / 100.0)