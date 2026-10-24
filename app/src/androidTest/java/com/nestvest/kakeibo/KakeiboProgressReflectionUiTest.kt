package com.nestvest.kakeibo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import com.nestvest.kakeibo.ui.KakeiboProgressScreen
import com.nestvest.kakeibo.ui.KakeiboReflectionScreen
import com.nestvest.kakeibo.ui.KakeiboDestination
import com.nestvest.kakeibo.ui.KakeiboViewModel
import com.nestvest.kakeibo.ui.KakeiboSavedReflectionScreen
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KakeiboProgressReflectionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun progressWithoutPlanIsExplicitAndReflectionCanBeSaved() {
        val repository = KakeiboPlanUiTest.FakeKakeiboRepository()
        lateinit var viewModel: KakeiboViewModel
        compose.runOnIdle { viewModel = KakeiboViewModel(
            repository,
            PrimaryProfile(UUID.randomUUID(), "Mira", "USD"),
            clock = { Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC) },
        ) }
        compose.setContent {
            val state by viewModel.state.collectAsState()
            if (state.navigation.current == KakeiboDestination.SavedReflection) {
                KakeiboSavedReflectionScreen(state, viewModel)
            } else if (state.navigation.current == KakeiboDestination.Reflection) {
                KakeiboReflectionScreen(state, viewModel)
            } else {
                KakeiboProgressScreen(state, viewModel)
            }
        }
        compose.waitUntil(5_000) {
            viewModel.state.value.plan != null && viewModel.state.value.expenses != null
        }

        compose.onNodeWithText("Recorded expenses are shown without a plan comparison.")
            .assertExists()
        compose.onNodeWithText("Plan this month").assertExists()

        compose.runOnIdle { viewModel.openReflection() }
        compose.waitUntil(5_000) { viewModel.state.value.reflection != null }
        compose.onNodeWithText("What did you notice?").assertExists()
        compose.onNodeWithContentDescription("What went well for you?").performScrollTo().performTextInput("I paused before buying.")
        compose.onNodeWithText("Save reflection").performScrollTo().performClick()
        compose.waitForIdle()

        assertEquals("I paused before buying.", viewModel.state.value.whatWentWell)
        assertEquals("I paused before buying.", repository.savedReflection?.whatWentWell)
        assertEquals(KakeiboDestination.SavedReflection, viewModel.state.value.navigation.current)
        compose.onNodeWithTag("kakeibo-saved-reflection").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("I paused before buying.").assertExists()
        compose.onNodeWithText("Edit reflection").performScrollTo().performClick()
        compose.onNodeWithContentDescription("What went well for you?").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Not now").performScrollTo().performClick()
        compose.runOnIdle { viewModel.openReflection() }
        compose.waitUntil(5_000) { viewModel.state.value.navigation.current == KakeiboDestination.SavedReflection }
        compose.onNodeWithText("Back to plan progress").performScrollTo().performClick()
        assertEquals(KakeiboDestination.Progress, viewModel.state.value.navigation.current)
    }
}
