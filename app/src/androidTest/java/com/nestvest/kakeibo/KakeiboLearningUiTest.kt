package com.nestvest.kakeibo

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nestvest.kakeibo.ui.KakeiboLearningScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class KakeiboLearningUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun threeSlidesExplainCategoriesAndAllowSkippingToPlanning() {
        var startedPlanning = false
        compose.setContent {
            KakeiboLearningScreen(
                onBack = {},
                onPlan = { startedPlanning = true },
            )
        }

        compose.onNodeWithText("Make room for what matters").assertIsDisplayed()
        compose.onNodeWithText("Next").performScrollTo().performClick()
        compose.onNodeWithText("Give spending a clear place").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Needs").assertExists()
        compose.onNodeWithText("Groceries, rent, or a regular transit pass.").assertExists()
        compose.onNodeWithText("Wants").assertExists()
        compose.onNodeWithText("A treat, a new accessory, or a meal out.").assertExists()
        compose.onNodeWithText("Culture").assertExists()
        compose.onNodeWithText("A book, a museum visit, or a class.").assertExists()
        compose.onNodeWithText("Unexpected").assertExists()
        compose.onNodeWithText("A repair, a replacement, or an unplanned urgent cost.").assertExists()

        compose.onNodeWithText("Next").performScrollTo().performClick()
        compose.onNodeWithText("Plan, notice, reflect").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Skip learning and plan").performScrollTo().performClick()
        assertTrue(startedPlanning)
    }
}
