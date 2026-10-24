package com.nestvest.kakeibo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.canonicalKakeiboCategories
import com.nestvest.kakeibo.domain.formatAsCurrency
import com.nestvest.kakeibo.ui.KakeiboDestination
import com.nestvest.kakeibo.ui.KakeiboRoute
import com.nestvest.kakeibo.ui.KakeiboViewModel
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KakeiboFeatureUiTest {
    @get:Rule val compose = createComposeRule()
    private val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
    private val clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC)

    private fun start(repository: KakeiboPlanUiTest.FakeKakeiboRepository): KakeiboViewModel {
        lateinit var viewModel: KakeiboViewModel
        compose.runOnIdle { viewModel = KakeiboViewModel(repository, profile, clock = { clock }) }
        compose.setContent { KakeiboRoute(viewModel) {} }
        return viewModel
    }

    private fun expense(amount: Long, category: KakeiboCategory? = null, currency: String = "USD") =
        Expense(UUID.randomUUID(), amount, currency, clock.millis(), profile.profileId,
            vendor = "Corner shop", category = category)

    private fun plan() = MonthlyPlan(
        UUID.randomUUID(), YearMonth.of(2026, 6), "USD", null, 0, false,
        canonicalKakeiboCategories.associateWith { 1_000L }, emptyList(), clock.millis(), clock.millis(),
    )

    @Test
    fun homeLearningIsInlineAndDestinationTilesRemainAvailable() {
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            learningState = LearningPromptState.UNSEEN
        }
        val viewModel = start(repository)
        compose.onNodeWithText("A plan that feels like yours.").assertExists()
        compose.onNodeWithTag("kakeibo-learning-prompt").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        compose.onNodeWithTag("kakeibo-learning-prompt").assertDoesNotExist()
        compose.onNodeWithText("View spending progress").performScrollTo().performClick()
        assertEquals(KakeiboDestination.Progress, viewModel.state.value.navigation.current)
        compose.onNodeWithText("Back").performScrollTo().performClick()
        compose.onNodeWithText("Reflect on this month").performScrollTo().performClick()
        assertEquals(KakeiboDestination.Reflection, viewModel.state.value.navigation.current)
        assertEquals(LearningPromptState.DISMISSED, repository.learningState)
    }

    @Test
    fun progressShowsNegativeRemainingAndSeparateUnconvertedAmounts() {
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            savedPlan = plan()
            expenses.value = KakeiboRead.Found(listOf(
                expense(1_500, KakeiboCategory.Needs), expense(750), expense(2_000, currency = "EUR"),
            ))
        }
        val viewModel = start(repository)
        compose.runOnIdle { viewModel.openProgress() }
        compose.onNodeWithTag("kakeibo-progress-overview").performScrollTo().assertIsDisplayed()
        val row = compose.onNodeWithTag("kakeibo-progress-Needs")
        row.performScrollTo().assertContentDescriptionContains("Over plan", substring = true)
        row.assertContentDescriptionContains((-500L).formatAsCurrency("USD", Locale.getDefault()), substring = true)
        compose.onNodeWithTag("kakeibo-progress-exceptions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Other currency · EUR").assertExists()
        compose.onNodeWithText(2_000L.formatAsCurrency("EUR", Locale.getDefault())).assertExists()
    }

    @Test
    fun categorySelectionDoesNotWriteUntilConfirmedAndFailureRetainsExpense() {
        val first = expense(750)
        val second = expense(500).copy(vendor = "Book shop")
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            savedPlan = plan()
            expenses.value = KakeiboRead.Found(listOf(first, second))
            assignmentWrite = KakeiboWrite.Failed(StorageError.UNAVAILABLE)
        }
        val viewModel = start(repository)
        compose.runOnIdle {
            viewModel.openProgress()
            viewModel.openCategory(KakeiboCategory.Needs)
        }
        compose.onNodeWithText("Corner shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Book shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithTag("kakeibo-select-${first.expenseId}").assertIsOn()
        compose.onNodeWithTag("kakeibo-select-${second.expenseId}").assertIsOn()
        compose.onNodeWithText("Corner shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithTag("kakeibo-select-${first.expenseId}").assertIsOff()
        compose.onNodeWithTag("kakeibo-select-${second.expenseId}").assertIsOn()
        assertEquals(0, repository.assignmentCount)
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        assertEquals(emptySet<UUID>(), viewModel.state.value.assignmentExpenseIds)
        compose.onNodeWithText("Assign selected (0)").assertIsNotEnabled()
        compose.onNodeWithText("Corner shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Book shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Assign selected (2)").performScrollTo().performClick()
        compose.onNodeWithText("The selected expenses were not assigned.", substring = true).assertExists()
        assertEquals(1, repository.assignmentCount)
        assertEquals(setOf(first.expenseId, second.expenseId), viewModel.state.value.assignmentExpenseIds)
        compose.runOnIdle { repository.assignmentWrite = KakeiboWrite.Saved }
        compose.onNodeWithText("Assign selected (2)").performScrollTo().performClick()
        compose.waitUntil(5_000) { viewModel.state.value.assignmentSaved }
        compose.onNodeWithText("No eligible unassigned expenses for this month.").assertExists()
        assertEquals(2, repository.assignmentCount)
        assertEquals(listOf(KakeiboCategory.Needs, KakeiboCategory.Needs),
            (repository.expenses.value as KakeiboRead.Found).value.map { it.category })
    }

    @Test
    fun staleBatchClearsSelectionWithoutReportingSuccess() {
        val first = expense(250)
        val second = expense(500).copy(vendor = "Book shop")
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            savedPlan = plan()
            expenses.value = KakeiboRead.Found(listOf(first, second))
            assignmentWrite = KakeiboWrite.NotEligible
        }
        val viewModel = start(repository)
        compose.runOnIdle { viewModel.openCategory(KakeiboCategory.Needs) }
        compose.onNodeWithText("Corner shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Book shop", substring = true).performScrollTo().performClick()
        compose.onNodeWithText("Assign selected (2)").performScrollTo().performClick()
        compose.onNodeWithText("One or more selected expenses changed", substring = true).assertExists()
        compose.onNodeWithText("Assign selected (0)").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(emptySet<UUID>(), viewModel.state.value.assignmentExpenseIds)
            assertEquals(false, viewModel.state.value.assignmentSaved)
            assertEquals(listOf(first, second), (repository.expenses.value as KakeiboRead.Found).value)
        }
    }

    @Test
    fun reflectionFailureRetainsDraftAndSuccessfulSaveKeepsLiveComparison() {
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            savedPlan = plan()
            reflectionWrite = KakeiboWrite.Failed(StorageError.UNAVAILABLE)
        }
        val viewModel = start(repository)
        compose.runOnIdle { viewModel.openReflection() }
        compose.waitUntil(5_000) { viewModel.state.value.reflection != null }
        compose.onNodeWithContentDescription("What went well for you?").performScrollTo().performTextInput("I made room for books.")
        compose.onNodeWithText("Save reflection").performScrollTo().performClick()
        compose.onNodeWithText("Your reflection was not saved.", substring = true).assertExists()
        assertEquals(KakeiboDestination.Reflection, viewModel.state.value.navigation.current)
        compose.runOnIdle { repository.reflectionWrite = KakeiboWrite.Saved }
        compose.onNodeWithText("Save reflection").performScrollTo().performClick()
        compose.onNodeWithTag("kakeibo-saved-reflection").performScrollTo().assertIsDisplayed()
        compose.runOnIdle {
            repository.expenses.value = KakeiboRead.Found(listOf(expense(250, KakeiboCategory.Culture)))
        }
        compose.onNodeWithTag("kakeibo-reflection-comparison").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("I made room for books.").assertExists()
        compose.onAllNodesWithText("No answer added").assertCountEquals(2)
        compose.onNodeWithText(
            "${250L.formatAsCurrency("USD", Locale.getDefault())} of " +
                1_000L.formatAsCurrency("USD", Locale.getDefault()),
        ).assertExists()
        assertEquals("I made room for books.", repository.savedReflection?.whatWentWell)
    }

    @Test
    fun failedReflectionReadOffersRetryWithoutEnablingOverwrite() {
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository().apply {
            reflectionReadFailure = StorageError.UNAVAILABLE
        }
        val viewModel = start(repository)
        compose.runOnIdle { viewModel.openReflection() }
        compose.waitUntil(5_000) { viewModel.state.value.reflection is KakeiboRead.Failed }
        compose.onNodeWithText("Save reflection").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { repository.reflectionReadFailure = null }
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.waitUntil(5_000) { viewModel.state.value.reflection == KakeiboRead.Absent }
        compose.onNodeWithText("Save reflection").performScrollTo().assertIsEnabled()
    }
}
