package com.nestvest.profile

import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import com.nestvest.profile.ui.theme.NestVestTheme
import org.junit.Rule
import org.junit.Test

class ProfileAppearanceSheetTest {
    @get:Rule val compose = createComposeRule()
    private fun show() {
        compose.setContent {
            val vm = remember { ProfileViewModel(UiRepository().apply {
                writeResult = ProfileWrite.Created(uiProfile)
            }, SavedStateHandle(mapOf("name" to "Mira", "currency" to "INR"))) }
            NestVestTheme { ProfileRoute(vm) }
        }
    }

    @Test fun sheetAppliesNamedSelectedChoicesThenAllowsSkipping() {
        show()
        compose.onNodeWithText("Choose a look (optional)").performScrollTo().performClick()
        compose.onNode(hasText("Leaf") and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("Use this look").performScrollTo().performClick()
        compose.onNodeWithText("Leaf").assertExists()
        compose.onNodeWithText("Choose a look (optional)").performScrollTo().performClick()
        compose.onNodeWithText("Keep it simple").performScrollTo().performClick()
        compose.onNodeWithText("Create my profile").performScrollTo().performClick()
        compose.onNodeWithText("Saved on this device").assertExists()
    }

    @Test fun creationNeverRequiresOpeningAppearance() {
        show()
        compose.onNodeWithText("Create my profile").performScrollTo().performClick()
        compose.onNodeWithText("Saved on this device").assertExists()
    }
}
