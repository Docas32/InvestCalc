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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import br.investcalc.app.BuildConfig
import br.investcalc.app.R
import br.investcalc.app.domain.plano.DesfechoCompra
import br.investcalc.app.domain.plano.EstadoPlanoSemAds
import br.investcalc.app.domain.plano.PlanoSemAds

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
 *   preço do Play. Com Billing indisponível ou produto não devolvido, o card
 *   aparece com "Comprar" visualmente desabilitado (cores do tema) e o
 *   subtítulo "disponível na loja"; o toque explica via snackbar (P7.3).
 *   "Restaurar compras" segue clicável e informa o resultado;
 * - "Guia educativo" (P8): reusa a tela existente do Guia, agora empilhada —
 *   o toque chama [onAbrirGuia] e o back (seta ou gesto) retorna a Ajustes;
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
    produtoDisponivel: Boolean,
    onComprarPlano: () -> Unit,
    onRestaurarCompras: () -> Unit,
    onAbrirGuia: () -> Unit,
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

    // P7.3: "Comprar" só abre o fluxo com Billing conectado E produto
    // devolvido. Indisponível => botão com cores de desabilitado (tema) que,
    // ao toque, explica no snackbar (via onComprarPlano — o Play nunca abre).
    val compraHabilitada = PlanoSemAds.tentarComprar(
        billingDisponivel = estadoPlano != EstadoPlanoSemAds.INDISPONIVEL,
        compradoPersistido = planoComprado,
        produtoEncontrado = produtoDisponivel,
    ) == DesfechoCompra.ABERTA

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
        } else if (estadoPlano == EstadoPlanoSemAds.NAO_COMPRADO ||
            estadoPlano == EstadoPlanoSemAds.INDISPONIVEL
        ) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    // Preço SOMENTE quando o Play o retorna; sem conexão (ou
                    // produto ausente) o subtítulo vira "disponível na loja".
                    val precoOuLoja = precoPlano?.let {
                        stringResource(R.string.plano_sem_ads_preco, it)
                    } ?: stringResource(R.string.plano_sem_ads_disponivel_loja)
                    Text(
                        text = precoOuLoja,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = onComprarPlano,
                            colors = if (compraHabilitada) {
                                ButtonDefaults.buttonColors()
                            } else {
                                // Visual de desabilitado do próprio Material 3
                                // (paleta do tema), MAS continuando clicável:
                                // o toque mostra a explicação no snackbar.
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                )
                            },
                        ) {
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
        Spacer(modifier = Modifier.height(8.dp))

        // P8: o Guia saiu da barra e virou item de Ajustes, acima de "Versão".
        LinhaIconeTexto(
            icone = Icons.Filled.MenuBook,
            rotulo = stringResource(R.string.guia_title),
            descricao = stringResource(R.string.ajustes_guia_item_desc),
            onClick = onAbrirGuia,
        )

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
private fun LinhaIconeTexto(
    icone: ImageVector,
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
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = rotulo, style = MaterialTheme.typography.bodyLarge)
            Text(text = descricao, style = MaterialTheme.typography.bodySmall)
        }
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