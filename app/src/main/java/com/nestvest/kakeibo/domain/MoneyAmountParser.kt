package com.nestvest.kakeibo.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

fun parseMinorUnits(
    text: String,
    currencyCode: String,
    locale: Locale,
    blankAsZero: Boolean = false,
): Long? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return if (blankAsZero) 0L else null
    require(trimmed.all { it.isDigit() || it == DecimalFormatSymbols.getInstance(locale).decimalSeparator })
    val decimalSeparator = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    require(trimmed.count { it == decimalSeparator } <= 1)
    val fractionDigits = Currency.getInstance(currencyCode).defaultFractionDigits
    require(fractionDigits >= 0)
    val canonical = if (decimalSeparator == '.') trimmed else trimmed.replace(decimalSeparator, '.')
    val amount = BigDecimal(canonical).setScale(fractionDigits, RoundingMode.UNNECESSARY)
    val minorUnits = amount.movePointRight(fractionDigits).longValueExact()
    require(minorUnits >= 0)
    return minorUnits
}
