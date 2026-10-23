package com.nestvest.expense

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.nestvest.expense.data.RoomReminderRepository
import com.nestvest.expense.domain.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.expense.reminder.ReminderScheduler
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ExpenseReminderWorkerTest {
    @Test fun uniqueWorkReplacesOnTimeChangeAndCancelsOnDisableOrRevoke() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        val db = Room.inMemoryDatabaseBuilder(context, ProfileDatabase::class.java).build()
        try {
            val repo = RoomReminderRepository(db)
            var available = true
            val scheduler = ReminderScheduler(context, repo) { available }
            val work = WorkManager.getInstance(context)
            fun pending() = work.getWorkInfosForUniqueWork(ReminderScheduler.UNIQUE_WORK).get()
                .filter { !it.state.isFinished }
            scheduler.reconcile(ReminderPreference())
            assertTrue(pending().isEmpty())
            repo.update(true, 1230)
            scheduler.reconcile(ReminderPreference(enabled = true))
            val first = pending().single().id
            scheduler.reconcile(ReminderPreference(enabled = true))
            assertEquals(first, pending().single().id)
            repo.update(true, 1200)
            scheduler.reconcile(ReminderPreference(enabled = true, localTimeMinutes = 1200))
            assertNotEquals(first, pending().single().id)
            available = false
            scheduler.reconcile(ReminderPreference(enabled = true))
            assertTrue(pending().isEmpty())
            available = true
            repo.update(false, 1200)
            // A stale resume snapshot must not resurrect work after a completed disable.
            scheduler.reconcile(ReminderPreference(enabled = true))
            assertTrue(pending().isEmpty())
        } finally { db.close() }
    }

    @Test fun defaultsValidationAndAtomicClaimSurviveConcurrentRetriesAndRollback() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),
            ProfileDatabase::class.java).build()
        try {
            val repo = RoomReminderRepository(db)
            assertEquals(ReminderPreference(), (repo.read() as ReminderRead.Found).preference)
            assertTrue(repo.update(true, -1) is ReminderWrite.Failed)
            assertEquals(ReminderWrite.Saved, repo.update(true, 1230))
            val date = LocalDate.of(2026, 10, 23)
            val results = (1..12).map { async { repo.claim(date) } }.awaitAll()
            assertEquals(1, results.count { it == ReminderClaim.Claimed })
            assertEquals(ReminderClaim.AlreadyClaimed, repo.claim(date.minusDays(1)))
            assertEquals(ReminderClaim.Claimed, repo.claim(date.plusDays(1)))
            repo.update(false, 1230)
            assertEquals(ReminderClaim.AlreadyClaimed, repo.claim(date.plusDays(2)))
        } finally { db.close() }
    }
}
