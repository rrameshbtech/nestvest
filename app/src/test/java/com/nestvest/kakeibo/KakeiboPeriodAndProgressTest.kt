package com.nestvest.kakeibo

import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.CategoryHealth
import com.nestvest.kakeibo.domain.DashboardKakeiboSummary
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.calculateMonthlyProgress
import com.nestvest.kakeibo.domain.dashboardSummary
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class KakeiboPeriodAndProgressTest {
    @Test
    fun monthEpochRangeUsesLocalDateBoundariesAcrossDaylightSaving() {
        val period = KakeiboPeriod(YearMonth.of(2026, 3)).epochRange(ZoneId.of("America/Los_Angeles"))
        assertEquals(
            Instant.parse("2026-03-01T08:00:00Z").toEpochMilli(),
            period.startInclusive,
        )
        assertEquals(
            Instant.parse("2026-04-01T07:00:00Z").toEpochMilli(),
            period.endExclusive,
        )
    }

    @Test
    fun localeWeeksAreClippedToMonthBoundaries() {
        val october = KakeiboPeriod(YearMonth.of(2026, 10))
        assertEquals(
            listOf(
                "2026-10-01..2026-10-03",
                "2026-10-04..2026-10-10",
                "2026-10-11..2026-10-17",
                "2026-10-18..2026-10-24",
                "2026-10-25..2026-10-31",
            ),
            october.weeks(Locale.US).map { "${it.weekStart}..${it.weekEndInclusive}" },
        )
        assertEquals(
            listOf(
                "2026-10-01..2026-10-04",
                "2026-10-05..2026-10-11",
                "2026-10-12..2026-10-18",
                "2026-10-19..2026-10-25",
                "2026-10-26..2026-10-31",
            ),
            october.weeks(Locale.UK).map { "${it.weekStart}..${it.weekEndInclusive}" },
        )
    }

    @Test
    fun categoryProgressSeparatesCurrenciesAndUnassignedAndIncludesZeroDays() {
        val month = YearMonth.of(2026, 6)
        val plan = plan(month, mapOf(
            KakeiboCategory.Needs to 500L,
            KakeiboCategory.Wants to 0L,
            KakeiboCategory.Culture to 0L,
            KakeiboCategory.Unexpected to 0L,
        ))
        val profileId = UUID.randomUUID()
        val expenses = listOf(
            expense(100, "USD", "2026-06-01T08:00:00Z", profileId, KakeiboCategory.Needs),
            expense(450, "USD", "2026-06-30T08:00:00Z", profileId, KakeiboCategory.Needs),
            expense(700, "USD", "2026-06-15T08:00:00Z", profileId, null),
            expense(900, "EUR", "2026-06-15T08:00:00Z", profileId, KakeiboCategory.Needs),
        )

        val progress = calculateMonthlyProgress(plan, expenses, ZoneId.of("UTC"))
        val needs = progress.categoryProgress.single { it.category == KakeiboCategory.Needs }
        assertEquals(550L, needs.spentMinorUnits)
        assertEquals(-50L, needs.remainingMinorUnits)
        assertEquals(CategoryHealth.OVER_PLAN, needs.health)
        assertEquals(30, needs.dailySpendingMinorUnits.size)
        assertEquals(0L, needs.dailySpendingMinorUnits[14])
        assertEquals(1, progress.unassignedExpenses.size)
        assertEquals(1, progress.otherCurrencyExpenses.size)
    }

    @Test
    fun exposesEachCategoryHealthAndNegativeRemainingWithoutColorDependence() {
        val month = YearMonth.of(2026, 6)
        val plan = plan(
            month,
            mapOf(
                KakeiboCategory.Needs to 100L,
                KakeiboCategory.Wants to 100L,
                KakeiboCategory.Culture to 20L,
                KakeiboCategory.Unexpected to 0L,
            ),
        )
        val profileId = UUID.randomUUID()
        val progress = calculateMonthlyProgress(
            plan,
            listOf(
                expense(20, "USD", "2026-06-01T08:00:00Z", profileId, KakeiboCategory.Needs),
                expense(100, "USD", "2026-06-02T08:00:00Z", profileId, KakeiboCategory.Wants),
                expense(21, "USD", "2026-06-03T08:00:00Z", profileId, KakeiboCategory.Culture),
            ),
            ZoneId.of("UTC"),
        )
        assertEquals(CategoryHealth.WITHIN_PLAN, progress.categoryProgress.single {
            it.category == KakeiboCategory.Needs
        }.health)
        assertEquals(CategoryHealth.AT_LIMIT, progress.categoryProgress.single {
            it.category == KakeiboCategory.Wants
        }.health)
        val overspent = progress.categoryProgress.single { it.category == KakeiboCategory.Culture }
        assertEquals(CategoryHealth.OVER_PLAN, overspent.health)
        assertEquals(-1L, overspent.remainingMinorUnits)
        assertEquals(CategoryHealth.NO_ALLOCATION, progress.categoryProgress.single {
            it.category == KakeiboCategory.Unexpected
        }.health)
    }

    @Test
    fun noPlanDoesNotPresentSpendingAsWithinBudgetAndDashboardUsesWorstCategory() {
        val month = YearMonth.of(2026, 6)
        val profileId = UUID.randomUUID()
        val expense = expense(250, "USD", "2026-06-10T08:00:00Z", profileId, KakeiboCategory.Needs)
        val unplanned = calculateMonthlyProgress(
            plan = null,
            expenses = listOf(expense),
            zoneId = ZoneId.of("UTC"),
            selectedMonth = month,
            currencyCode = "USD",
        )
        assertEquals(CategoryHealth.NO_PLAN, unplanned.categoryProgress.first().health)
        assertEquals(null, unplanned.categoryProgress.first().remainingMinorUnits)
        assertEquals(DashboardKakeiboSummary.NoPlan, dashboardSummary(null, unplanned))

        val plan = plan(month, mapOf(
            KakeiboCategory.Needs to 100L,
            KakeiboCategory.Wants to 1000L,
            KakeiboCategory.Culture to 0L,
            KakeiboCategory.Unexpected to 0L,
        ))
        val overspent = listOf(
            expense(250, "USD", "2026-06-10T08:00:00Z", profileId, KakeiboCategory.Needs),
        )
        val plannedProgress = calculateMonthlyProgress(plan, overspent, ZoneId.of("UTC"))
        val summary = dashboardSummary(plan, plannedProgress) as DashboardKakeiboSummary.Available
        assertEquals(CategoryHealth.OVER_PLAN, summary.health)
        assertEquals(250L, summary.spentMinorUnits)
    }

    @Test
    fun progressRejectsOverflowingExpenseTotals() {
        val month = YearMonth.of(2026, 6)
        val plan = plan(month, KakeiboCategory.entries.associateWith { Long.MAX_VALUE / 4 })
        val profileId = UUID.randomUUID()
        assertThrows(ArithmeticException::class.java) {
            calculateMonthlyProgress(
                plan,
                listOf(
                    expense(Long.MAX_VALUE, "USD", "2026-06-10T08:00:00Z", profileId, KakeiboCategory.Needs),
                    expense(1, "USD", "2026-06-11T08:00:00Z", profileId, KakeiboCategory.Needs),
                ),
                ZoneId.of("UTC"),
            )
        }
    }

    private fun plan(month: YearMonth, amounts: Map<KakeiboCategory, Long>) = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = month,
        currencyCode = "USD",
        availableMinorUnits = null,
        savingsIntentionMinorUnits = 0,
        weeklyPacingEnabled = false,
        categoryAmounts = amounts,
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    private fun expense(
        amount: Long,
        currency: String,
        capturedAt: String,
        profileId: UUID,
        category: KakeiboCategory?,
    ) = Expense(
        expenseId = UUID.randomUUID(),
        amountMinorUnits = amount,
        currencyCode = currency,
        capturedAtEpochMillis = Instant.parse(capturedAt).toEpochMilli(),
        createdByProfileId = profileId,
        category = category,
    )
}
