package br.investcalc.app.ui.guia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
 * Tela Guia — conteúdo educativo (P5) em 8 tópicos expansíveis.
 *
 * Desde P8 o Guia não é mais aba da bottom nav: é uma tela EMPILHADA aberta de
 * dentro de Ajustes. Por isso recebe [onVoltar] (seta + "Voltar" no topo) e
 * também responde ao system back (a rota "guia" fica no topo do back stack;
 * o pop-back devolve a Ajustes). O `screen_view` continua sendo o genérico do
 * AppNavHost (rota "guia") — sem evento próprio.
 *
 * Cada card mostra o título e, ao toque, expande o corpo (4–8 linhas) em
 * tom didático, com animação suave ([animateContentSize]). Zero novo
 * formatter: os exemplos monetários vêm traduzidos nas strings (pt-BR e EN)
 * e o tema é o [MaterialTheme] existente.
 */
@Composable
fun GuiaScreen(onVoltar: () -> Unit) {
    val topicos = listOf(
        TopicoGuia(
            titulo = stringResource(R.string.guia_t1_titulo),
            corpo = stringResource(R.string.guia_t1_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t2_titulo),
            corpo = stringResource(R.string.guia_t2_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t3_titulo),
            corpo = stringResource(R.string.guia_t3_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t4_titulo),
            corpo = stringResource(R.string.guia_t4_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t5_titulo),
            corpo = stringResource(R.string.guia_t5_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t6_titulo),
            corpo = stringResource(R.string.guia_t6_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t7_titulo),
            corpo = stringResource(R.string.guia_t7_corpo),
        ),
        TopicoGuia(
            titulo = stringResource(R.string.guia_t8_titulo),
            corpo = stringResource(R.string.guia_t8_corpo),
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onVoltar) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.voltar),
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.voltar))
            }
        }
        Text(
            text = stringResource(R.string.guia_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))
        topicos.forEach { topico ->
            TopicoCard(topico)
            Spacer(Modifier.height(8.dp))
        }
    }
}

private data class TopicoGuia(val titulo: String, val corpo: String)

@Composable
private fun TopicoCard(topico: TopicoGuia) {
    var expandido by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandido = !expandido }
                .padding(16.dp)
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = topico.titulo,
                    style = MaterialTheme.typography.titleSmall,
                )
                Icon(
                    imageVector = if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(visible = expandido) {
                Text(
                    text = topico.corpo,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}