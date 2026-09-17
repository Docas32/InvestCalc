package br.investcalc.app.ui.chart

import androidx.compose.foundation.Canvas
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import br.investcalc.app.domain.chart.EvolutionChartSeries
import kotlin.math.max
import kotlin.math.abs

private val CorComposto = Color(0xFF2E7D32)
private val CorBaseline = Color(0xFF9E9E9E)
private val CorBandaConcav = Color(0x33FFB300)

/**
 * Hermetic chart canvas: draws the compound patrimony line (green) over the
 * simple-interest baseline (grey) and fills the band between them -- the
 * visible juros-sobre-juros premium. Pure GraphicsLayer/Compose, no click,
 * no network, never throws, allocates nothing outside draw.
 */
@androidx.compose.runtime.Composable
fun EvolutionChartCanvas(
    series: EvolutionChartSeries?,
    modifier: Modifier = Modifier,
) {
    val s = series ?: return
    val pontos = s.pontos
    if (pontos.isEmpty()) return

    Canvas(modifier = modifier) {
        val n = pontos.size
        if (n == 0) return@Canvas
        val largura = size.width
        val altura = size.height
        if (largura <= 0f || altura <= 0f) return@Canvas

        val maxSaldo = pontos.maxOf { max(it.saldoCompostoFimDoMes, it.jurosSimplesBaselineFimDoMes) }
        val maxBaseline = pontos.maxOf { it.jurosSimplesBaselineFimDoMes }
        val topo = max(maxSaldo, maxBaseline)
        if (topo <= 0.0) return@Canvas

        val padX = 6.dp.toPx()
        val padY = 6.dp.toPx()
        val ux = (largura - 2 * padX) / max(n - 1, 1)
        val uy = (altura - 2 * padY) / topo

        fun x(m: Int): Float = padX + m * ux
        fun y(v: Double): Float = (altura - padY) - (v * uy).toFloat()

        // compound line (green)
        val composto = Path()
        pontos.forEachIndexed { m, p ->
            val px = x(m)
            val py = y(p.saldoCompostoFimDoMes)
            if (m == 0) composto.moveTo(px, py) else composto.lineTo(px, py)
        }

        // baseline line (grey, simple interest)
        val baseline = Path()
        pontos.forEachIndexed { m, p ->
            val px = x(m)
            val py = y(p.jurosSimplesBaselineFimDoMes)
            if (m == 0) baseline.moveTo(px, py) else baseline.lineTo(px, py)
        }

        drawPath(
            path = composto,
            color = CorComposto,
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
        drawPath(
            path = baseline,
            color = CorBaseline,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round),
        )

        // band between the two lines = juros sobre juros premium
        val banda = Path()
        pontos.forEachIndexed { m, p ->
            val px = x(m)
            if (m == 0) {
                banda.moveTo(px, y(p.saldoCompostoFimDoMes))
            } else {
                val ant = pontos[m - 1]
                // vertical stripe between previous segment endpoints
                banda.lineTo(px, y(p.saldoCompostoFimDoMes))
                val pxAnt = x(m - 1)
                banda.lineTo(pxAnt, y(ant.saldoCompostoFimDoMes))
            }
        }
        // close the top edge back to start for a filled premium band
        for (m in n - 1 downTo 0) {
            val px = x(m)
            banda.lineTo(px, y(pontos[m].jurosSimplesBaselineFimDoMes))
        }
        banda.close()
        drawPath(path = banda, color = CorBandaConcav)

        // last point marker
        val ultimo = pontos[n - 1]
        drawCircle(
            color = CorComposto,
            radius = 3.4.dp.toPx(),
            center = Offset(
                x(n - 1),
                y(ultimo.saldoCompostoFimDoMes),
            ),
        )
        drawCircle(
            color = CorBaseline,
            radius = 3.0.dp.toPx(),
            center = Offset(
                x(n - 1),
                y(ultimo.jurosSimplesBaselineFimDoMes),
            ),
        )
    }
}
