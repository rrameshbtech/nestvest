package com.nestvest.expense

import com.nestvest.expense.reminder.ReminderSchedule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ReminderSchedulerTest {
    @Test fun nextOccurrenceIsStrictlyFutureAndUsesLocalTime() {
        val now = Instant.parse("2026-01-01T20:30:00Z")
        assertEquals(Instant.parse("2026-01-02T20:30:00Z"),
            ReminderSchedule.next(now, ZoneId.of("UTC"), 1230))
        assertEquals(Instant.parse("2026-01-02T01:30:00Z"),
            ReminderSchedule.next(now, ZoneId.of("America/New_York"), 1230))
    }

    @Test fun daylightSavingGapAndOverlapRemainOneOccurrence() {
        val zone = ZoneId.of("America/New_York")
        assertEquals(Instant.parse("2026-03-08T07:30:00Z"),
            ReminderSchedule.next(Instant.parse("2026-03-08T05:00:00Z"), zone, 150))
        assertEquals(Instant.parse("2026-11-01T05:30:00Z"),
            ReminderSchedule.next(Instant.parse("2026-11-01T04:00:00Z"), zone, 90))
        assertEquals(Instant.parse("2026-11-02T06:30:00Z"),
            ReminderSchedule.next(Instant.parse("2026-11-01T05:45:00Z"), zone, 90))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidTime() { ReminderSchedule.next(Instant.EPOCH, ZoneId.of("UTC"), 1440) }
}
