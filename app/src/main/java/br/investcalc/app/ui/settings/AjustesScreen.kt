package br.investcalc.app.ui.settings

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.BuildConfig
import br.investcalc.app.R
import br.investcalc.app.domain.plano.EstadoPlanoSemAds

private const val JANELA_TOQUES_MS = 700L
private const val TOQUES_PARA_CRASH = 5

/**
 * Ajustes (Material 3, T4/P7).
 *
 * - "Anúncios (consentimento)": reflete o estado atual do UMP; ao alternar,
 *   [onAlternarConsentimento] reabre o formulário de consentimento — quem
 *   decide é o próprio usuário no formulário (toggle é o gatilho). Some quando
 *   o plano sem ads foi comprado (mais o banner já é gated no nav host);
 * - "Remover anúncios" ([EstadoPlanoSemAds]): compra única não-consumível com
 *   preço do Play; card some em aparelho sem Play (INDISPONIVEL), vira status
 *   "comprado" quando [EstadoPlanoSemAds.COMPRADO];
 * - "Versão %1$s": **5 toques rápidos** (janela de [JANELA_TOQUES_MS]) no
 *   número da versão disparam o teste de crash do Crashlytics, visível APENAS
 *   em build DEBUG ([BuildConfig.DEBUG]) — some no release sem perder o
 *   caminho de teste (P5).
 */
@Composable
fun AjustesScreen(
    consentimentoAtivo: Boolean,
    onAlternarConsentimento: () -> Unit,
    estadoPlano: EstadoPlanoSemAds,
    precoPlano: String?,
    onComprarPlano: () -> Unit,
    onRestaurarCompras: () -> Unit,
    onForcarCrashTeste: () -> Unit,
) {
    var consentimentoLocal by remember { mutableStateOf(consentimentoAtivo) }
    var ultimoToqueVersao by remember { mutableLongStateOf(0L) }
    var toquesVersao by remember { mutableIntStateOf(0) }

    // Sincroniza com o estado real do UMP após o formulário ser respondido.
    LaunchedEffect(consentimentoAtivo) {
        consentimentoLocal = consentimentoAtivo
    }

    val planoComprado = estadoPlano == EstadoPlanoSemAds.COMPRADO

    val registrarToqueVersao: () -> Unit = {
        if (BuildConfig.DEBUG) {
            val agora = SystemClock.uptimeMillis()
            toquesVersao = if (agora - ultimoToqueVersao <= JANELA_TOQUES_MS) toquesVersao + 1 else 1
            ultimoToqueVersao = agora
            if (toquesVersao >= TOQUES_PARA_CRASH) {
                onForcarCrashTeste()
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = stringResource(R.string.ajustes_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (!planoComprado) {
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
        }

        if (planoComprado) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.plano_sem_ads_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.plano_sem_ads_comprado),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else if (estadoPlano == EstadoPlanoSemAds.NAO_COMPRADO) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.plano_sem_ads_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.plano_sem_ads_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    precoPlano?.let { preco ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.plano_sem_ads_preco, preco),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onComprarPlano) {
                            Text(stringResource(R.string.plano_sem_ads_comprar))
                        }
                        TextButton(onClick = onRestaurarCompras) {
                            Text(stringResource(R.string.plano_sem_ads_restaurar))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        LinhaTexto(
            rotulo = stringResource(R.string.ajustes_versao, BuildConfig.VERSION_NAME),
            onClick = registrarToqueVersao,
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

@Composable
private fun LinhaTexto(
    rotulo: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = rotulo, style = MaterialTheme.typography.bodyLarge)
    }
}