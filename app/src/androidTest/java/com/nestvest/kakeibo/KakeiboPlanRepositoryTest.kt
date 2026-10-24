package com.nestvest.kakeibo

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.kakeibo.data.RoomKakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.profile.data.ProfileDatabase
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KakeiboPlanRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun failedReplacementRollsBackPlanAndCategoryRows() = runBlocking {
        val name = "kakeibo-plan-rollback-${UUID.randomUUID()}.db"
        val month = YearMonth.of(2026, 6)
        val database = ProfileDatabase.open(context, name)
        try {
            val repository = RoomKakeiboRepository(database)
            val original = plan(month, 1_000L)
            assertEquals(KakeiboWrite.Saved, repository.savePlan(original))

            database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_kakeibo_category BEFORE INSERT ON kakeibo_category_plan " +
                    "WHEN NEW.categoryCode = 'Needs' " +
                    "BEGIN SELECT RAISE(ABORT, 'forced test failure'); END",
            )
            assertTrue(repository.savePlan(plan(month, 2_000L)) is KakeiboWrite.Failed)

            val saved = repository.readPlan(month) as KakeiboRead.Found
            assertEquals(original.categoryAmounts, saved.value.categoryAmounts)
            assertEquals(original.planId, saved.value.planId)
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun plan(month: YearMonth, amount: Long) = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = month,
        currencyCode = "USD",
        availableMinorUnits = null,
        savingsIntentionMinorUnits = 0,
        weeklyPacingEnabled = false,
        categoryAmounts = KakeiboCategory.entries.associateWith { amount },
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
