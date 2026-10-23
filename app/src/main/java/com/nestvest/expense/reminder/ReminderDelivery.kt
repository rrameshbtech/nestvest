package com.nestvest.expense.reminder

import com.nestvest.expense.domain.*
import java.time.LocalDate
import java.time.ZoneId

/** The durable successor must commit before a date is claimed, including duplicate-day retries. */
internal class ReminderDelivery(
    private val repository: ReminderRepository,
    private val available: () -> Boolean,
    private val scheduleNext: suspend (ReminderPreference) -> Unit,
    private val post: () -> Unit,
    private val stopped: () -> Boolean = { false },
    private val today: () -> LocalDate = LocalDate::now,
) {
    suspend fun deliver(expectedMinutes: Int, expectedZone: String = ZoneId.systemDefault().id): Boolean {
        val read = repository.read()
        if (read is ReminderRead.Failed) return false
        val preference = (read as ReminderRead.Found).preference
        if (!preference.enabled || !available() || stopped()) return true
        scheduleNext(preference)
        if (expectedMinutes != preference.localTimeMinutes ||
            expectedZone != ZoneId.systemDefault().id || stopped()) return true
        return when (repository.claim(today())) {
            ReminderClaim.Claimed -> {
                val latest = repository.read()
                if (latest is ReminderRead.Failed) false
                else {
                    if (!stopped() && (latest as ReminderRead.Found).preference.enabled && available()) post()
                    true
                }
            }
            ReminderClaim.AlreadyClaimed -> true
            is ReminderClaim.Failed -> false
        }
    }
}
