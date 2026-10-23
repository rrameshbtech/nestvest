package com.nestvest.expense.domain

import java.util.Currency
import java.util.UUID

enum class KakeiboCategory { Needs, Wants, Culture, Unexpected }

data class Expense(
    val expenseId: UUID,
    val amountMinorUnits: Long,
    val currencyCode: String,
    val capturedAtEpochMillis: Long,
    val createdByProfileId: UUID,
    val vendor: String? = null,
    val category: KakeiboCategory? = null,
    val receiptFileName: String? = null,
) {
    init {
        require(amountMinorUnits > 0)
        require(Currency.getInstance(currencyCode).defaultFractionDigits >= 0)
        require(vendor == null || (vendor.isNotBlank() && vendor == vendor.trim() &&
            vendor.codePointCount(0, vendor.length) <= 80))
        require(receiptFileName == null || receiptFileName.matches(Regex("[a-f0-9-]+\\.receipt")))
    }
}
