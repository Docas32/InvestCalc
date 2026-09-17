package br.investcalc.app.domain.chart

import java.time.LocalDate
import kotlin.math.max

/**
 * Linha base de **juros simples** do gráfico de evolução — o contrafactual
 * que torna o "card de destaque" verdadeiro:
 *
 * > "A juros simples você teria R$ W — os juros sobre juros te deram R$ Z a mais."
 *
 * Fórmula fechada equivalente ao somatório dos aportes mensais creditados no
 * fim de cada mês (aporte do mês *m* rende por `n-m` meses):
 *
 * ```
 * jurosSimples = P·i·n + A·i·n·(n-1)/2
 * saldo(m)     = valorInicial + aporte·m + jurosSimplesAcumulado(m)
 * totalInvestido(m) = valorInicial + aporte·m
 * ```
 *
 * Com P=1000, A=100, i=1% a.m., n=12: jurosSimples = 1000·0,01·12 +
 * 100·0,01·12·11/2 = 120 + 66 = **186,00** (caso de referência do contrato).
 *
 * Domínio puro JVM — sem Android, sem rede; nunca lança (valores não finitos
 * viram 0.0). A UI desenha esta série como linha reta cinza por cima da curva
 * exponencial dos juros compostos.
 */
data class JurosSimplesBaselinePoint(
    val inicioDoMes: LocalDate,
    val totalInvestidoFimDoMes: Double,
    val saldoJurosSimplesFimDoMes: Double,
    val jurosSimplesAcumuladoFimDoMes: Double,
)

/** Série completa da linha base de juros simples (mês a mês). */
data class JurosSimplesBaseline(
    val pontos: List<JurosSimplesBaselinePoint>,
) {
    val totalInvestidoFinal: Double get() = pontos.lastOrNull()?.totalInvestidoFimDoMes ?: 0.0
    val saldoJurosSimplesFinal: Double get() = pontos.lastOrNull()?.saldoJurosSimplesFimDoMes ?: 0.0
    val jurosSimplesTotal: Double get() = pontos.lastOrNull()?.jurosSimplesAcumuladoFimDoMes ?: 0.0
}

object JurosSimplesBaselineBuilder {

    /** Juros simples fechados (P·i·n + A·i·n·(n-1)/2) — caso-padrão → 186,00. */
    fun jurosSimplesFechados(
        valorInicial: Double,
        aporteMensal: Double,
        mes: Int,
        taxaPercentualMensal: Double,
    ): Double {
        val p = valorInicial.sane()
        val a = aporteMensal.sane()
        val i = (taxaPercentualMensal.sane() / 100.0)
        val n = mes.toDouble().coerceAtLeast(0.0)
        return p * i * n + a * i * n * (n - 1.0) / 2.0
    }

    /**
     * Constrói a linha base mês a mês. [patrimonioJurosCompostosFimDoMes]
     * vem da série composta já existente — será XOR com a linha simples pelo
     * card de destaque (Z = composto − simples).
     */
    fun construir(
        valorInicial: Double,
        aporteMensal: Double,
        meses: Int,
        taxaPercentualMensal: Double,
    ): JurosSimplesBaseline {
        val n = max(1, meses)
        val pontos = ArrayList<JurosSimplesBaselinePoint>(n)
        for (mes in 0 until n) {
            val m = mes + 1
            val totalInvestido = valorInicial.sane() + aporteMensal.sane() * m
            val jurosSimples = jurosSimplesFechados(valorInicial, aporteMensal, m, taxaPercentualMensal)
            pontos += JurosSimplesBaselinePoint(
                inicioDoMes = LocalDate.now().withDayOfMonth(1).plusMonths(mes.toLong()),
                totalInvestidoFimDoMes = totalInvestido,
                saldoJurosSimplesFimDoMes = totalInvestido + jurosSimples,
                jurosSimplesAcumuladoFimDoMes = jurosSimples,
            )
        }
        return JurosSimplesBaseline(pontos = pontos)
    }

    private fun Double.sane(): Double = if (this.isFinite() && this >= 0.0) this else 0.0
}
