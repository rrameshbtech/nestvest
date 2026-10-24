package com.nestvest.kakeibo.domain

import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.UUID

data class MonthlyPlan(
    val planId: UUID,
    val month: YearMonth,
    val currencyCode: String,
    val availableMinorUnits: Long?,
    val savingsIntentionMinorUnits: Long,
    val weeklyPacingEnabled: Boolean,
    val categoryAmounts: Map<KakeiboCategory, Long>,
    val weeklyAllocations: List<WeeklyAllocation>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(month.toString().matches(Regex("\\d{4}-\\d{2}")))
        require(Currency.getInstance(currencyCode).defaultFractionDigits >= 0)
        require(availableMinorUnits == null || availableMinorUnits >= 0)
        require(savingsIntentionMinorUnits >= 0)
        require(categoryAmounts.keys == canonicalKakeiboCategories.toSet())
        require(categoryAmounts.values.all { it >= 0 })
        val total = categoryAmounts.values.fold(savingsIntentionMinorUnits, Math::addExact)
        require(availableMinorUnits == null || total <= availableMinorUnits)
        require(weeklyPacingEnabled || weeklyAllocations.isEmpty())
        require(weeklyAllocations.all {
            it.weekStartDate >= month.atDay(1) &&
                it.weekEndDateInclusive <= month.atEndOfMonth() &&
                it.plannedMinorUnits >= 0
        })
        require(weeklyAllocations.map { it.category to it.weekStartDate }.distinct().size == weeklyAllocations.size)
        require(weeklyAllocations.groupBy { it.category }.all { (category, rows) ->
            rows.fold(0L) { totalWeekly, row -> Math.addExact(totalWeekly, row.plannedMinorUnits) } <=
                categoryAmounts.getValue(category)
        })
    }

    val categoryTotalMinorUnits: Long
        get() = categoryAmounts.values.fold(0L, Math::addExact)

    val totalAllocationMinorUnits: Long
        get() = Math.addExact(categoryTotalMinorUnits, savingsIntentionMinorUnits)
}

data class WeeklyAllocation(
    val category: KakeiboCategory,
    val weekStartDate: LocalDate,
    val weekEndDateInclusive: LocalDate,
    val plannedMinorUnits: Long,
) {
    init {
        require(weekStartDate <= weekEndDateInclusive)
        require(plannedMinorUnits >= 0)
    }
}
