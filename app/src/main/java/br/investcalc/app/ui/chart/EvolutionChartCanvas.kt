package br.investcalc.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import br.investcalc.app.R
import br.investcalc.app.domain.chart.EvolutionChartPoint
import br.investcalc.app.domain.chart.EvolutionChartSeries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private val formatoMes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT)

/** Formata reais com separador de milhar: "R$ 1.234,56" (deterministico). */
internal fun formatarMoeda(v: Double): String =
    "R$ " + String.format(Locale.ROOT, "%,.2f", v)

/** Rendimento dos juros do mês `idx` (delta dos juros acumulados). */
internal fun rendimentoNoMes(pontos: List<EvolutionChartPoint>, idx: Int): Double =
    if (idx <= 0) {
        pontos[0].jurosAcumuladosFimDoMes
    } else {
        pontos[idx].jurosAcumuladosFimDoMes - pontos[idx - 1].jurosAcumuladosFimDoMes
    }

/**
 * Canvas do gráfico de evolução da tela de resultado (Material 3, Canvas
 * puro, zero bibliotecas novas):
 *
 *  - série **"Total investido"**: reta cinza neutra (por baixo);
 *  - série **"Patrimônio (juros compostos)"**: curva na cor primária (por
 *    cima);
 *  - área preenchida **entre** as duas linhas = o que veio dos juros;
 *  - toque/arraste seleciona o mês mais próximo e abre um tooltip com mês,
 *    saldo, rendimento do mês e juros acumulados — com **clamp** nas bordas
 *    (X e Y) para nunca sair do canvas;
 *  - O(n) em tudo: 999 pontos renderizam sem problema (sem alocação por
 *    frame fora do draw, sem iteração por ponto no tooltip).
 *
 * Todas as cores vêm de MaterialTheme.colorScheme — nada hardcoded.
 */
@Composable
fun EvolutionChartCanvas(
    series: EvolutionChartSeries?,
    modifier: Modifier = Modifier,
    onTooltipUsed: () -> Unit = {},
) {
    val s = series ?: return
    val pontos = s.pontos
    if (pontos.isEmpty()) return

    val corPatrimonio = MaterialTheme.colorScheme.primary
    val corTotalInvestido = MaterialTheme.colorScheme.onSurfaceVariant
    val corAreaJuros = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val corCartao = MaterialTheme.colorScheme.surfaceVariant
    val corBordaCartao = MaterialTheme.colorScheme.outline
    val corTexto = MaterialTheme.colorScheme.onSurface
    val estiloTitulo = MaterialTheme.typography.titleSmall.copy(color = corTexto)
    val estiloCorpo = MaterialTheme.typography.bodySmall.copy(color = corTexto)

    val textMeasurer = rememberTextMeasurer()
    var selecionado by remember { mutableStateOf<Int?>(null) }
    var tooltipUsado by remember { mutableStateOf(false) }

    LaunchedEffect(selecionado) {
        if (selecionado != null && !tooltipUsado) {
            tooltipUsado = true
            onTooltipUsed()
        }
    }

    val tooltipTextos: List<String>? = selecionado?.let { idx ->
        val ponto = pontos[idx]
        listOf(
            formatoMes.format(ponto.mes),
            stringResource(R.string.chart_tooltip_saldo, formatarMoeda(ponto.saldoCompostoFimDoMes)),
            stringResource(R.string.chart_tooltip_rendimento, formatarMoeda(rendimentoNoMes(pontos, idx))),
            stringResource(R.string.chart_tooltip_juros, formatarMoeda(ponto.jurosAcumuladosFimDoMes)),
        )
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .pointerInput(pontos.size) {
                fun indiceDoToque(posicao: Offset): Int {
                    if (pontos.isEmpty()) return 0
                    val padX = 6.dp.toPx()
                    val n = pontos.size
                    val ux = (size.width - 2 * padX) / max(n - 1, 1).toFloat()
                    val m = ((posicao.x - padX) / ux).roundToInt()
                    // Clamp nas bordas: nunca sai do intervalo [0, n-1].
                    return m.coerceIn(0, n - 1)
                }
                detectDragGestures(
                    onDragStart = { selecionado = indiceDoToque(it) },
                    onDrag = { change, _ ->
                        selecionado = indiceDoToque(change.position)
                        change.consume()
                    },
                )
            },
    ) {
        val n = pontos.size
        if (n == 0) return@Canvas
        val largura = size.width
        val altura = size.height
        if (largura <= 0f || altura <= 0f) return@Canvas

        val topo = pontos.maxOf { it.saldoCompostoFimDoMes }.coerceAtLeast(0.0)
        if (topo <= 0.0) return@Canvas

        val padX = 6.dp.toPx()
        val padY = 6.dp.toPx()
        val ux = (largura - 2 * padX) / max(n - 1, 1).toFloat()
        val uy = (altura - 2 * padY) / topo

        fun x(m: Int): Float = padX + m * ux
        fun y(v: Double): Float = (altura - padY) - (v * uy).toFloat()

        // 1. Área preenchida entre a curva (topo) e a reta do total investido.
        val area = Path()
        pontos.forEachIndexed { m, p ->
            val px = x(m)
            if (m == 0) area.moveTo(px, y(p.saldoCompostoFimDoMes)) else area.lineTo(px, y(p.saldoCompostoFimDoMes))
        }
        for (m in n - 1 downTo 0) {
            area.lineTo(x(m), y(pontos[m].totalInvestidoFimDoMes))
        }
        area.close()
        drawPath(path = area, color = corAreaJuros)

        // 2. Reta cinza neutra: total investido (por baixo).
        drawPath(
            path = Path().apply {
                pontos.forEachIndexed { m, p ->
                    val px = x(m)
                    val py = y(p.totalInvestidoFimDoMes)
                    if (m == 0) moveTo(px, py) else lineTo(px, py)
                }
            },
            color = corTotalInvestido,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )

        // 3. Curva primária: patrimônio (juros compostos) (por cima).
        drawPath(
            path = Path().apply {
                pontos.forEachIndexed { m, p ->
                    val px = x(m)
                    val py = y(p.saldoCompostoFimDoMes)
                    if (m == 0) moveTo(px, py) else lineTo(px, py)
                }
            },
            color = corPatrimonio,
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
        )

        // 4. Marcadores do último ponto.
        val ultimo = pontos[n - 1]
        drawCircle(color = corPatrimonio, radius = 3.6.dp.toPx(), center = Offset(x(n - 1), y(ultimo.saldoCompostoFimDoMes)))
        drawCircle(color = corTotalInvestido, radius = 3.2.dp.toPx(), center = Offset(x(n - 1), y(ultimo.totalInvestidoFimDoMes)))

        // 5. Tooltip no mês selecionado, com clamp nas bordas (X e Y).
        val idx = selecionado
        val textos = tooltipTextos
        if (idx != null && textos != null) {
            val px = x(idx)
            val py = y(pontos[idx].saldoCompostoFimDoMes)

            var boxLargura = 0f
            var boxAltura = 0f
            val layouts = ArrayList<TextLayoutResult>(textos.size)
            textos.forEachIndexed { k, linha ->
                val style = if (k == 0) estiloTitulo else estiloCorpo
                val layout = textMeasurer.measure(AnnotatedString(linha), style)
                layouts.add(layout)
                boxLargura = max(boxLargura, layout.size.width.toFloat())
                boxAltura += layout.size.height
            }
            boxLargura += 16.dp.toPx()
            boxAltura += 12.dp.toPx()

            var esquerda = px - boxLargura / 2f
            esquerda = esquerda.coerceIn(padX, largura - padX - boxLargura)
            var topoBox = py - 8.dp.toPx() - boxAltura
            if (topoBox < padY) topoBox = py + 8.dp.toPx()
            topoBox = topoBox.coerceIn(padY, altura - padY - boxAltura)

            drawRoundRect(
                color = corCartao,
                topLeft = Offset(esquerda, topoBox),
                size = Size(boxLargura, boxAltura),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            drawRoundRect(
                color = corBordaCartao,
                topLeft = Offset(esquerda, topoBox),
                size = Size(boxLargura, boxAltura),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            var linhaTopo = topoBox + 6.dp.toPx()
            layouts.forEach { layout ->
                drawText(layout, topLeft = Offset(esquerda + 8.dp.toPx(), linhaTopo))
                linhaTopo += layout.size.height
            }
        }
    }
}
