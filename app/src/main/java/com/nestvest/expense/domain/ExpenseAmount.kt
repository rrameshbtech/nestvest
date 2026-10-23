package com.nestvest.expense.domain

import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object ExpenseAmount {
    fun parse(text: String, currencyCode: String, locale: Locale = Locale.getDefault()): Long? {
        val scale = Currency.getInstance(currencyCode).defaultFractionDigits
        if (scale < 0 || text.isEmpty()) return null
        val separator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
        val normalized = buildString {
            for (char in text) {
                when {
                    char == separator -> append('.')
                    Character.digit(char, 10) >= 0 -> append(Character.digit(char, 10))
                    else -> return null
                }
            }
        }
        if (!normalized.matches(Regex("[0-9]+(?:\\.[0-9]+)?"))) return null
        if (normalized.substringAfter('.', "").length > scale) return null
        val value = BigDecimal(normalized).movePointRight(scale)
        if (value <= BigDecimal.ZERO || value > BigDecimal.valueOf(Long.MAX_VALUE)) return null
        return value.longValueExact()
    }

    fun format(amount: Long, currency: String, locale: Locale = Locale.getDefault()) =
        format(BigInteger.valueOf(amount), currency, locale)

    fun format(amount: BigInteger, currency: String, locale: Locale = Locale.getDefault()): String {
        val unit = Currency.getInstance(currency)
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = unit
            minimumFractionDigits = unit.defaultFractionDigits
            maximumFractionDigits = unit.defaultFractionDigits
        }.format(BigDecimal(amount, unit.defaultFractionDigits))
    }
}
