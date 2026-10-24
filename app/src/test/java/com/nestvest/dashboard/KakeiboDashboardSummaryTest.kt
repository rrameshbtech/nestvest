package com.nestvest.dashboard

import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.CategoryHealth
import com.nestvest.kakeibo.domain.DashboardKakeiboSummary
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.calculateMonthlyProgress
import com.nestvest.kakeibo.domain.dashboardSummary
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Test

class KakeiboDashboardSummaryTest {
    @Test
    fun noPlanAndMeaningfulZeroStayDistinct() {
        assertEquals(DashboardKakeiboSummary.NoPlan, dashboardSummary(null, null))
        val plan = plan(
            mapOf(
                KakeiboCategory.Needs to 10_000L,
                KakeiboCategory.Wants to 0L,
                KakeiboCategory.Culture to 0L,
                KakeiboCategory.Unexpected to 0L,
            ),
        )
        val progress = calculateMonthlyProgress(plan, emptyList(), ZoneId.of("UTC"))
        assertEquals(
            DashboardKakeiboSummary.Available(0L, 10_000L, CategoryHealth.WITHIN_PLAN),
            dashboardSummary(plan, progress),
        )
    }

    @Test
    fun mostConstrainedApplicableCategoryDeterminesHealth() {
        val plan = plan(
            mapOf(
                KakeiboCategory.Needs to 100L,
                KakeiboCategory.Wants to 100L,
                KakeiboCategory.Culture to 500L,
                KakeiboCategory.Unexpected to 0L,
            ),
        )
        val profileId = UUID.randomUUID()
        val expenses = listOf(
            expense(100, "2026-06-01T08:00:00Z", profileId, KakeiboCategory.Needs),
            expense(150, "2026-06-02T08:00:00Z", profileId, KakeiboCategory.Wants),
        )
        val progress = calculateMonthlyProgress(plan, expenses, ZoneId.of("UTC"))
        val summary = dashboardSummary(plan, progress) as DashboardKakeiboSummary.Available
        assertEquals(250L, summary.spentMinorUnits)
        assertEquals(CategoryHealth.OVER_PLAN, summary.health)
    }

    @Test
    fun noAllocationIsUsedOnlyWhenNothingWasAllocatedOrSpent() {
        val zeroPlan = plan(KakeiboCategory.entries.associateWith { 0L })
        val zeroProgress = calculateMonthlyProgress(zeroPlan, emptyList(), ZoneId.of("UTC"))
        assertEquals(
            CategoryHealth.NO_ALLOCATION,
            (dashboardSummary(zeroPlan, zeroProgress) as DashboardKakeiboSummary.Available).health,
        )
        val expense = expense(1, "2026-06-01T08:00:00Z", UUID.randomUUID(), KakeiboCategory.Needs)
        val spent = calculateMonthlyProgress(zeroPlan, listOf(expense), ZoneId.of("UTC"))
        assertEquals(
            CategoryHealth.OVER_PLAN,
            (dashboardSummary(zeroPlan, spent) as DashboardKakeiboSummary.Available).health,
        )
    }

    private fun plan(amounts: Map<KakeiboCategory, Long>) = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = YearMonth.of(2026, 6),
        currencyCode = "USD",
        availableMinorUnits = null,
        savingsIntentionMinorUnits = 0,
        weeklyPacingEnabled = false,
        categoryAmounts = amounts,
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    private fun expense(amount: Long, date: String, profileId: UUID, category: KakeiboCategory) = Expense(
        expenseId = UUID.randomUUID(),
        amountMinorUnits = amount,
        currencyCode = "USD",
        capturedAtEpochMillis = Instant.parse(date).toEpochMilli(),
        createdByProfileId = profileId,
        category = category,
    )
}
