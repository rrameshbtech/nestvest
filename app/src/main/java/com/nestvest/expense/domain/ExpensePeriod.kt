package com.nestvest.expense.domain

import java.math.BigInteger
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

data class ExpenseRange(val startInclusive: Long, val endExclusive: Long) {
    fun contains(instant: Long) = instant >= startInclusive && instant < endExclusive
}

enum class ExpensePeriod {
    DAY, WEEK, MONTH;

    fun range(clock: Clock, locale: Locale = Locale.getDefault()): ExpenseRange {
        val today = LocalDate.now(clock)
        val start = when (this) {
            DAY -> today
            WEEK -> today.with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
            MONTH -> today.withDayOfMonth(1)
        }
        val end = when (this) {
            DAY -> start.plusDays(1)
            WEEK -> start.plusWeeks(1)
            MONTH -> start.plusMonths(1)
        }
        return ExpenseRange(start.atStartOfDay(clock.zone).toInstant().toEpochMilli(),
            end.atStartOfDay(clock.zone).toInstant().toEpochMilli())
    }
}

fun expenseTotals(expenses: List<Expense>): Map<String, BigInteger> =
    expenses.groupBy { it.currencyCode }.mapValues { (_, rows) ->
        rows.fold(BigInteger.ZERO) { total, row -> total + BigInteger.valueOf(row.amountMinorUnits) }
    }
