package com.nestvest.kakeibo

import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.EpochRange
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.PeriodReflection
import com.nestvest.kakeibo.domain.WeeklyAllocation
import com.nestvest.kakeibo.ui.KakeiboDestination
import com.nestvest.kakeibo.ui.KakeiboViewModel
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import com.nestvest.profile.MainDispatcherRule
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KakeiboViewModelTest {
    @get:org.junit.Rule val main = MainDispatcherRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
    private val fixedClock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun pendingBatchBlocksDuplicateConfirmationAndSelectionChanges() = runTest {
        val pending = kotlinx.coroutines.CompletableDeferred<KakeiboWrite>()
        var writes = 0
        var submitted = emptySet<UUID>()
        val repository = object : KakeiboRepository by FakeKakeiboRepository() {
            override suspend fun assignExpenses(
                expenseIds: Set<UUID>,
                category: KakeiboCategory,
                month: YearMonth,
                currencyCode: String,
            ): KakeiboWrite {
                writes++
                submitted = expenseIds
                return pending.await()
            }
        }
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        val first = Expense(UUID.randomUUID(), 200, "USD", fixedClock.millis(), profile.profileId)
        val second = first.copy(expenseId = UUID.randomUUID())
        viewModel.openCategory(KakeiboCategory.Needs)
        viewModel.beginAssignment(KakeiboCategory.Needs, first)
        viewModel.beginAssignment(KakeiboCategory.Needs, second)
        viewModel.confirmAssignment()
        runCurrent()
        viewModel.confirmAssignment()
        viewModel.cancelAssignment()
        viewModel.beginAssignment(KakeiboCategory.Needs, first)
        viewModel.back()
        viewModel.selectMonth(YearMonth.of(2026, 7))
        runCurrent()
        assertEquals(1, writes)
        assertEquals(setOf(first.expenseId, second.expenseId), submitted)
        assertEquals(submitted, viewModel.state.value.assignmentExpenseIds)
        assertEquals(YearMonth.of(2026, 6), viewModel.state.value.month)
        assertEquals(KakeiboDestination.CategoryDetail("Needs"), viewModel.state.value.navigation.current)
        pending.complete(KakeiboWrite.Saved)
        runCurrent()
        assertEquals(emptySet<UUID>(), viewModel.state.value.assignmentExpenseIds)
        assertTrue(viewModel.state.value.assignmentSaved)
    }

    @Test
    fun unseenLearningPromptIsSavedAsViewedAndNotRepeated() = runTest {
        val repository = FakeKakeiboRepository()
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        assertTrue(viewModel.state.value.showLearningPrompt)

        viewModel.openLearning()
        runCurrent()
        assertEquals(KakeiboDestination.Learning, viewModel.state.value.navigation.current)
        assertEquals(LearningPromptState.VIEWED, repository.learningState)
        assertFalse(viewModel.state.value.showLearningPrompt)
    }

    @Test
    fun dismissingLearningPromptIsPersistedAndSuppressesItOnNextEntry() = runTest {
        val repository = FakeKakeiboRepository()
        val firstViewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        firstViewModel.dismissLearningPrompt()
        runCurrent()

        assertEquals(LearningPromptState.DISMISSED, repository.learningState)
        val nextViewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        assertFalse(nextViewModel.state.value.showLearningPrompt)
    }

    @Test
    fun monthlyPlanCanBeSavedWithoutAnAvailableTotal() = runTest {
        val repository = FakeKakeiboRepository()
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateSavingsAmount("25.00")
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "400.50")
        viewModel.updateCategoryAmount(KakeiboCategory.Wants, "80")
        viewModel.updateCategoryAmount(KakeiboCategory.Culture, "20.25")
        viewModel.updateCategoryAmount(KakeiboCategory.Unexpected, "10")
        viewModel.reviewPlan()
        assertEquals(KakeiboDestination.PlanReview, viewModel.state.value.navigation.current)
        viewModel.savePlan()
        runCurrent()

        val saved = repository.savedPlan ?: error("Plan should have been saved")
        assertEquals(YearMonth.of(2026, 6), saved.month)
        assertEquals(null, saved.availableMinorUnits)
        assertEquals(40_050L, saved.categoryAmounts.getValue(KakeiboCategory.Needs))
        assertEquals(51_075L, saved.categoryTotalMinorUnits)
        assertFalse(viewModel.state.value.planSaving)
        assertTrue(viewModel.state.value.planSaved)
        assertEquals(KakeiboDestination.Progress, viewModel.state.value.navigation.current)
    }

    @Test
    fun calculatedOverallIncludesSavingsWithoutBecomingAnExplicitBudget() = runTest {
        val repository = FakeKakeiboRepository()
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        assertEquals(0L, viewModel.sliderMaximumMinorUnits())
        viewModel.updateSavingsAmount("25")
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "75")
        assertEquals(10_000L, viewModel.allocationTotalMinorUnits())
        assertEquals(10_000L, viewModel.sliderMaximumMinorUnits())
        assertEquals("", viewModel.state.value.draft.availableAmount)
        viewModel.reviewPlan()
        viewModel.savePlan()
        runCurrent()
        assertEquals(null, repository.savedPlan?.availableMinorUnits)
    }

    @Test
    fun explicitSliderMaximumNeverExpandsToFitAnOverallocatedCategory() = runTest {
        val viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateAvailableAmount("100")
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "150")
        assertEquals(10_000L, viewModel.sliderMaximumMinorUnits())
        viewModel.updateAvailableAmount("0")
        assertEquals(0L, viewModel.sliderMaximumMinorUnits())
        viewModel.updateAvailableAmount("invalid")
        assertEquals(null, viewModel.sliderMaximumMinorUnits())
        viewModel.updateAvailableAmount("")
        assertEquals(15_000L, viewModel.sliderMaximumMinorUnits())
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "invalid")
        assertEquals(null, viewModel.allocationTotalMinorUnits())
        assertEquals(null, viewModel.sliderMaximumMinorUnits())
    }

    @Test
    fun overflowingCalculatedTotalIsUnavailableRatherThanZero() = runTest {
        val viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { fixedClock })
        runCurrent()
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "92233720368547758.07")
        viewModel.updateSavingsAmount("0.01")
        assertEquals(null, viewModel.allocationTotalMinorUnits())
        assertEquals(null, viewModel.sliderMaximumMinorUnits())
    }

    @Test
    fun failedSaveRetainsDraftAndShowsFailure() = runTest {
        val repository = FakeKakeiboRepository().apply {
            planWrite = KakeiboWrite.Failed(StorageError.UNAVAILABLE)
        }
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "125")
        val draft = viewModel.state.value.draft
        viewModel.reviewPlan()
        viewModel.savePlan()
        runCurrent()

        assertEquals(draft, viewModel.state.value.draft)
        assertEquals("storage", viewModel.state.value.planError)
        assertFalse(viewModel.state.value.planSaving)
    }

    @Test
    fun weeklyPacingRejectsTotalsAboveCategoryAndRetainsDraft() = runTest {
        val viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "10")
        viewModel.toggleWeeklyPacing(true)
        val weeks = KakeiboPeriod(YearMonth.of(2026, 6)).weeks(java.util.Locale.getDefault())
        viewModel.updateWeeklyAmount(KakeiboCategory.Needs, weeks[0].weekStart, "6")
        viewModel.updateWeeklyAmount(KakeiboCategory.Needs, weeks[1].weekStart, "5")
        val draft = viewModel.state.value.draft

        viewModel.reviewPlan()

        assertEquals(draft, viewModel.state.value.draft)
        assertEquals("invalid_amount", viewModel.state.value.planError)
        assertEquals(KakeiboDestination.MonthlyPlan, viewModel.state.value.navigation.current)
    }

    @Test
    fun reflectionSaveShowsSavedViewAndEditingDoesNotRewriteUntilConfirmed() = runTest {
        val repository = FakeKakeiboRepository()
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.openReflection()
        runCurrent()
        viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.WENT_WELL, "Paused before buying")
        viewModel.saveReflection()
        runCurrent()
        assertEquals(KakeiboDestination.SavedReflection, viewModel.state.value.navigation.current)
        viewModel.editReflection()
        viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.WENT_WELL, "Unsaved change")
        viewModel.back()
        viewModel.openReflection()
        runCurrent()
        assertEquals(KakeiboDestination.SavedReflection, viewModel.state.value.navigation.current)
        assertEquals("Paused before buying", viewModel.state.value.whatWentWell)
        viewModel.returnToProgress()
        assertEquals(KakeiboDestination.Progress, viewModel.state.value.navigation.current)
        assertEquals(listOf(KakeiboDestination.Home, KakeiboDestination.Progress),
            viewModel.state.value.navigation.backStack)
    }

    @Test
    fun leavingReflectionCancelsPendingReadWithoutChangingTheNewMonth() = runTest {
        val pending = kotlinx.coroutines.CompletableDeferred<KakeiboRead<PeriodReflection>>()
        val base = FakeKakeiboRepository()
        val repository = object : KakeiboRepository by base {
            override suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection> = pending.await()
        }
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.openReflection()
        runCurrent()
        viewModel.back()
        viewModel.selectMonth(YearMonth.of(2026, 7))
        pending.complete(KakeiboRead.Absent)
        runCurrent()
        assertEquals(KakeiboDestination.Home, viewModel.state.value.navigation.current)
        assertEquals(YearMonth.of(2026, 7), viewModel.state.value.month)
        assertEquals(null, viewModel.state.value.reflection)
    }

    @Test
    fun weeklySaveRejectsOverLimitAndSkipDoesNotChangeMonthlyAmounts() = runTest {
        val viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "10")
        viewModel.toggleWeeklyPacing(true)
        viewModel.navigate(KakeiboDestination.WeeklyPacing)
        val week = KakeiboPeriod(YearMonth.of(2026, 6)).weeks(java.util.Locale.getDefault()).first()
        viewModel.updateWeeklyAmount(KakeiboCategory.Needs, week.weekStart, "11")
        viewModel.finishWeeklyPacing()
        assertEquals(KakeiboDestination.WeeklyPacing, viewModel.state.value.navigation.current)
        assertEquals("invalid_amount", viewModel.state.value.planError)
        viewModel.skipWeeklyPacing()
        assertEquals(KakeiboDestination.MonthlyPlan, viewModel.state.value.navigation.current)
        assertFalse(viewModel.state.value.draft.weeklyPacingEnabled)
        assertEquals("10", viewModel.state.value.draft.categoryAmounts[KakeiboCategory.Needs])
    }

    @Test
    fun malformedCategoryAmountCannotBeReviewedAsZero() = runTest {
        val repository = FakeKakeiboRepository()
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.beginPlan()
        viewModel.updateCategoryAmount(KakeiboCategory.Needs, "not-an-amount")

        viewModel.reviewPlan()

        assertEquals("invalid_amount", viewModel.state.value.planError)
        assertEquals(KakeiboDestination.MonthlyPlan, viewModel.state.value.navigation.current)
        assertEquals(null, repository.savedPlan)
    }

    @Test
    fun reflectionDraftSurvivesFailureAndSavedAnswersReloadUnchanged() = runTest {
        val repository = FakeKakeiboRepository().apply {
            reflectionWrite = KakeiboWrite.Failed(StorageError.UNAVAILABLE)
        }
        val viewModel = KakeiboViewModel(repository, profile, clock = { fixedClock })
        runCurrent()
        viewModel.openReflection()
        runCurrent()
        viewModel.updateReflectionField(KakeiboViewModel.ReflectionField.WENT_WELL, "I paused before buying.")
        viewModel.saveReflection()
        runCurrent()

        assertEquals("I paused before buying.", viewModel.state.value.whatWentWell)
        assertEquals("storage", viewModel.state.value.reflectionError)
        repository.reflectionWrite = KakeiboWrite.Saved
        viewModel.saveReflection()
        runCurrent()
        assertEquals("I paused before buying.", repository.reflection?.whatWentWell)

        viewModel.openReflection()
        runCurrent()
        assertEquals("I paused before buying.", viewModel.state.value.whatWentWell)
        assertEquals("", viewModel.state.value.whatToChange)
    }

    private class FakeKakeiboRepository : KakeiboRepository {
        var learningState = LearningPromptState.UNSEEN
        var savedPlan: MonthlyPlan? = null
        var planWrite: KakeiboWrite = KakeiboWrite.Saved
        var reflection: PeriodReflection? = null
        var reflectionWrite: KakeiboWrite = KakeiboWrite.Saved

        override suspend fun readPlan(month: YearMonth): KakeiboRead<MonthlyPlan> =
            savedPlan?.let { KakeiboRead.Found(it) } ?: KakeiboRead.Absent

        override suspend fun savePlan(plan: MonthlyPlan): KakeiboWrite {
            if (planWrite == KakeiboWrite.Saved) savedPlan = plan
            return planWrite
        }

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
        ): KakeiboWrite = KakeiboWrite.NotEligible

        override suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection> =
            reflection?.let { KakeiboRead.Found(it) } ?: KakeiboRead.Absent

        override suspend fun saveReflection(reflection: PeriodReflection): KakeiboWrite {
            if (reflectionWrite == KakeiboWrite.Saved) this.reflection = reflection
            return reflectionWrite
        }

        override suspend fun readLearningState(): KakeiboRead<LearningPromptState> =
            KakeiboRead.Found(learningState)

        override suspend fun saveLearningState(state: LearningPromptState): KakeiboWrite {
            if (learningState != LearningPromptState.UNSEEN && learningState != state) {
                return KakeiboWrite.InvalidDraft
            }
            learningState = state
            return KakeiboWrite.Saved
        }
    }
}
