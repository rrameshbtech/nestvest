package com.nestvest.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.unit.*
import androidx.lifecycle.SavedStateHandle
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import com.nestvest.profile.ui.theme.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class ProfileAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactLargeTextRtlSetupKeepsRequiredActionsReachable() {
        compose.setContent {
            val viewModel = remember { ProfileViewModel(UiRepository(), SavedStateHandle()) }
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                NestVestTheme {
                    Box(Modifier.width(320.dp).height(520.dp)) {
                        ProfileRoute(viewModel)
                    }
                }
            }
        }
        compose.enableAccessibilityChecks()
        compose.onNodeWithTag("name").performScrollTo().performClick().performTextInput("Mira")
        compose.onNodeWithTag("name").assertTextContains("Mira")
        compose.onNodeWithText("Choose a currency").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Create my profile").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @Test fun errorProgressSavedAndAppearanceExposeAccessibleSemantics() {
        val state = mutableStateOf(ProfileUiState(stage = ProfileStage.SETUP, saveError = StorageError.UNAVAILABLE))
        compose.setContent { NestVestTheme { ProfileContent(state.value) {} } }
        compose.enableAccessibilityChecks()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive))
            .assertExists()
        compose.onNodeWithText("Retry").performScrollTo().assertHeightIsAtLeast(48.dp)
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.runOnIdle { state.value = ProfileUiState(stage = ProfileStage.SAVING) }
        compose.onNodeWithText("Create my profile").assertIsNotEnabled()
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)).assertExists()
        compose.runOnIdle { state.value = ProfileUiState(stage = ProfileStage.SAVED, profile = uiProfile) }
        compose.onNodeWithText("Saved on this device").assertExists()
        compose.onRoot().tryPerformAccessibilityChecks()
        compose.runOnIdle {
            state.value = ProfileUiState(stage = ProfileStage.SETUP,
                sheetDraft = AppearanceSelection(avatarId = "leaf"))
        }
        compose.onNode(hasText("Leaf") and hasClickAction()).performScrollTo().assertIsSelected()
        compose.onRoot().tryPerformAccessibilityChecks()
    }

    @Test fun coreThemePairsMeetNormalTextContrast() {
        fun ratio(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Double {
            fun linear(v: Float): Double =
                if (v <= .04045f) v / 12.92 else Math.pow((v + .055) / 1.055, 2.4)
            fun luminance(c: androidx.compose.ui.graphics.Color) =
                .2126 * linear(c.red) + .7152 * linear(c.green) + .0722 * linear(c.blue)
            val x = luminance(a)
            val y = luminance(b)
            return (maxOf(x, y) + .05) / (minOf(x, y) + .05)
        }
        assertTrue(ratio(Ink, Paper) >= 4.5)
        assertTrue(ratio(androidx.compose.ui.graphics.Color.White, Sage) >= 4.5)
        assertTrue(ratio(Ink, SoftSage) >= 4.5)
    }
}
