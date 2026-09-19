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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.BuildConfig
import br.investcalc.app.R

/**
 * Ajustes (Material 3).
 *
 * - "Anúncios (consentimento)": reflete o estado atual do UMP; ao alternar,
 *   [onAlternarConsentimento] reabre o formulário de consentimento — quem
 *   decide é o próprio usuário no formulário (toggle é o gatilho);
 * - "Forçar crash (teste)": dispara o teste do Crashlytics, visível APENAS
 *   em build DEBUG ([BuildConfig.DEBUG]) — some no release sem perder o
 *   caminho de teste do Crashlytics (P5).
 */
@Composable
fun AjustesScreen(
    consentimentoAtivo: Boolean,
    onAlternarConsentimento: () -> Unit,
    onForcarCrashTeste: () -> Unit,
) {
    var consentimentoLocal by remember { mutableStateOf(consentimentoAtivo) }

    // Sincroniza com o estado real do UMP após o formulário ser respondido.
    LaunchedEffect(consentimentoAtivo) {
        consentimentoLocal = consentimentoAtivo
    }

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
            onToggle = onAlternarConsentimento,
        )

        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.ajustes_testes_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinhaTexto(
                rotulo = stringResource(R.string.forcar_crash),
                descricao = stringResource(R.string.forcar_crash_desc),
                onClick = onForcarCrashTeste,
            )
        }
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

@Composable
private fun LinhaTexto(
    rotulo: String,
    descricao: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rotulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = descricao, style = MaterialTheme.typography.bodySmall)
        }
    }
}