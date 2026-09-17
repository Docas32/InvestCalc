package br.investcalc.app.domain.chart

import java.time.LocalDate

/**
 * Single point of the patrimony evolution line (compound curve).
 *
 * All values are produced for month `m` (months start at 1; LocalDate is the
 * first day of the month). Everything here is already net of the app's
 * income-tax model: the calculator domain emits pre-tax lines and the
 * Android screen applies IR (RendaVariavel/IncomeTax) on the final saldo.
 *
 * Hermetic JVM: no Android, no network, never throws.
 */
data class EvolutionChartPoint(
    /** First day of the month. */
    val mes: LocalDate,
    /** Invested principal so far (aportes acumulados, sem juros). */
    val totalInvestidoFimDoMes: Double,
    /** Patrimony with compound interest at end of month. */
    val saldoCompostoFimDoMes: Double,
    /** Juros (compostos) acumulados at end of month. */
    val jurosAcumuladosFimDoMes: Double,
    /** Reference line: juros SIMPLES at end of month (baseline). */
    val jurosSimplesBaselineFimDoMes: Double,
    /** Aporte (monthly contribution) credited this month. */
    val aporteNoMes: Double,
    /** Premium of compounding over the simple baseline, never negative. */
    val diferencaJurosSobreJurosFimDoMes: Double,
    /** Share 0..1 of saldo that came from juros. */
    val fracaoDeJurosNoSaldo: Double,
) {
    /** Inicial + aportes acumulados, no juros (alias for clarity in UI). */
    val totalInvestido: Double get() = totalInvestidoFimDoMes
    /** Compound saldo (alias for clarity in UI). */
    val saldoComposto: Double get() = saldoCompostoFimDoMes
}

/** Ordered series of chart points (one per month, ascending by mes). */
data class EvolutionChartSeries(
    val pontos: List<EvolutionChartPoint>,
) {
    val totalInvestidoFinal: Double get() = pontos.lastOrNull()?.totalInvestidoFimDoMes ?: 0.0
    val saldoCompostoFinal: Double get() = pontos.lastOrNull()?.saldoCompostoFimDoMes ?: 0.0
    val jurosAcumuladosFinal: Double get() = pontos.lastOrNull()?.jurosAcumuladosFimDoMes ?: 0.0
    /** Compounding premium over the simple baseline at the end. */
    val bonusJurosSobreJurosFinal: Double
        get() = pontos.lastOrNull()?.diferencaJurosSobreJurosFimDoMes ?: 0.0
    /** When the last point was written (deals been null until first month). */
    val atualizadaEm: LocalDate? get() = pontos.lastOrNull()?.mes
}

/**
 * Builds the chart series as the visible line: compound saldo on top, the
 * simple-interest baseline below it, and the juros-sobre-juros band betweem.
 *
 * Fully self-contained (takes plain lists, no other domain classes, never
 * throws): the UI can feed it month-local/gross values and the annualized
 * CDI; the tax curve stays a screen concern.
 */
object EvolutionChartBuilder {

    /**
     * @param meses Monthly anchors, ascending, one entry per month (month 1
     *   first). Caller guarantees 12 entries for an annual chart.
     * @param totalInvestidoFimDoMes Principal (aportes acumulados, no juros).
     * @param saldoCompostoFimDoMes Compound saldo at end of each month.
     * @param jurosSimplesBaselineFimDoMes Simple-interest baseline at end of
     *   each month (same size; missing entries default to 0).
     * @param aportesPorMes Contribution credited in each month (month m
     *   credit happens at end of month; 0 for "no aporte").
     */
    fun construir(
        meses: List<LocalDate>,
        totalInvestidoFimDoMes: List<Double>,
        saldoCompostoFimDoMes: List<Double>,
        jurosSimplesBaselineFimDoMes: List<Double>,
        aportesPorMes: List<Double>,
    ): EvolutionChartSeries {
        val n = minOf(
            meses.size,
            totalInvestidoFimDoMes.size,
            saldoCompostoFimDoMes.size,
            jurosSimplesBaselineFimDoMes.size,
            aportesPorMes.size,
        )
        if (n <= 0) return EvolutionChartSeries(pontos = emptyList())

        val pontos = ArrayList<EvolutionChartPoint>(n)
        for (m in 0 until n) {
            val total = totalInvestidoFimDoMes[m]
            val saldo = saldoCompostoFimDoMes[m]
            val juros = (saldo - total).coerceAtLeast(0.0)
            val baseline = if (m < jurosSimplesBaselineFimDoMes.size)
                jurosSimplesBaselineFimDoMes[m].coerceAtLeast(0.0) else 0.0
            val diferenca = (juros - baseline).coerceAtLeast(0.0)
            val fracao = if (saldo > 0.0) (juros / saldo).coerceIn(0.0, 1.0) else 0.0
            pontos += EvolutionChartPoint(
                mes = meses[m],
                totalInvestidoFimDoMes = total,
                saldoCompostoFimDoMes = saldo,
                jurosAcumuladosFimDoMes = juros,
                jurosSimplesBaselineFimDoMes = baseline,
                aporteNoMes = if (m < aportesPorMes.size) aportesPorMes[m] else 0.0,
                diferencaJurosSobreJurosFimDoMes = diferenca,
                fracaoDeJurosNoSaldo = fracao,
            )
        }
        return EvolutionChartSeries(pontos = pontos)
    }
}
