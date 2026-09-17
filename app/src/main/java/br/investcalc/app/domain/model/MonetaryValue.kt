package br.investcalc.app.domain.model

import kotlin.math.roundToLong

data class MonetaryValue(val cents: Long) : Comparable<MonetaryValue> {

    operator fun plus(other: MonetaryValue): MonetaryValue = MonetaryValue(cents + other.cents)

    operator fun minus(other: MonetaryValue): MonetaryValue = MonetaryValue(cents - other.cents)

    fun times(factor: Double): MonetaryValue = MonetaryValue((cents * factor).roundToLong())

    override fun compareTo(other: MonetaryValue): Int = cents.compareTo(other.cents)

    companion object {
        val ZERO: MonetaryValue = MonetaryValue(0L)

        fun ofReais(value: Double): MonetaryValue = MonetaryValue((value * 100.0).roundToLong())
    }
}