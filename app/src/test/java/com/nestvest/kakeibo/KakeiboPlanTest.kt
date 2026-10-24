package com.nestvest.kakeibo

import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.allocationSummary
import java.time.YearMonth
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class KakeiboPlanTest {
    @Test
    fun calculatesAvailableRemainderOnlyWhenAvailableAmountWasProvided() {
        val amounts = KakeiboCategory.entries.associateWith { 100L }
        val withoutTotal = allocationSummary(plan(amounts, available = null))
        assertEquals(400L, withoutTotal.categoryTotalMinorUnits)
        assertNull(withoutTotal.unassignedRemainderMinorUnits)

        val withTotal = allocationSummary(plan(amounts, available = 650L, savings = 50L))
        assertEquals(450L, withTotal.totalAllocationMinorUnits)
        assertEquals(200L, withTotal.unassignedRemainderMinorUnits)
    }

    @Test
    fun rejectsNegativeOrOverAllocatedAmounts() {
        val amounts = KakeiboCategory.entries.associateWith { 100L }
        assertThrows(IllegalArgumentException::class.java) {
            plan(amounts + (KakeiboCategory.Needs to -1L), available = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            plan(amounts, available = 399L)
        }
    }

    private fun plan(
        amounts: Map<KakeiboCategory, Long>,
        available: Long?,
        savings: Long = 0,
    ) = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = YearMonth.of(2026, 6),
        currencyCode = "USD",
        availableMinorUnits = available,
        savingsIntentionMinorUnits = savings,
        weeklyPacingEnabled = false,
        categoryAmounts = amounts,
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
