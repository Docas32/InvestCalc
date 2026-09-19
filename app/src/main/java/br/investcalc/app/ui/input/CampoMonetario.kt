package br.investcalc.app.ui.input

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import br.investcalc.app.core.format.CurrencyInputMask

/**
 * Campo monetário com máscara "R$ 0,00" — **o único** componente de dinheiro
 * de todas as telas (CDI/CDB, Tesouro nos 3 modos e Comparar).
 *
 * P3.2-FALHA1: o estado é um [TextFieldValue] e o `onValueChange` re-emite
 * SEMPRE com `selection = TextRange(text.length)` — tocar no meio e digitar
 * ou apagar continua funcionando, mas o cursor volta ao fim a cada mudança.
 *
 * [valorInicial] só vale como seed da 1ª composição (aceita dígitos crus ou
 * texto já mascarado); depois disso o estado interno é a fonte da verdade e o
 * texto mascarado é reemitido via [onValorMudou].
 */
@Composable
fun CampoMonetario(
    rotulo: String,
    valorInicial: String,
    onValorMudou: (String) -> Unit,
    imeAction: ImeAction,
    placeholder: String? = null,
) {
    var estado by remember {
        val seed = CurrencyInputMask.apply(CurrencyInputMask.digitsOf(valorInicial))
        mutableStateOf(TextFieldValue(seed, TextRange(seed.length)))
    }

    OutlinedTextField(
        value = estado,
        onValueChange = { novo ->
            val mascara = CurrencyInputMask.reaplicar(novo.text)
            estado = TextFieldValue(mascara.texto, TextRange(mascara.posicaoCursor))
            onValorMudou(mascara.texto)
        },
        label = { Text(rotulo) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = imeAction,
        ),
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}