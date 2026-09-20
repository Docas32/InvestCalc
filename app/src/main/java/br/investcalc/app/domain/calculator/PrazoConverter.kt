package br.investcalc.app.domain.calculator

import kotlin.math.ceil

/**
 * Unidade de prazo escolhida pelo usuário nos formulários (T6/P7).
 */
enum class UnidadePrazo { DIAS, MESES, ANOS }

/**
 * Conversão da unidade de prazo informada na UI para o **motor**, que
 * permanece em meses.
 *
 * - anos × 12;
 * - dias → `ceil(dias / 30,4375)`, mínimo 1 (entrada em dias com menos de 1
 *   mês exibe 1 linha na tabela);
 * - validações por unidade: dias 1..18.250, meses 1..9.999, anos 1..50.
 *
 * IR continua calculado por meses (simplificação documentada no Guia).
 * Puro e hermético (JVM, sem Android/rede); nunca lança.
 */
object PrazoConverter {

    /** Dias médios por mês (365,25 / 12) usados na conversão dias → meses. */
    const val DIAS_POR_MES = 30.4375

    const val DIAS_MIN = 1
    const val DIAS_MAX = 18_250
    const val MESES_MIN = 1
    const val MESES_MAX = 9_999
    const val ANOS_MIN = 1
    const val ANOS_MAX = 50

    /** Converte prazo em [unidade] para meses do motor. */
    fun paraMeses(valor: Int, unidade: UnidadePrazo): Int = when (unidade) {
        UnidadePrazo.MESES -> valor
        UnidadePrazo.ANOS -> valor * 12
        UnidadePrazo.DIAS -> ceil(valor / DIAS_POR_MES).toInt().coerceAtLeast(1)
    }

    /** Faixas válidas por unidade (dias 1..18.250; meses 1..9.999; anos 1..50). */
    fun valido(valor: Int, unidade: UnidadePrazo): Boolean = when (unidade) {
        UnidadePrazo.DIAS -> valor in DIAS_MIN..DIAS_MAX
        UnidadePrazo.MESES -> valor in MESES_MIN..MESES_MAX
        UnidadePrazo.ANOS -> valor in ANOS_MIN..ANOS_MAX
    }
}