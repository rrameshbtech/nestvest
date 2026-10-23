package com.nestvest.expense

import com.nestvest.expense.domain.*
import java.math.BigInteger
import java.time.*
import java.util.*
import org.junit.Assert.*
import org.junit.Test

class ExpensePeriodTest {
    @Test fun dayUsesCalendarBoundariesNotTwentyFourHours() {
        val clock = Clock.fixed(Instant.parse("2026-03-08T16:00:00Z"), ZoneId.of("America/New_York"))
        val range = ExpensePeriod.DAY.range(clock, Locale.US)
        assertEquals(23L * 3600000, range.endExclusive - range.startInclusive)
        assertTrue(range.contains(range.startInclusive))
        assertFalse(range.contains(range.endExclusive))
    }
    @Test fun weeksRespectLocaleAndMonthRollsAcrossYear() {
        val clock = Clock.fixed(Instant.parse("2026-01-01T12:00:00Z"), ZoneOffset.UTC)
        assertEquals(Instant.parse("2025-12-28T00:00:00Z").toEpochMilli(),
            ExpensePeriod.WEEK.range(clock, Locale.US).startInclusive)
        assertEquals(Instant.parse("2025-12-29T00:00:00Z").toEpochMilli(),
            ExpensePeriod.WEEK.range(clock, Locale.UK).startInclusive)
        assertEquals(Instant.parse("2026-02-01T00:00:00Z").toEpochMilli(),
            ExpensePeriod.MONTH.range(clock, Locale.US).endExclusive)
    }
    @Test fun totalsDoNotOverflowOrMixCurrencies() {
        fun expense(currency: String) = Expense(UUID.randomUUID(), Long.MAX_VALUE, currency, 0, UUID.randomUUID())
        val totals = expenseTotals(listOf(expense("USD"), expense("USD"), expense("EUR")))
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO), totals["USD"])
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), totals["EUR"])
    }
    @Test fun fallBackAndYearRolloverFollowLocalCalendar() {
        val fallBack = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneId.of("America/New_York"))
        val range = ExpensePeriod.DAY.range(fallBack, Locale.US)
        assertEquals(25L * 3600000, range.endExclusive - range.startInclusive)
        val yearEnd = Clock.fixed(Instant.parse("2026-12-31T23:59:00Z"), ZoneOffset.UTC)
        assertEquals(Instant.parse("2027-01-01T00:00:00Z").toEpochMilli(),
            ExpensePeriod.MONTH.range(yearEnd, Locale.US).endExclusive)
    }
}
