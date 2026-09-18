package br.investcalc.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.R

/**
 * Ajustes hermetica (Material 3, zero rede, zero Firebase, zero crash).
 * Unico toggle: "Anúncios (consentimento)" — sera ligado ao UMP na fase
 * AdMob. O toggle de Notificacoes foi removido (nao havia feature por tras).
 */
@Composable
fun AjustesScreen(
    consentimentoAdsAtivo: Boolean,
    onAlternativeConsentimento: () -> Unit,
) {
    var consentimentoLocal by remember { mutableStateOf(consentimentoAdsAtivo) }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = stringResource(R.string.ajustes_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        LinhaToggle(
            rotulo = stringResource(R.string.anuncios_consentimento),
            descricao = if (consentimentoLocal) {
                stringResource(R.string.anuncios_ativo)
            } else {
                stringResource(R.string.anuncios_inativo)
            },
            ligado = consentimentoLocal,
            onToggle = {
                consentimentoLocal = !consentimentoLocal
                onAlternativeConsentimento()
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
