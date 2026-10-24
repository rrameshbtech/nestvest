package com.nestvest.dashboard

import com.nestvest.dashboard.domain.DashboardSummary
import com.nestvest.dashboard.domain.summarizeExpenses
import com.nestvest.expense.domain.Expense
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSummaryTest {
    private val profileId = UUID.randomUUID()
    private val zone = ZoneId.of("America/Los_Angeles")
    private val clock = Clock.fixed(Instant.parse("2026-10-24T08:30:00Z"), zone)

    @Test fun includesLocalDayBoundaryAndAddsExactMinorUnits() {
        val localStart = Instant.parse("2026-10-24T07:00:00Z").toEpochMilli()
        val expenses = listOf(
            expense(101, localStart - 1, "USD"),
            expense(123, localStart, "USD"),
            expense(Long.MAX_VALUE, localStart + 1, "USD"),
        )

        val result = summarizeExpenses(expenses, "USD", clock)

        assertTrue(result is DashboardSummary.Available)
        result as DashboardSummary.Available
        assertEquals(2, result.expenseCount)
        assertEquals("9223372036854775930", result.totalMinorUnits.toString())
        assertEquals("2026-10-24", result.localDate.toString())
    }

    @Test fun distinguishesNoHistoryAndNoActivityToday() {
        assertEquals(DashboardSummary.NoExpenseHistory, summarizeExpenses(emptyList(), "USD", clock))
        assertTrue(
            summarizeExpenses(listOf(expense(99, Instant.parse("2026-10-23T18:00:00Z").toEpochMilli(), "USD")),
                "USD", clock) is DashboardSummary.NoExpensesToday,
        )
    }

    @Test fun doesNotCombineDifferentCurrencies() {
        assertEquals(
            DashboardSummary.Unavailable,
            summarizeExpenses(listOf(expense(100, clock.millis(), "EUR")), "USD", clock),
        )
        assertTrue(
            summarizeExpenses(
                listOf(
                    expense(100, Instant.parse("2026-10-23T18:00:00Z").toEpochMilli(), "EUR"),
                    expense(200, clock.millis(), "USD"),
                ),
                "USD",
                clock,
            ) is DashboardSummary.Available,
        )
    }

    private fun expense(amount: Long, capturedAt: Long, currency: String) =
        Expense(UUID.randomUUID(), amount, currency, capturedAt, profileId)
}
