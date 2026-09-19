package br.investcalc.app.core.format

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Formatação **de saída** de valores monetários (T0/P4). Único formatador de
 * moeda de saída do app — extendido a partir do P3.1, nunca coexiste com um
 * segundo.
 *
 * Contrato fixo:
 *  - o prefixo é **sempre** `"R$ "` (investimentos são em BRL), nos dois
 *    idiomas;
 *  - os separadores de milhar e decimal seguem o [Locale] atual:
 *    pt-BR → `"R$ 3.688,31"`, en → `"R$ 3,688.31"`.
 */
object CurrencyFormat {

    private const val MAX_DIGITS = 13
    private const val CENTS_PER_UNIT = 100.0

    fun parseToCents(input: String): Long {
        val digits = input.filter(Char::isDigit)
        if (digits.isEmpty()) return 0L
        return digits.takeLast(MAX_DIGITS).toLongOrNull() ?: 0L
    }

    /**
     * Formata centavos com prefixo fixo `"R$ "` e separadores do [locale].
     * `368831L` → pt-BR `"R$ 3.688,31"` | en `"R$ 3,688.31"`.
     */
    fun format(cents: Long, locale: Locale = Locale.getDefault()): String =
        formatReais(cents.coerceAtLeast(0L) / CENTS_PER_UNIT, locale)

    /**
     * Formata um montante em reais ([Double]) com prefixo fixo `"R$ "` e
     * separadores do [locale] — usado pelos rótulos do gráfico, tooltip e
     * Comparador. `3688.31` → pt-BR `"R$ 3.688,31"` | en `"R$ 3,688.31"`.
     */
    fun formatReais(valorReais: Double, locale: Locale = Locale.getDefault()): String =
        "R$ " + NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            roundingMode = RoundingMode.HALF_UP
        }.format(valorReais.coerceAtLeast(0.0))
}