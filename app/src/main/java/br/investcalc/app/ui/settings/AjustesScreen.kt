package br.investcalc.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Ajustes hermetica (ASCII puro, M3, zero rede, zero Firebase, zero crash).
 * Todos os switches guardam estado local (remember) e jamais disparam rede.
 */
@Composable
fun AjustesScreen(
    consentimentoAdsAtivo: Boolean,
    notificacoesAtivas: Boolean,
    onAlternativeConsentimento: () -> Unit,
    onAlternarNotificacoes: () -> Unit,
) {
    var consentimentoLocal by remember { mutableStateOf(consentimentoAdsAtivo) }
    var notificacoesLocal by remember { mutableStateOf(notificacoesAtivas) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        LinhaToggle(
            rotulo = "Anuncios (consentimento)",
            descricao = if (consentimentoLocal) "Ativo" else "Inativo, sem ads",
            ligado = consentimentoLocal,
            onToggle = {
                consentimentoLocal = !consentimentoLocal
                onAlternativeConsentimento()
            },
        )
        HorizontalDivider()
        LinhaToggle(
            rotulo = "Notificacoes",
            descricao = if (notificacoesLocal) "Ligadas" else "Desligadas",
            ligado = notificacoesLocal,
            onToggle = {
                notificacoesLocal = !notificacoesLocal
                onAlternarNotificacoes()
            },
        )
    }
}

@Composable
private fun LinhaToggle(
    rotulo: String,
    descricao: String,
    ligado: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rotulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = descricao, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = ligado, onCheckedChange = { onToggle() })
    }
}
