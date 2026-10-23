package com.nestvest.profile.domain

import java.util.Currency
import java.util.Locale

object CurrencyCatalog {
    val codes: Set<String> = Currency.getAvailableCurrencies()
        .filter { it.defaultFractionDigits >= 0 }.map { it.currencyCode }.toSortedSet()

    fun label(code: String, locale: Locale): String = Currency.getInstance(code).getDisplayName(locale)
}
