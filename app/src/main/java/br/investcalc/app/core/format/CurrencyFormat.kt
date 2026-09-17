package br.investcalc.app.core.format

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormat {

    private const val MAX_DIGITS = 13
    private const val CENTS_PER_UNIT = 100.0

    fun parseToCents(input: String): Long {
        val digits = input.filter(Char::isDigit)
        if (digits.isEmpty()) return 0L
        return digits.takeLast(MAX_DIGITS).toLongOrNull() ?: 0L
    }

    fun format(cents: Long, locale: Locale = Locale.getDefault()): String {
        val amount = cents.coerceAtLeast(0L) / CENTS_PER_UNIT
        return NumberFormat.getCurrencyInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(amount)
    }
}