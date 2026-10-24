package com.nestvest.kakeibo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.EpochRange
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.PeriodReflection
import com.nestvest.kakeibo.ui.KakeiboDestination
import com.nestvest.kakeibo.ui.KakeiboMonthlyPlanScreen
import com.nestvest.kakeibo.ui.KakeiboPlanReviewScreen
import com.nestvest.kakeibo.ui.KakeiboViewModel
import com.nestvest.kakeibo.ui.KakeiboRoute
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class KakeiboPlanUiTest {
    @get:Rule val compose = createComposeRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
    private val clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun exactInputAndSliderUpdateThePlanDraftAndReviewCanBeCancelled() {
        lateinit var viewModel: KakeiboViewModel
        compose.runOnIdle {
            viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { clock })
            viewModel.beginPlan()
        }
        compose.setContent {
            val state by viewModel.state.collectAsState()
            if (state.navigation.current == KakeiboDestination.PlanReview) {
                KakeiboPlanReviewScreen(state, viewModel)
            } else {
                KakeiboMonthlyPlanScreen(state, viewModel)
            }
        }

        compose.onNodeWithText("Overall amount (calculated)").assertExists()
        compose.onNodeWithContentDescription("Needs planned amount slider").assertIsNotEnabled()
        compose.onNodeWithText("Amount for Needs").performScrollTo().performTextReplacement("12.34")
        compose.waitForIdle()
        assertEquals("12.34", viewModel.state.value.draft.categoryAmounts[KakeiboCategory.Needs])

        compose.onNodeWithContentDescription("Needs planned amount slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress -> setProgress(0.5f) }
        compose.waitForIdle()
        assertNotEquals("12.34", viewModel.state.value.draft.categoryAmounts[KakeiboCategory.Needs])

        compose.onNodeWithText("Review plan").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(KakeiboDestination.PlanReview, viewModel.state.value.navigation.current)
        compose.onNodeWithTag("kakeibo-review-totals").performScrollTo().assertExists()
        compose.onNodeWithTag("kakeibo-review-categories").performScrollTo().assertExists()
        compose.onNodeWithText("USD").assertExists()
        val formattedNeeds = java.text.NumberFormat.getCurrencyInstance().apply {
            currency = java.util.Currency.getInstance("USD")
        }.format(java.math.BigDecimal.valueOf(viewModel.categoryAmountMinorUnits(KakeiboCategory.Needs)!!, 2))
        compose.onNode(
            hasText(formattedNeeds) and hasAnyAncestor(hasTestTag("kakeibo-review-categories")),
        ).assertExists()
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(KakeiboDestination.MonthlyPlan, viewModel.state.value.navigation.current)
    }

    @Test
    fun calculatedOverallCanBeEditedAndSliderUsesTheExplicitCap() {
        lateinit var viewModel: KakeiboViewModel
        compose.runOnIdle {
            viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { clock })
            viewModel.beginPlan()
            viewModel.updateCategoryAmount(KakeiboCategory.Needs, "75")
            viewModel.updateSavingsAmount("25")
        }
        compose.setContent {
            val state by viewModel.state.collectAsState()
            KakeiboMonthlyPlanScreen(state, viewModel)
        }
        compose.onNodeWithText("Overall amount (calculated)")
            .assertTextContains("100").assert(hasSetTextAction().not())
        compose.onNodeWithText("Edit overall amount").performScrollTo().performClick()
        compose.onNodeWithText("Total available to plan (optional)")
            .performScrollTo().performTextReplacement("200")
        compose.onNodeWithContentDescription("Needs planned amount slider")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.runOnIdle {
            assertEquals(10_000L, viewModel.categoryAmountMinorUnits(KakeiboCategory.Needs))
            assertEquals(20_000L, viewModel.sliderMaximumMinorUnits())
        }
        compose.onNodeWithText("Use calculated amount").performScrollTo().performClick()
        compose.onNodeWithText("Overall amount (calculated)").assertTextContains("125")
        compose.runOnIdle { assertEquals("", viewModel.state.value.draft.availableAmount) }
    }

    @Test
    fun homeHasNoRedundantBackAndBrandReturnsToDashboard() {
        lateinit var viewModel: KakeiboViewModel
        var exited = false
        compose.runOnIdle {
            viewModel = KakeiboViewModel(FakeKakeiboRepository(), profile, clock = { clock })
        }
        compose.setContent { KakeiboRoute(viewModel) { exited = true } }
        compose.onNodeWithText("Kakeibo planner").assertExists()
        compose.onNodeWithText("Back").assertDoesNotExist()
        compose.onNodeWithContentDescription("NestVest, return to dashboard").performClick()
        compose.runOnIdle { assertEquals(true, exited) }
    }

    internal class FakeKakeiboRepository : KakeiboRepository {
        var savedReflection: PeriodReflection? = null
        var savedPlan: MonthlyPlan? = null
        var learningState = LearningPromptState.VIEWED
        var reflectionReadFailure: com.nestvest.profile.domain.StorageError? = null
        var reflectionWrite: KakeiboWrite = KakeiboWrite.Saved
        var assignmentWrite: KakeiboWrite = KakeiboWrite.Saved
        var assignmentCount = 0
        val expenses = kotlinx.coroutines.flow.MutableStateFlow<KakeiboRead<List<Expense>>>(
            KakeiboRead.Found(emptyList()),
        )

        override suspend fun readPlan(month: YearMonth): KakeiboRead<MonthlyPlan> =
            savedPlan?.takeIf { it.month == month }?.let { KakeiboRead.Found(it) } ?: KakeiboRead.Absent
        override suspend fun savePlan(plan: MonthlyPlan): KakeiboWrite {
            savedPlan = plan
            return KakeiboWrite.Saved
        }
        override fun observeExpenses(period: EpochRange): Flow<KakeiboRead<List<Expense>>> = expenses
        override suspend fun unassignedExpenses(
            period: EpochRange,
            currencyCode: String,
        ): KakeiboRead<List<Expense>> = KakeiboRead.Found(emptyList())
        override suspend fun assignExpenses(
            expenseIds: Set<UUID>,
            category: KakeiboCategory,
            month: YearMonth,
            currencyCode: String,
        ): KakeiboWrite {
            assignmentCount++
            if (assignmentWrite == KakeiboWrite.Saved) {
                val current = (expenses.value as? KakeiboRead.Found)?.value.orEmpty()
                expenses.value = KakeiboRead.Found(current.map {
                    if (it.expenseId in expenseIds) it.copy(category = category) else it
                })
            }
            return assignmentWrite
        }
        override suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection> =
            reflectionReadFailure?.let { KakeiboRead.Failed(it) }
                ?: savedReflection?.takeIf { it.month == month }?.let { KakeiboRead.Found(it) }
                ?: KakeiboRead.Absent
        override suspend fun saveReflection(reflection: PeriodReflection): KakeiboWrite {
            if (reflectionWrite == KakeiboWrite.Saved) savedReflection = reflection
            return reflectionWrite
        }
        override suspend fun readLearningState(): KakeiboRead<LearningPromptState> =
            KakeiboRead.Found(learningState)
        override suspend fun saveLearningState(state: LearningPromptState): KakeiboWrite {
            learningState = state
            return KakeiboWrite.Saved
        }
    }
}
