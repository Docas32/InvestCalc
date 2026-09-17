package br.investcalc.app.domain.chart

import java.time.LocalDate
import kotlin.math.max

/**
 * Ponto da linha de evolução do patrimônio no mês *m*.
 *
 * `inicioDoMes` = data do balanço (dia 1 do mês seguinte à data de início);
 * `aporteNoMes` = contribuição creditada no fim do mês;
 * `rendimentoNoMes` = juros do mês sobre o saldo do mês anterior;
 * `patrimonioLiquidoFimDoMes` = saldo após rendimento + aporte do mês.
 */
data class PatrimonioPoint(
    val inicioDoMes: LocalDate,
    val aporteNoMes: Double,
    val rendimentoNoMes: Double,
    val patrimonioLiquidoFimDoMes: Double,
) {
    val patrimonioFimDoMesVariavel: Double get() = patrimonioLiquidoFimDoMes
}

/**
 * Série do gráfico de evolução do patrimônio (linha mês a mês).
 *
 * **Contrato puro (JVM, sem Android, sem rede):** recebe o resultado mês a mês
 * já calculado pela camada de domínio e apenas agrupa em pontos ordenados
 * por [PatrimonioPoint.inicioDoMes]. Nunca lança: entrada vazia → série vazia.
 */
data class PatrimonioSeries(
    val pontos: List<PatrimonioPoint>,
) {
    init {
        require(pontos.map { it.inicioDoMes }.toSortedSet().toList() == pontos.map { it.inicioDoMes }) {
            "Série deve estar ordenada por mês (crescente)."
        }
    }

    val primeiroMes: LocalDate? get() = pontos.firstOrNull()?.inicioDoMes
    val ultimoMes: LocalDate? get() = pontos.lastOrNull()?.inicioDoMes

    val patrimonioLiquidoFinal: Double
        get() = pontos.lastOrNull()?.patrimonioLiquidoFimDoMes ?: 0.0

    val aporteTotal: Double
        get() = pontos.sumOf { it.aporteNoMes }

    /** Maior patrimônio líquido da série (para o eixo Y do gráfico). */
    val maxPatrimonioLiquido: Double
        get() = pontos.maxOfOrNull { it.patrimonioLiquidoFimDoMes } ?: 0.0
}

/**
 * Constrói a série mês a mês dos juros compostos.
 *
 * Convenção fechada em disco (nunca calcula "silenciosamente" com taxa 0):
 *
 * ```
 * saldo(m)   = saldo(m-1) * (1 + i) + aporte
 * rendimento(m) = saldo(m-1) * i
 * saldo(0)   = investimento inicial
 * ```
 *
 * - o aporte do mês *m* é creditado no fim do mês (rende a partir do mês *m+1*);
 * - o loop cobre `1..meses` sem sobrescrever ponto final (nenhuma linha zerada);
 * - os meses começam no mês seguinte à data de início (fonte de relógio injetável);
 * - sempre devolve exatamente `meses` pontos ordenados; nunca lança
 *   (valores não finitos viram `0.0`);
 * - a UI consome isso, nunca rede/Thread.
 */
object PatrimonioSeriesBuilder {

    fun construir(
        valorInicial: Double,
        aporteMensal: Double,
        meses: Int,
        taxaMensalPercentual: Double,
        inicio: LocalDate = LocalDate.now().withDayOfMonth(1),
    ): PatrimonioSeries {
        val n = max(1, meses)
        val i = taxaMensalPercentual.sane() / 100.0
        val p0 = valorInicial.sane()
        val a = aporteMensal.sane()
        val pontos = ArrayList<PatrimonioPoint>(n)

        var saldo = p0
        for (m in 1..n) {
            val rendimento = saldo * i
            saldo = saldo + rendimento + a
            pontos += PatrimonioPoint(
                inicioDoMes = inicio.plusMonths(m.toLong()),
                aporteNoMes = a,
                rendimentoNoMes = rendimento,
                patrimonioLiquidoFimDoMes = saldo,
            )
        }

        return PatrimonioSeries(pontos = pontos)
    }

    /** Normaliza valores não finitos para 0.0 (hermeticidade máxima). */
    private fun Double.sane() = if (this.isFinite()) this else 0.0
}