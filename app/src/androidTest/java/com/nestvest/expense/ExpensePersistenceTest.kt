package com.nestvest.expense

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.ReminderPreferenceEntity
import com.nestvest.expense.data.ExpenseEntity
import com.nestvest.expense.data.RoomExpenseRepository
import com.nestvest.expense.domain.ExpenseRead
import com.nestvest.expense.domain.ExpenseWrite
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpensePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val capturedAt = Instant.parse("2026-10-22T12:34:56Z")

    @Test fun emptyHistoryAndDisabledReminderPreferenceSurviveReopen() = runBlocking {
        val name = "expense-empty-${UUID.randomUUID()}.db"
        var db = ProfileDatabase.open(context, name)
        try {
            assertTrue(db.expenseDao().observeAll().first().isEmpty())
            db.reminderPreferenceDao().initialize()
            assertEquals(ReminderPreferenceEntity(), db.reminderPreferenceDao().read())

            db.close()
            db = ProfileDatabase.open(context, name)
            assertEquals(ReminderPreferenceEntity(), db.reminderPreferenceDao().read())
            assertTrue(db.expenseDao().observeAll().first().isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun sameAmountAndCaptureTimeStillReceiveDistinctExpenseIds() = runBlocking {
        val name = "expense-identities-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val ids = listOf(
                UUID.fromString("31f54d6f-8513-43c4-93ce-86ebea9d8001"),
                UUID.fromString("31f54d6f-8513-43c4-93ce-86ebea9d8002"),
            )
            var nextId = 0
            val repository = RoomExpenseRepository(
                db,
                clock = Clock.fixed(capturedAt, ZoneOffset.UTC),
                newId = { ids[nextId++] },
            )

            val first = repository.create(1250, profile()) as ExpenseWrite.Saved
            val second = repository.create(1250, profile()) as ExpenseWrite.Saved

            assertNotEquals(first.expense.expenseId, second.expense.expenseId)
            assertEquals(first.expense.amountMinorUnits, second.expense.amountMinorUnits)
            assertEquals(first.expense.capturedAtEpochMillis, second.expense.capturedAtEpochMillis)
            assertEquals(capturedAt.toEpochMilli(), first.expense.capturedAtEpochMillis)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun savedExpenseRetainsCurrencyTimeAndCreatorAfterReopen() = runBlocking {
        val name = "expense-reopen-${UUID.randomUUID()}.db"
        var db = ProfileDatabase.open(context, name)
        try {
            val profile = profile()
            val expectedTime = capturedAt.toEpochMilli()
            val saved = RoomExpenseRepository(db, Clock.fixed(capturedAt, ZoneOffset.UTC))
                .create(499, profile) as ExpenseWrite.Saved
            db.close()

            db = ProfileDatabase.open(context, name)
            val result = RoomExpenseRepository(db).observeAll().first() as ExpenseRead.Found
            assertEquals(1, result.expenses.size)
            assertEquals(saved.expense.expenseId, result.expenses.single().expenseId)
            assertEquals(499L, result.expenses.single().amountMinorUnits)
            assertEquals("INR", result.expenses.single().currencyCode)
            assertEquals(expectedTime, result.expenses.single().capturedAtEpochMillis)
            assertEquals(profile.profileId, result.expenses.single().createdByProfileId)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun rejectedInsertReturnsFailureAndLeavesHistoryEmpty() = runBlocking {
        val name = "expense-failure-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_expense BEFORE INSERT ON expenses " +
                    "BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            val result = RoomExpenseRepository(db, Clock.fixed(capturedAt, ZoneOffset.UTC))
                .create(1250, profile())

            assertTrue(result is ExpenseWrite.Failed)
            assertTrue((RoomExpenseRepository(db).observeAll().first() as ExpenseRead.Found).expenses.isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }

        @Test fun halfOpenQueriesSortNewestFirstAndInvalidPersistedRowsSurfaceAsErrors() = runBlocking {
            val name = "expense-range-${UUID.randomUUID()}.db"
            val db = ProfileDatabase.open(context, name)
            try {
                val ids = (1..4).map { "31f54d6f-8513-43c4-93ce-86ebea9d800$it" }
                listOf(100L, 200L, 200L, 300L).forEachIndexed { index, time ->
                    db.expenseDao().insert(ExpenseEntity(ids[index], 1, "USD", time,
                        profile().profileId.toString()))
                }
                val rows = db.expenseDao().observe(100, 300).first()
                assertEquals(listOf(ids[2], ids[1], ids[0]), rows.map { it.expenseId })
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE expenses SET currencyCode = 'INVALID' WHERE expenseId = ?", arrayOf(ids[0]))
                assertTrue(RoomExpenseRepository(db).observeAll().first() is ExpenseRead.Failed)
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }

    private fun profile() = PrimaryProfile(
        UUID.fromString("446dc509-0f45-4a92-a4d5-201e9b6f8592"),
        "Mira",
        "INR",
    )
}
