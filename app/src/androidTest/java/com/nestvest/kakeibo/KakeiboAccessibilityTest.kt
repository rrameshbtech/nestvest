package com.nestvest.kakeibo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.ui.DailySpendingChart
import com.nestvest.kakeibo.ui.KakeiboLearningScreen
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KakeiboAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dailyChartExposesPerDayTextualDataToAssistiveTechnology() {
        compose.setContent {
            DailySpendingChart(
                values = listOf(0L, 250L, 0L),
                firstDate = LocalDate.of(2026, 6, 1),
                category = KakeiboCategory.Needs.name,
                currencyCode = "USD",
            )
        }

        val contentDescription = compose.onNodeWithTag("kakeibo-daily-chart")
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription]
            .joinToString(" ")
        assertTrue(contentDescription.contains("Needs"))
        assertTrue(contentDescription.contains("3 days"))
        assertTrue(contentDescription.contains("$2.50"))
    }

    @Test
    fun largeTextOnCompactScreenKeepsPlanningActionReachableWithMinimumTarget() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                Box(Modifier.width(320.dp).height(480.dp)) {
                    KakeiboLearningScreen(onBack = {}, onPlan = {})
                }
            }
        }

        compose.onNodeWithText("Skip learning and plan")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
    }
}
