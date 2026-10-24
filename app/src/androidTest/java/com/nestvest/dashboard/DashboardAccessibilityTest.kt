package com.nestvest.dashboard

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.ui.DashboardScreen
import com.nestvest.dashboard.ui.DashboardUiState
import com.nestvest.profile.ui.theme.NestVestTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class DashboardAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test fun customizationActionsHavePurposefulLabelsAndPinnedStatus() {
        var toggledOption: String? = null
        var saved = false
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = DashboardUiState(
                        customizing = true,
                        pinnedOptionIds = DashboardPreference.DEFAULT.pinnedOptionIds,
                        draftOptionIds = DashboardPreference.DEFAULT.pinnedOptionIds,
                    ),
                    onOpenDestination = {},
                    onCustomize = {},
                    onToggle = { toggledOption = it.id },
                    onSave = { saved = true },
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.onNodeWithText("Pin your favorites.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Unpin Add expense").assertIsDisplayed()
        compose.onAllNodesWithText("Pinned on dashboard").assertCountEquals(2)
        compose.onNodeWithContentDescription("Pin Reminders").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals("reminders", toggledOption) }
        compose.onNodeWithText("Save choices").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Save choices").performClick()
        compose.runOnIdle { assertTrue(saved) }
        compose.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @Test fun appDoesNotRequestInternetPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val permissions = context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
        assertFalse("Dashboard app must remain offline", permissions.contains("android.permission.INTERNET"))
    }
}
