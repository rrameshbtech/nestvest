package com.nestvest.expense.reminder

import android.content.Context
import androidx.work.*
import com.nestvest.expense.domain.ReminderPreference
import com.nestvest.expense.domain.ReminderRead
import com.nestvest.expense.domain.ReminderRepository
import com.nestvest.expense.data.RoomReminderRepository
import com.nestvest.profile.data.ProfileDatabase
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ReminderSchedule {
    fun next(now: Instant, zone: ZoneId, minutes: Int): Instant {
        require(minutes in 0..1439)
        val date = now.atZone(zone).toLocalDate()
        fun at(day: java.time.LocalDate) =
            day.atStartOfDay().plusMinutes(minutes.toLong()).atZone(zone).toInstant()
        return at(date).takeIf { it > now } ?: at(date.plusDays(1))
    }
}

interface ReminderScheduling {
    suspend fun reconcile(preference: ReminderPreference, force: Boolean = false)
}

class ReminderScheduler(
    context: Context,
    private val repository: ReminderRepository? = null,
    private val deliveryAvailable: (() -> Boolean)? = null,
) : ReminderScheduling {
    private val context = context.applicationContext
    private val work = WorkManager.getInstance(context.applicationContext)
    private val notifier = ExpenseReminderNotifier(context.applicationContext)

    override suspend fun reconcile(preference: ReminderPreference, force: Boolean) {
        lock.withLock {
            withContext(Dispatchers.IO) {
                // A lifecycle caller may hold a snapshot older than a completed disable.
                val latest = if (repository != null) repository.read() else {
                    val database = ProfileDatabase.open(context)
                    try { RoomReminderRepository(database).read() } finally { database.close() }
                }
                check(latest is ReminderRead.Found) { "Reminder preferences unavailable" }
                val current = latest.preference
                if (!current.enabled || !(deliveryAvailable?.invoke() ?: notifier.available())) {
                    work.cancelUniqueWork(UNIQUE_WORK).result.get()
                    notifier.dismiss()
                    return@withContext
                }
                val fingerprint = fingerprint(current)
                val pending = work.getWorkInfosForUniqueWork(UNIQUE_WORK).get()
                    .filter { !it.state.isFinished }
                if (force || pending.isEmpty() || pending.any { fingerprint !in it.tags }) {
                    work.enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE,
                        request(current)).result.get()
                }
            }
        }
    }

    // A retry can see the committed successor even if this process died before claiming the date.
    internal suspend fun appendSuccessor(parent: UUID, preference: ReminderPreference) {
        withContext(Dispatchers.IO) {
            val child = UUID.nameUUIDFromBytes("reminder-successor:$parent".toByteArray(Charsets.UTF_8))
            if (work.getWorkInfoById(child).get() == null) {
                work.enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE,
                    request(preference, child)).result.get()
            }
        }
    }

    private fun request(preference: ReminderPreference, id: UUID = UUID.randomUUID()): OneTimeWorkRequest {
        val now = Instant.now()
        val next = ReminderSchedule.next(now, ZoneId.systemDefault(), preference.localTimeMinutes)
        return OneTimeWorkRequestBuilder<ExpenseReminderWorker>()
            .setId(id)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MINUTES to preference.localTimeMinutes, ZONE to ZoneId.systemDefault().id))
            .addTag(fingerprint(preference))
            .build()
    }

    private fun fingerprint(preference: ReminderPreference) =
        "reminder:${preference.localTimeMinutes}:${ZoneId.systemDefault().id}"

    companion object {
        const val UNIQUE_WORK = "daily-expense-reminder"
        internal const val MINUTES = "localTimeMinutes"
        internal const val ZONE = "zoneId"
        internal val lock = Mutex()
    }
}
