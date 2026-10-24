package com.nestvest.kakeibo

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.ui.KakeiboDestination
import com.nestvest.kakeibo.ui.KakeiboViewModel
import com.nestvest.kakeibo.ui.KakeiboWeeklyPacingScreen
import com.nestvest.profile.domain.PrimaryProfile
import java.time.Clock
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KakeiboWeeklyPlanUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun weeklyInputsUseRealClippedDatesAndRetainTypedValues() {
        val month = YearMonth.of(2026, 10)
        val clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC)
        val viewModel = KakeiboViewModel(
            KakeiboPlanUiTest.FakeKakeiboRepository(),
            PrimaryProfile(UUID.randomUUID(), "Mira", "USD"),
            clock = { clock },
        )
        viewModel.beginPlan()
        viewModel.toggleWeeklyPacing(true)
        viewModel.navigate(KakeiboDestination.WeeklyPacing)
        val firstWeek = KakeiboPeriod(month).weeks(Locale.getDefault()).first()
        val format = java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val label = "${firstWeek.weekStart.format(format)} – ${firstWeek.weekEndInclusive.format(format)}"

        compose.setContent {
            val state by viewModel.state.collectAsState()
            KakeiboWeeklyPacingScreen(state, viewModel)
        }

        compose.onNodeWithTag("kakeibo-weekly-Needs:${firstWeek.weekStart}")
            .performScrollTo().assertTextContains(label).performTextInput("12.50")
        compose.waitForIdle()

        assertEquals(
            "12.50",
            viewModel.state.value.draft.weeklyAmounts["Needs:${firstWeek.weekStart}"],
        )
    }
}
