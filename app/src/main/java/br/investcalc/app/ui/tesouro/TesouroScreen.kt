package br.investcalc.app.ui.tesouro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.data.rates.RatesFallback
import br.investcalc.app.data.rates.RatesSnapshot
import br.investcalc.app.domain.calculator.RateConverter
import br.investcalc.app.domain.calculator.TesouroIpcaPlusCalculator
import br.investcalc.app.domain.calculator.TesouroPrefixadoCalculator
import br.investcalc.app.domain.calculator.TesouroSelicCalculator
import br.investcalc.app.domain.chart.EvolutionChartBuilder
import br.investcalc.app.domain.chart.PatrimonioSeries
import br.investcalc.app.domain.chart.PatrimonioSeriesBuilder
import br.investcalc.app.domain.model.InvestmentResult
import br.investcalc.app.domain.model.MonetaryValue
import br.investcalc.app.ui.chart.EvolutionChartScreen
import br.investcalc.app.ui.input.CampoMonetario
import br.investcalc.app.ui.input.formatarTaxaUi
import br.investcalc.app.ui.input.parseDecimalBr
import br.investcalc.app.core.format.CurrencyInputMask
import br.investcalc.app.ui.result.ResumoResultado

private enum class TesouroModo { SELIC, IPCA_PLUS, PREFIXADO }

/**
 * Aba Tesouro Direto — 3 modos (Selic | IPCA+ | Prefixado) em segmented
 * buttons, reaproveitando o motor composto e o IR regressivo da camada de
 * domínio ([TesouroSelicCalculator], [TesouroIpcaPlusCalculator] e
 * [TesouroPrefixadoCalculator], todos pré-existentes).
 *
 * - Selic: taxa auto via BCB série 1178 (cache → API → fallback 10,90),
 *   editável, com badge de origem (mesmo snapshot da camada de taxas);
 * - IPCA+: IPCA auto via série 13522 (fallback 4,50) + taxa real editável
 *   (hint "ex.: 6,20"); anual efetiva = (1+IPCA)(1+real) − 1;
 * - Prefixado: taxa contratada editável;
 * - conversão anual → mensal por equivalência exponencial;
 * - nota informativa fixa da B3 (exibição apenas, sem cálculo).
 *
 * Resultado idêntico à aba CDI/CDB: resumo com IR + gráfico + tabela.
 */
@Composable
fun TesouroScreen(
    taxas: RatesSnapshot,
    taxaOrigemLabel: String,
) {
    var modo by remember { mutableStateOf(TesouroModo.SELIC) }
    var valorInicialTexto by remember { mutableStateOf("100000") }
    var aporteMensalTexto by remember { mutableStateOf("20000") }
    var mesesTexto by remember { mutableStateOf("12") }

    val selicAutomatica = taxas.taxaSelicAnualPercentual.takeIf { it > 0.0 }
        ?: RatesFallback.TAXA_SELIC_ANUAL_PERCENTUAL
    val ipcaAutomatica = taxas.variacaoIpca12MesesPercentual.takeIf { it > 0.0 }
        ?: RatesFallback.IPCA_12_MESES_PERCENTUAL

    var selicTexto by remember { mutableStateOf(selicAutomatica.formatarTaxaUi()) }
    var selicEditada by remember { mutableStateOf(false) }
    var ipcaTexto by remember { mutableStateOf(ipcaAutomatica.formatarTaxaUi()) }
    var ipcaEditada by remember { mutableStateOf(false) }
    var taxaRealTexto by remember { mutableStateOf("") }
    var prefixadoTexto by remember { mutableStateOf("") }

    var serie by remember { mutableStateOf<PatrimonioSeries?>(null) }
    var resultado by remember { mutableStateOf<InvestmentResult?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }

    // Auto-preenchimento como o DI da aba CDI/CDB: só quando o usuário não editou.
    LaunchedEffect(selicAutomatica) {
        if (!selicEditada) selicTexto = selicAutomatica.formatarTaxaUi()
    }
    LaunchedEffect(ipcaAutomatica) {
        if (!ipcaEditada) ipcaTexto = ipcaAutomatica.formatarTaxaUi()
    }

    val chart = remember(serie) {
        serie?.let { EvolutionChartBuilder.dePatrimonio(it) }
    }

    if (serie == null) {
        val erroValorAporte = stringResource(R.string.erro_valor_aporte)
        val erroSelic = stringResource(R.string.erro_taxa_selic)
        val erroReal = stringResource(R.string.erro_taxa_real)
        val erroPrefixada = stringResource(R.string.erro_taxa_prefixada)
        Formulario(
            modo = modo,
            onModoMudou = { modo = it },
            valorInicialTexto = valorInicialTexto,
            onValorInicialMudou = { valorInicialTexto = it },
            aporteMensalTexto = aporteMensalTexto,
            onAporteMensalMudou = { aporteMensalTexto = it },
            mesesTexto = mesesTexto,
            onMesesMudou = { mesesTexto = it },
            selicTexto = selicTexto,
            onSelicMudou = { selicTexto = it; selicEditada = true },
            ipcaTexto = ipcaTexto,
            onIpcaMudou = { ipcaTexto = it; ipcaEditada = true },
            taxaRealTexto = taxaRealTexto,
            onTaxaRealMudou = { taxaRealTexto = it },
            prefixadoTexto = prefixadoTexto,
            onPrefixadoMudou = { prefixadoTexto = it },
            taxaOrigemLabel = taxaOrigemLabel,
            aviso = aviso,
            onSimular = {
                aviso = null
                aviso = simular(
                modo = modo,
                valorInicialTexto = valorInicialTexto,
                aporteMensalTexto = aporteMensalTexto,
                mesesTexto = mesesTexto,
                selicTexto = selicTexto,
                ipcaTexto = ipcaTexto,
                selicAutomatica = selicAutomatica,
                ipcaAutomatica = ipcaAutomatica,
                taxaRealTexto = taxaRealTexto,
                prefixadoTexto = prefixadoTexto,
                erroValorAporte = erroValorAporte,
                erroSelic = erroSelic,
                erroReal = erroReal,
                erroPrefixada = erroPrefixada,
                onSerie = { serie = it },
                onResultado = { resultado = it },
            )
        },
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TextButton(onClick = { serie = null; resultado = null; aviso = null }) {
                Text(stringResource(R.string.voltar))
            }
            resultado?.let { ResumoResultado(it) }
            EvolutionChartScreen(
                series = chart,
                onTooltipUsed = {},
                onTableOpened = {},
            )
        }
    }
}

@Composable
private fun Formulario(
    modo: TesouroModo,
    onModoMudou: (TesouroModo) -> Unit,
    valorInicialTexto: String,
    onValorInicialMudou: (String) -> Unit,
    aporteMensalTexto: String,
    onAporteMensalMudou: (String) -> Unit,
    mesesTexto: String,
    onMesesMudou: (String) -> Unit,
    selicTexto: String,
    onSelicMudou: (String) -> Unit,
    ipcaTexto: String,
    onIpcaMudou: (String) -> Unit,
    taxaRealTexto: String,
    onTaxaRealMudou: (String) -> Unit,
    prefixadoTexto: String,
    onPrefixadoMudou: (String) -> Unit,
    taxaOrigemLabel: String,
    aviso: String?,
    onSimular: () -> Unit,
) {
    val rotulos = arrayOf(
        stringResource(R.string.tesouro_aba_selic),
        stringResource(R.string.tesouro_aba_ipca),
        stringResource(R.string.tesouro_aba_prefixado),
    )
    val modos = TesouroModo.entries

    val taxaSelicFracao = (selicTexto.parseDecimalBr() ?: 0.0) / 100.0
    val ipca = (ipcaTexto.parseDecimalBr() ?: 0.0)
    val taxaReal = taxaRealTexto.parseDecimalBr() ?: 0.0
    val prefixadaFracao = (prefixadoTexto.parseDecimalBr() ?: 0.0) / 100.0

    val mensalPercentual = when (modo) {
        TesouroModo.SELIC -> RateConverter.anualParaMensal(taxaSelicFracao) * 100.0
        TesouroModo.IPCA_PLUS ->
            RateConverter.anualParaMensal(
                TesouroIpcaPlusCalculator.taxaAnualComposta(ipca / 100.0, taxaReal / 100.0),
            ) * 100.0
        TesouroModo.PREFIXADO -> RateConverter.anualParaMensal(prefixadaFracao) * 100.0
    }
    val linhaTaxaMensal = if (mensalPercentual.isFinite() && mensalPercentual > 0.0) {
        stringResource(
            R.string.taxa_mensal_efetiva,
            mensalPercentual.formatarTaxaUi(),
            (mensalPercentual / 100.0).mensalParaAnualUi().formatarTaxaUi(),
        )
    } else {
        stringResource(R.string.taxa_mensal_efetiva_vazia)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.tesouro_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            modos.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = modo == m,
                    onClick = { onModoMudou(m) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modos.size),
                ) {
                    Text(rotulos[index])
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        CampoMonetario(
            rotulo = stringResource(R.string.campo_valor_inicial),
            valorInicial = valorInicialTexto,
            onValorMudou = onValorInicialMudou,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(8.dp))

        CampoMonetario(
            rotulo = stringResource(R.string.campo_aporte_mensal),
            valorInicial = aporteMensalTexto,
            onValorMudou = onAporteMensalMudou,
            imeAction = ImeAction.Next,
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = mesesTexto,
            onValueChange = onMesesMudou,
            label = { Text(stringResource(R.string.campo_meses)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        when (modo) {
            TesouroModo.SELIC -> {
                OutlinedTextField(
                    value = selicTexto,
                    onValueChange = onSelicMudou,
                    label = { Text(stringResource(R.string.campo_taxa_selic)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = taxaOrigemLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            TesouroModo.IPCA_PLUS -> {
                OutlinedTextField(
                    value = ipcaTexto,
                    onValueChange = onIpcaMudou,
                    label = { Text(stringResource(R.string.campo_ipca)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = taxaOrigemLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = taxaRealTexto,
                    onValueChange = onTaxaRealMudou,
                    label = { Text(stringResource(R.string.campo_taxa_real)) },
                    placeholder = { Text(stringResource(R.string.hint_taxa_real)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TesouroModo.PREFIXADO -> {
                OutlinedTextField(
                    value = prefixadoTexto,
                    onValueChange = onPrefixadoMudou,
                    label = { Text(stringResource(R.string.campo_taxa_prefixada)) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        Text(
            text = linhaTaxaMensal,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.tesouro_nota_b3),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSimular, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.simular))
            }
        }

        aviso?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Executa a simulação; devolve a mensagem de erro localizada (ou null). */
private fun simular(
    modo: TesouroModo,
    valorInicialTexto: String,
    aporteMensalTexto: String,
    mesesTexto: String,
    selicTexto: String,
    ipcaTexto: String,
    selicAutomatica: Double,
    ipcaAutomatica: Double,
    taxaRealTexto: String,
    prefixadoTexto: String,
    erroValorAporte: String,
    erroSelic: String,
    erroReal: String,
    erroPrefixada: String,
    onSerie: (PatrimonioSeries) -> Unit,
    onResultado: (InvestmentResult) -> Unit,
): String? {
    val valorInicial = CurrencyInputMask.toReais(valorInicialTexto)
    val aporteMensal = CurrencyInputMask.toReais(aporteMensalTexto)
    val meses = mesesTexto.trim().toIntOrNull()?.coerceAtLeast(1) ?: 12

    if (valorInicial <= 0.0 && aporteMensal <= 0.0) return erroValorAporte

    // Taxa anual efetiva (fração) do modo + motor de cálculo correspondente.
    val (resultado, taxaAnualFracao) = when (modo) {
        TesouroModo.SELIC -> {
            val selic = selicTexto.parseDecimalBr() ?: 0.0
            if (selic <= 0.0) return erroSelic
            val fracao = selic / 100.0
            TesouroSelicCalculator.calcular(
                MonetaryValue.ofReais(valorInicial),
                MonetaryValue.ofReais(aporteMensal),
                fracao,
                meses,
            ) to fracao
        }
        TesouroModo.IPCA_PLUS -> {
            val ipca = ipcaTexto.parseDecimalBr()?.takeIf { it >= 0.0 } ?: ipcaAutomatica
            val real = taxaRealTexto.parseDecimalBr() ?: 0.0
            if (real <= 0.0) return erroReal
            val fracao = TesouroIpcaPlusCalculator.taxaAnualComposta(ipca / 100.0, real / 100.0)
            TesouroIpcaPlusCalculator.calcular(
                MonetaryValue.ofReais(valorInicial),
                MonetaryValue.ofReais(aporteMensal),
                ipca / 100.0,
                real / 100.0,
                meses,
            ) to fracao
        }
        TesouroModo.PREFIXADO -> {
            val prefixada = prefixadoTexto.parseDecimalBr() ?: 0.0
            if (prefixada <= 0.0) return erroPrefixada
            val fracao = prefixada / 100.0
            TesouroPrefixadoCalculator.calcular(
                MonetaryValue.ofReais(valorInicial),
                MonetaryValue.ofReais(aporteMensal),
                fracao,
                meses,
            ) to fracao
        }
    }

    val mensalPercentual = RateConverter.anualParaMensal(taxaAnualFracao) * 100.0
    val serie = PatrimonioSeriesBuilder.construir(
        valorInicial = valorInicial,
        aporteMensal = aporteMensal,
        meses = meses,
        taxaMensalPercentual = mensalPercentual,
    )

    onSerie(serie)
    onResultado(resultado)
    return null
}

private fun Double.mensalParaAnualUi(): Double =
    (Math.pow(1.0 + this, 12.0) - 1.0) * 100.0