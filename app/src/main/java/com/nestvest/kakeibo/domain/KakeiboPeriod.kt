package com.nestvest.kakeibo.domain

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class KakeiboPeriod(val month: YearMonth) {
    val startDate: LocalDate get() = month.atDay(1)
    val endDateInclusive: LocalDate get() = month.atEndOfMonth()

    fun epochRange(zoneId: ZoneId): EpochRange = EpochRange(
        startInclusive = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
        endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli(),
    )

    fun weeks(locale: Locale): List<KakeiboWeek> {
        val localizedDayOfWeek = WeekFields.of(locale).dayOfWeek()
        val weeks = mutableListOf<KakeiboWeek>()
        var weekStart = startDate
        while (weekStart <= endDateInclusive) {
            val nextWeekStart = weekStart.with(localizedDayOfWeek, 1).plusWeeks(1)
            val weekEnd = minOf(nextWeekStart.minusDays(1), endDateInclusive)
            weeks += KakeiboWeek(weekStart, weekEnd)
            weekStart = weekEnd.plusDays(1)
        }
        return weeks
    }
}

data class KakeiboWeek(
    val weekStart: LocalDate,
    val weekEndInclusive: LocalDate,
)

data class EpochRange(
    val startInclusive: Long,
    val endExclusive: Long,
) {
    init {
        require(startInclusive < endExclusive)
    }

    fun contains(epochMillis: Long): Boolean =
        epochMillis >= startInclusive && epochMillis < endExclusive
}

fun Instant.toLocalDate(zoneId: ZoneId): LocalDate = atZone(zoneId).toLocalDate()
