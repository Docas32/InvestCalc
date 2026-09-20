package br.investcalc.app.ui.input

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.domain.calculator.PrazoConverter
import br.investcalc.app.domain.calculator.UnidadePrazo

/**
 * Campo de prazo com dropdown de unidade (Dias | Meses | Anos), default
 * **Meses** (T6/P7), compartilhado pelos 3 formulários (CDI/CDB, Tesouro e
 * Comparar). O motor continua em meses — a conversão acontece
 * em [PrazoConverter].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoPrazo(
    prazoTexto: String,
    onPrazoMudou: (String) -> Unit,
    unidade: UnidadePrazo,
    onUnidadeMudou: (UnidadePrazo) -> Unit,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = prazoTexto,
            onValueChange = onPrazoMudou,
            label = { Text(stringResource(R.string.campo_prazo)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = imeAction,
            ),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expandido,
            onExpandedChange = { expandido = it },
        ) {
            OutlinedTextField(
                value = rotuloUnidade(unidade),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false },
            ) {
                UnidadePrazo.entries.forEach { u ->
                    DropdownMenuItem(
                        text = { Text(rotuloUnidade(u)) },
                        onClick = {
                            onUnidadeMudou(u)
                            expandido = false
                        },
                    )
                }
            }
        }
    }
}

/** Rótulo i18n de cada unidade (Dias/Meses/Anos · Days/Months/Years). */
@Composable
internal fun rotuloUnidade(unidade: UnidadePrazo): String = stringResource(
    when (unidade) {
        UnidadePrazo.DIAS -> R.string.unidade_dias
        UnidadePrazo.MESES -> R.string.unidade_meses
        UnidadePrazo.ANOS -> R.string.unidade_anos
    },
)

/** Mensagem de validação do prazo por unidade (faixas do T6). */
@Composable
internal fun erroPrazo(unidade: UnidadePrazo): String = stringResource(
    when (unidade) {
        UnidadePrazo.DIAS -> R.string.erro_prazo_dias
        UnidadePrazo.MESES -> R.string.erro_prazo_meses
        UnidadePrazo.ANOS -> R.string.erro_prazo_anos
    },
)