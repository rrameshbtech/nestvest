package com.nestvest.kakeibo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.ExpenseEntity
import com.nestvest.kakeibo.data.RoomKakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.profile.data.ProfileDatabase
import java.time.ZoneId
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KakeiboExpenseAssignmentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun assignsOnlyUncategorizedSameMonthAndSameCurrencyExpenses() = runBlocking {
        val name = "kakeibo-assignment-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val range = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val profileId = UUID.randomUUID().toString()
        val eligible = expense(profileId, "USD", range.startInclusive + 1_000)
        val otherCurrency = expense(profileId, "EUR", range.startInclusive + 1_000)
        val outsideMonth = expense(profileId, "USD", range.endExclusive + 1_000)
        val categorized = expense(profileId, "USD", range.startInclusive + 2_000, "Wants")
        val database = ProfileDatabase.open(context, name)
        try {
            listOf(eligible, otherCurrency, outsideMonth, categorized).forEach {
                database.expenseDao().insert(it)
            }
            val repository = RoomKakeiboRepository(database)
            assertEquals(KakeiboWrite.Saved, repository.savePlan(plan(month)))

            assertEquals(
                KakeiboWrite.Saved,
                repository.assignExpense(UUID.fromString(eligible.expenseId), KakeiboCategory.Needs, month, "USD"),
            )
            assertEquals(
                KakeiboWrite.NotEligible,
                repository.assignExpense(UUID.fromString(otherCurrency.expenseId), KakeiboCategory.Needs, month, "USD"),
            )
            assertEquals(
                KakeiboWrite.NotEligible,
                repository.assignExpense(UUID.fromString(outsideMonth.expenseId), KakeiboCategory.Needs, month, "USD"),
            )
            assertEquals(
                KakeiboWrite.NotEligible,
                repository.assignExpense(UUID.fromString(categorized.expenseId), KakeiboCategory.Needs, month, "USD"),
            )
            assertEquals("Needs", database.expenseDao().read(eligible.expenseId)?.category)
            assertNull(database.expenseDao().read(otherCurrency.expenseId)?.category)
            assertNull(database.expenseDao().read(outsideMonth.expenseId)?.category)
            assertEquals("Wants", database.expenseDao().read(categorized.expenseId)?.category)
            assertEquals(
                KakeiboWrite.NotEligible,
                repository.assignExpense(UUID.fromString(eligible.expenseId), KakeiboCategory.Culture, month, "USD"),
            )
            assertTruePlanStillSaved(repository, month)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun failedAssignmentLeavesExpenseUnchangedAndNoPlanRejectsAssignment() = runBlocking {
        val name = "kakeibo-assignment-failure-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val range = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val expense = expense(UUID.randomUUID().toString(), "USD", range.startInclusive + 1_000)
        val database = ProfileDatabase.open(context, name)
        try {
            database.expenseDao().insert(expense)
            val repository = RoomKakeiboRepository(database)
            assertEquals(
                KakeiboWrite.NotEligible,
                repository.assignExpense(UUID.fromString(expense.expenseId), KakeiboCategory.Needs, month, "USD"),
            )
            assertEquals(KakeiboWrite.Saved, repository.savePlan(plan(month)))
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_kakeibo_assignment BEFORE UPDATE OF category ON expenses " +
                    "BEGIN SELECT RAISE(ABORT, 'test failure'); END",
            )
            val result = repository.assignExpense(
                UUID.fromString(expense.expenseId),
                KakeiboCategory.Needs,
                month,
                "USD",
            )
            assertTrue(result is KakeiboWrite.Failed)
            assertNull(database.expenseDao().read(expense.expenseId)?.category)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun batchAssignmentChangesOnlySelectedCategoriesAndPreservesOtherFields() = runBlocking {
        val name = "kakeibo-batch-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val range = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val first = expense(UUID.randomUUID().toString(), "USD", range.startInclusive + 1_000)
        val second = first.copy(expenseId = UUID.randomUUID().toString(), amountMinorUnits = 350)
        val unselected = first.copy(expenseId = UUID.randomUUID().toString())
        val database = ProfileDatabase.open(context, name)
        try {
            listOf(first, second, unselected).forEach { database.expenseDao().insert(it) }
            val repository = RoomKakeiboRepository(database)
            repository.savePlan(plan(month))
            assertEquals(KakeiboWrite.InvalidDraft,
                repository.assignExpenses(emptySet(), KakeiboCategory.Needs, month, "USD"))
            assertEquals(KakeiboWrite.Saved, repository.assignExpenses(
                linkedSetOf(UUID.fromString(first.expenseId), UUID.fromString(second.expenseId)),
                KakeiboCategory.Needs, month, "USD",
            ))
            assertEquals(first.copy(category = "Needs"), database.expenseDao().read(first.expenseId))
            assertEquals(second.copy(category = "Needs"), database.expenseDao().read(second.expenseId))
            assertEquals(unselected, database.expenseDao().read(unselected.expenseId))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun ineligibleBatchMemberRollsBackEarlierUpdates() = runBlocking {
        val name = "kakeibo-batch-stale-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val range = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val first = expense(UUID.randomUUID().toString(), "USD", range.startInclusive + 1_000)
        val database = ProfileDatabase.open(context, name)
        try {
            database.expenseDao().insert(first)
            val repository = RoomKakeiboRepository(database)
            repository.savePlan(plan(month))
            val ineligible = listOf(
                first.copy(expenseId = UUID.randomUUID().toString(), currencyCode = "EUR"),
                first.copy(expenseId = UUID.randomUUID().toString(), capturedAtEpochMillis = range.endExclusive),
                first.copy(expenseId = UUID.randomUUID().toString(), category = "Wants"),
            )
            ineligible.forEach { changed ->
                database.expenseDao().insert(changed)
                assertEquals(KakeiboWrite.NotEligible, repository.assignExpenses(
                    linkedSetOf(UUID.fromString(first.expenseId), UUID.fromString(changed.expenseId)),
                    KakeiboCategory.Needs, month, "USD",
                ))
                assertEquals(first, database.expenseDao().read(first.expenseId))
                assertEquals(changed, database.expenseDao().read(changed.expenseId))
            }
            assertEquals(KakeiboWrite.NotEligible, repository.assignExpenses(
                linkedSetOf(UUID.fromString(first.expenseId), UUID.randomUUID()),
                KakeiboCategory.Needs, month, "USD",
            ))
            assertEquals(first, database.expenseDao().read(first.expenseId))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun batchWriteFailureRollsBackEverySelectedExpense() = runBlocking {
        val name = "kakeibo-batch-failure-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val range = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val first = expense(UUID.randomUUID().toString(), "USD", range.startInclusive + 1_000)
        val second = first.copy(expenseId = UUID.randomUUID().toString())
        val database = ProfileDatabase.open(context, name)
        try {
            listOf(first, second).forEach { database.expenseDao().insert(it) }
            val repository = RoomKakeiboRepository(database)
            repository.savePlan(plan(month))
            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_second_assignment BEFORE UPDATE OF category ON expenses " +
                    "WHEN OLD.expenseId = '${second.expenseId}' " +
                    "BEGIN SELECT RAISE(ABORT, 'test batch failure'); END",
            )
            assertTrue(repository.assignExpenses(
                linkedSetOf(UUID.fromString(first.expenseId), UUID.fromString(second.expenseId)),
                KakeiboCategory.Needs, month, "USD",
            ) is KakeiboWrite.Failed)
            assertEquals(first, database.expenseDao().read(first.expenseId))
            assertEquals(second, database.expenseDao().read(second.expenseId))
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private suspend fun assertTruePlanStillSaved(
        repository: RoomKakeiboRepository,
        month: YearMonth,
    ) {
        assertTrue(repository.readPlan(month) is KakeiboRead.Found)
    }

    private fun plan(month: YearMonth) = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = month,
        currencyCode = "USD",
        availableMinorUnits = null,
        savingsIntentionMinorUnits = 0,
        weeklyPacingEnabled = false,
        categoryAmounts = KakeiboCategory.entries.associateWith { 1_000L },
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    private fun expense(
        profileId: String,
        currency: String,
        capturedAt: Long,
        category: String? = null,
    ) = ExpenseEntity(
        expenseId = UUID.randomUUID().toString(),
        amountMinorUnits = 200,
        currencyCode = currency,
        capturedAtEpochMillis = capturedAt,
        createdByProfileId = profileId,
        vendor = "Shop",
        category = category,
    )
}
