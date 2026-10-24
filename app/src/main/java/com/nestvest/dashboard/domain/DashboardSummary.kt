package com.nestvest.dashboard.domain

import com.nestvest.expense.domain.Expense
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.Currency

sealed interface DashboardSummary {
    data object Loading : DashboardSummary
    data object NoExpenseHistory : DashboardSummary
    data class NoExpensesToday(val localDate: LocalDate, val currencyCode: String) : DashboardSummary
    data class Available(
        val localDate: LocalDate,
        val expenseCount: Int,
        val totalMinorUnits: BigInteger,
        val currencyCode: String,
    ) : DashboardSummary
    data object Unavailable : DashboardSummary
}

fun summarizeExpenses(
    expenses: List<Expense>,
    profileCurrency: String,
    clock: Clock,
): DashboardSummary {
    if (expenses.isEmpty()) return DashboardSummary.NoExpenseHistory
    val currency = try {
        Currency.getInstance(profileCurrency)
    } catch (_: IllegalArgumentException) {
        return DashboardSummary.Unavailable
    }
    val today = LocalDate.now(clock)
    val todayExpenses = expenses.filter {
        Instant.ofEpochMilli(it.capturedAtEpochMillis).atZone(clock.zone).toLocalDate() == today
    }
    if (todayExpenses.isEmpty()) return DashboardSummary.NoExpensesToday(today, currency.currencyCode)
    if (todayExpenses.any { it.currencyCode != currency.currencyCode }) return DashboardSummary.Unavailable
    val total = todayExpenses.fold(BigInteger.ZERO) { sum, expense ->
        sum + BigInteger.valueOf(expense.amountMinorUnits)
    }
    return DashboardSummary.Available(today, todayExpenses.size, total, currency.currencyCode)
}
