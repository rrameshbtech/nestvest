package com.nestvest.dashboard

import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardPreferenceRead
import com.nestvest.dashboard.domain.DashboardPreferenceRepository
import com.nestvest.dashboard.domain.DashboardPreferenceWrite
import com.nestvest.dashboard.ui.DashboardViewModel
import com.nestvest.expense.domain.Expense
import com.nestvest.expense.domain.ExpenseRead
import com.nestvest.expense.domain.ExpenseRepository
import com.nestvest.expense.domain.ExpenseWrite
import com.nestvest.kakeibo.domain.DashboardKakeiboSummary
import com.nestvest.kakeibo.domain.EpochRange
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.PeriodReflection
import com.nestvest.kakeibo.domain.WeeklyAllocation
import com.nestvest.profile.MainDispatcherRule
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import java.time.Clock
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KakeiboDashboardViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")

    @Test
    fun unavailablePlanReadNeverShowsPreviouslyLoadedSummary() = runTest {
        val kakeibo = FakeKakeiboRepository().apply { plan = KakeiboRead.Found(plan()) }
        val viewModel = dashboard(kakeibo)
        runCurrent()
        assertEquals(DashboardKakeiboSummary.Available(0L, 400L, com.nestvest.kakeibo.domain.CategoryHealth.WITHIN_PLAN),
            viewModel.state.value.kakeiboSummary)

        kakeibo.plan = KakeiboRead.Failed(StorageError.UNAVAILABLE)
        viewModel.resume()
        runCurrent()
        assertEquals(DashboardKakeiboSummary.Unavailable, viewModel.state.value.kakeiboSummary)
    }

    @Test
    fun missingPlanDisplaysPlanningPrompt() = runTest {
        val viewModel = dashboard(FakeKakeiboRepository())
        runCurrent()
        assertEquals(DashboardKakeiboSummary.NoPlan, viewModel.state.value.kakeiboSummary)
    }

    private fun dashboard(repository: FakeKakeiboRepository) = DashboardViewModel(
        preferences = FakePreferences(),
        expenses = FakeExpenseRepository(),
        profile = profile,
        clock = { Clock.fixed(java.time.Instant.parse("2026-06-15T12:00:00Z"), java.time.ZoneOffset.UTC) },
        kakeibo = repository,
    )

    private class FakePreferences : DashboardPreferenceRepository {
        override fun observe(): Flow<DashboardPreferenceRead> =
            flowOf(DashboardPreferenceRead.Found(DashboardPreference.DEFAULT))

        override suspend fun save(preference: DashboardPreference) = DashboardPreferenceWrite.Saved
    }

    private class FakeExpenseRepository : ExpenseRepository {
        private val state = MutableStateFlow<ExpenseRead>(ExpenseRead.Found(emptyList()))
        override fun observe(startInclusive: Long, endExclusive: Long) = state
        override fun observeAll() = state
        override suspend fun create(amount: Long, profile: PrimaryProfile): ExpenseWrite =
            error("Not used by dashboard")
    }

    private class FakeKakeiboRepository : KakeiboRepository {
        var plan: KakeiboRead<MonthlyPlan> = KakeiboRead.Absent
        override suspend fun readPlan(month: YearMonth) = plan
        override suspend fun savePlan(plan: MonthlyPlan) = KakeiboWrite.Saved
        override fun observeExpenses(period: EpochRange): Flow<KakeiboRead<List<Expense>>> =
            flowOf(KakeiboRead.Found(emptyList()))
        override suspend fun unassignedExpenses(
            period: EpochRange,
            currencyCode: String,
        ): KakeiboRead<List<Expense>> = KakeiboRead.Found(emptyList())
        override suspend fun assignExpenses(
            expenseIds: Set<UUID>,
            category: KakeiboCategory,
            month: YearMonth,
            currencyCode: String,
        ) = KakeiboWrite.NotEligible
        override suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection> =
            KakeiboRead.Absent
        override suspend fun saveReflection(reflection: PeriodReflection) = KakeiboWrite.Saved
        override suspend fun readLearningState() = KakeiboRead.Found(LearningPromptState.UNSEEN)
        override suspend fun saveLearningState(state: LearningPromptState) = KakeiboWrite.Saved
    }

    private fun plan() = MonthlyPlan(
        planId = UUID.randomUUID(),
        month = YearMonth.of(2026, 6),
        currencyCode = "USD",
        availableMinorUnits = null,
        savingsIntentionMinorUnits = 0,
        weeklyPacingEnabled = false,
        categoryAmounts = KakeiboCategory.entries.associateWith { 100L },
        weeklyAllocations = emptyList(),
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )
}
