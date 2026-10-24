package com.nestvest.kakeibo.domain

import com.nestvest.expense.domain.Expense
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import java.text.NumberFormat

enum class CategoryHealth { NO_PLAN, NO_ALLOCATION, WITHIN_PLAN, AT_LIMIT, OVER_PLAN }

data class CategoryProgress(
    val category: KakeiboCategory,
    val plannedMinorUnits: Long?,
    val spentMinorUnits: Long,
    val remainingMinorUnits: Long?,
    val health: CategoryHealth,
    val dailySpendingMinorUnits: List<Long>,
)

data class MonthlyProgress(
    val plan: MonthlyPlan?,
    val month: YearMonth,
    val currencyCode: String,
    val categoryProgress: List<CategoryProgress>,
    val unassignedExpenses: List<Expense>,
    val otherCurrencyExpenses: List<Expense>,
)

sealed interface DashboardKakeiboSummary {
    data object Loading : DashboardKakeiboSummary
    data object NoPlan : DashboardKakeiboSummary
    data object Unavailable : DashboardKakeiboSummary
    data class Available(
        val spentMinorUnits: Long,
        val plannedMinorUnits: Long,
        val health: CategoryHealth,
    ) : DashboardKakeiboSummary
}

fun calculateMonthlyProgress(
    plan: MonthlyPlan?,
    expenses: List<Expense>,
    zoneId: ZoneId,
    selectedMonth: YearMonth = checkNotNull(plan?.month) { "Selected month is required without a plan" },
    currencyCode: String = checkNotNull(plan?.currencyCode) { "Currency is required without a plan" },
): MonthlyProgress {
    require(plan == null || (plan.month == selectedMonth && plan.currencyCode == currencyCode))
    val start = selectedMonth.atDay(1)
    val days = selectedMonth.lengthOfMonth()
    val monthExpenses = expenses.filter {
        val date = it.localDate(zoneId)
        date >= start && date <= selectedMonth.atEndOfMonth()
    }
    val unassigned = monthExpenses.filter { it.category == null && it.currencyCode == currencyCode }
    val otherCurrency = monthExpenses.filter { it.currencyCode != currencyCode }
    val categories = canonicalKakeiboCategories.map { category ->
        val matching = monthExpenses.filter {
            it.currencyCode == currencyCode && it.category == category
        }
        val byDate = matching.groupBy { it.localDate(zoneId) }
        val daily = (0 until days).map { offset ->
            byDate[start.plusDays(offset.toLong())].orEmpty().fold(0L) { total, expense ->
                Math.addExact(total, expense.amountMinorUnits)
            }
        }
        val spent = daily.fold(0L, Math::addExact)
        val planned = plan?.categoryAmounts?.getValue(category)
        val health = if (planned == null) {
            CategoryHealth.NO_PLAN
        } else {
            when {
                planned == 0L && spent == 0L -> CategoryHealth.NO_ALLOCATION
                spent > planned -> CategoryHealth.OVER_PLAN
                spent == planned -> CategoryHealth.AT_LIMIT
                else -> CategoryHealth.WITHIN_PLAN
            }
        }
        CategoryProgress(
            category = category,
            plannedMinorUnits = planned,
            spentMinorUnits = spent,
            remainingMinorUnits = planned?.let { Math.subtractExact(it, spent) },
            health = health,
            dailySpendingMinorUnits = daily,
        )
    }
    return MonthlyProgress(plan, selectedMonth, currencyCode, categories, unassigned, otherCurrency)
}

fun dashboardSummary(plan: MonthlyPlan?, progress: MonthlyProgress?): DashboardKakeiboSummary {
    if (plan == null || progress == null) return DashboardKakeiboSummary.NoPlan
    val applicable = progress.categoryProgress.filter {
        it.health != CategoryHealth.NO_ALLOCATION && it.health != CategoryHealth.NO_PLAN
    }
    val health = applicable.maxByOrNull { it.health.rank }?.health ?: CategoryHealth.NO_ALLOCATION
    val spent = progress.categoryProgress.fold(0L) { total, row ->
        Math.addExact(total, row.spentMinorUnits)
    }
    return DashboardKakeiboSummary.Available(
        spentMinorUnits = spent,
        plannedMinorUnits = plan.categoryTotalMinorUnits,
        health = health,
    )
}

private val CategoryHealth.rank: Int
    get() = when (this) {
        CategoryHealth.NO_PLAN -> 0
        CategoryHealth.NO_ALLOCATION -> 1
        CategoryHealth.WITHIN_PLAN -> 1
        CategoryHealth.AT_LIMIT -> 2
        CategoryHealth.OVER_PLAN -> 3
    }

fun Long.formatAsCurrency(currencyCode: String, locale: Locale): String {
    val currency = Currency.getInstance(currencyCode)
    val fractionDigits = currency.defaultFractionDigits
    require(fractionDigits >= 0)
    val scale = java.math.BigDecimal.TEN.pow(fractionDigits)
    return NumberFormat.getCurrencyInstance(locale).apply { this.currency = currency }
        .format(java.math.BigDecimal.valueOf(this).divide(scale))
}
fun Expense.localDate(zoneId: ZoneId): java.time.LocalDate =
    Instant.ofEpochMilli(capturedAtEpochMillis).atZone(zoneId).toLocalDate()
