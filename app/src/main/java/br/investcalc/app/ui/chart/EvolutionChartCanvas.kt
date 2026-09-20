package br.investcalc.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
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
import br.investcalc.app.core.format.CurrencyFormat
import br.investcalc.app.domain.chart.EvolutionChartPoint
import br.investcalc.app.domain.chart.EvolutionChartSeries
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val formatoMes: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM/yyyy", Locale.ROOT)

/**
 * Formata reais seguindo o locale atual com prefixo fixo "R$ " (T0/P4):
 * pt-BR "R$ 1.234,56", en "R$ 1,234.56".
 */
internal fun formatarMoeda(v: Double): String =
    CurrencyFormat.formatReais(v)

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
 *  - toque/arraste **HORIZONTAL** seleciona o mês mais próximo e abre um
 *    tooltip com mês, saldo, rendimento do mês e juros acumulados — com
 *    **clamp** nas bordas (X e Y) para nunca sair do canvas;
 *  - T2/P7: o tooltip responde SOMENTE a arraste horizontal — arraste
 *    vertical sobre o gráfico NUNCA é consumido, então a página rola;
 *  - T2/P7: insets interno de eixo (rótulo Y mais largo + altura do rótulo
 *    X) para que TODOS os labels fiquem 100% dentro do canvas, sem clip.
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
    val corRotuloEixo = MaterialTheme.colorScheme.outline
    val estiloTitulo = MaterialTheme.typography.titleSmall.copy(color = corTexto)
    val estiloCorpo = MaterialTheme.typography.bodySmall.copy(color = corTexto)
    val estiloRotulo = MaterialTheme.typography.bodySmall.copy(color = corRotuloEixo)

    val textMeasurer = rememberTextMeasurer()
    var selecionado by remember { mutableStateOf<Int?>(null) }
    var tooltipUsado by remember { mutableStateOf(false) }

    // Largura do maior rótulo do eixo Y ("R$ 3.688,31") — usada tanto no
    // desenho quanto no mapeamento toque→mês (mesma origem, zero duplicação).
    val larguraLabelY = remember(pontos) {
        val topoV = pontos.maxOf { it.saldoCompostoFimDoMes }.coerceAtLeast(0.0)
        val inicial = pontos.first().totalInvestidoFimDoMes
        max(
            textMeasurer.measure(AnnotatedString(formatarMoeda(inicial)), estiloRotulo).size.width,
            textMeasurer.measure(AnnotatedString(formatarMoeda(topoV)), estiloRotulo).size.width,
        ).toFloat()
    }

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
                val pad = 4.dp.toPx()
                val espaco = 4.dp.toPx()
                val esquerda = pad + larguraLabelY + espaco
                val direita = size.width - pad
                val limiarSlop = viewConfiguration.touchSlop.toFloat()

                fun indiceDoToque(posicao: Offset): Int {
                    if (pontos.isEmpty()) return 0
                    val n = pontos.size
                    val areaLargura = (direita - esquerda).coerceAtLeast(1f)
                    val ux = areaLargura / max(n - 1, 1).toFloat()
                    val m = ((posicao.x - esquerda) / ux).roundToInt()
                    // Clamp nas bordas: nunca sai do intervalo [0, n-1].
                    return m.coerceIn(0, n - 1)
                }

                // T2/P7: o tooltip responde SÓ ao arraste horizontal; arraste
                // vertical sobre o gráfico NÃO é consumido → a página rola.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var decidido = false
                    var horizontal = false
                    drag(down.id) { change ->
                        val dx = change.position.x - down.position.x
                        val dy = change.position.y - down.position.y
                        if (!decidido && (dx * dx + dy * dy) >= limiarSlop * limiarSlop) {
                            decidido = true
                            horizontal = abs(dx) >= abs(dy)
                            if (horizontal) {
                                selecionado = indiceDoToque(change.position)
                                change.consume()
                            }
                        } else if (horizontal) {
                            selecionado = indiceDoToque(change.position)
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val n = pontos.size
        if (n == 0) return@Canvas
        val largura = size.width
        val altura = size.height
        if (largura <= 0f || altura <= 0f) return@Canvas

        val topo = pontos.maxOf { it.saldoCompostoFimDoMes }.coerceAtLeast(0.0)
        if (topo <= 0.0) return@Canvas

        fun medirRotulo(texto: String): TextLayoutResult =
            textMeasurer.measure(AnnotatedString(texto), estiloRotulo)

        val rotuloMesIni = medirRotulo(formatoMes.format(pontos.first().mes))
        val rotuloMesFim = medirRotulo(formatoMes.format(pontos.last().mes))
        val rotuloValIni = medirRotulo(formatarMoeda(pontos.first().totalInvestidoFimDoMes))
        val rotuloValFim = medirRotulo(formatarMoeda(topo))

        // Insets internos (T2/P7): rótulos de eixo INSIDE do canvas — o
        // gráfico ocupa o retângulo restante, sem nada clipped.
        val pad = 4.dp.toPx()
        val espaco = 4.dp.toPx()
        val alturaLabelX = rotuloMesIni.size.height.toFloat()
        val minAlturaY = minOf(rotuloValIni.size.height, rotuloValFim.size.height)

        val esquerda = pad + larguraLabelY + espaco
        val direita = largura - pad
        val topoArea = pad + max(rotuloValIni.size.height, rotuloValFim.size.height) + espaco
        val baseArea = altura - pad - alturaLabelX - espaco - minAlturaY

        val areaLargura = (direita - esquerda).coerceAtLeast(1f)
        val areaAltura = (baseArea - topoArea).coerceAtLeast(1f)
        val ux = areaLargura / max(n - 1, 1).toFloat()
        val uy = areaAltura / topo

        fun x(m: Int): Float = esquerda + m * ux
        fun y(v: Double): Float = baseArea - (v * uy).toFloat()

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

        // 5. Rótulos discretos dos eixos (mês inicial/final no X; valor
        //    inicial/final no Y), todos 100% dentro do canvas (T2/P7).
        drawText(
            rotuloMesIni,
            topLeft = Offset(esquerda, altura - pad - alturaLabelX),
        )
        drawText(
            rotuloMesFim,
            topLeft = Offset(largura - pad - rotuloMesFim.size.width, altura - pad - alturaLabelX),
        )
        drawText(
            rotuloValFim,
            topLeft = Offset(pad, pad),
        )
        drawText(
            rotuloValIni,
            topLeft = Offset(pad, baseArea + espaco),
        )

        // 6. Tooltip no mês selecionado, com clamp dentro da área do gráfico.
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

            var esquerdaBox = px - boxLargura / 2f
            esquerdaBox = esquerdaBox.coerceIn(esquerda, (direita - boxLargura).coerceAtLeast(esquerda))
            var topoBox = py - 8.dp.toPx() - boxAltura
            if (topoBox < topoArea) topoBox = py + 8.dp.toPx()
            topoBox = topoBox.coerceIn(topoArea, (baseArea - boxAltura).coerceAtLeast(topoArea))

            drawRoundRect(
                color = corCartao,
                topLeft = Offset(esquerdaBox, topoBox),
                size = Size(boxLargura, boxAltura),
                cornerRadius = CornerRadius(6.dp.toPx()),
            )
            drawRoundRect(
                color = corBordaCartao,
                topLeft = Offset(esquerdaBox, topoBox),
                size = Size(boxLargura, boxAltura),
                cornerRadius = CornerRadius(6.dp.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
            var linhaTopo = topoBox + 6.dp.toPx()
            layouts.forEach { layout ->
                drawText(layout, topLeft = Offset(esquerdaBox + 8.dp.toPx(), linhaTopo))
                linhaTopo += layout.size.height
            }
        }
    }
}