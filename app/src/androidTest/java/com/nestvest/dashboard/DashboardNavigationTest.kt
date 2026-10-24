package com.nestvest.dashboard

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nestvest.dashboard.domain.DashboardDestination
import com.nestvest.dashboard.domain.DashboardOptionCatalog
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardSummary
import com.nestvest.dashboard.ui.DashboardScreen
import com.nestvest.dashboard.ui.DashboardUiState
import com.nestvest.profile.ui.theme.NestVestTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun groupsAvailableOptionsAndOpensEachDestination() {
        var destination: DashboardDestination? = null
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = DashboardUiState(
                        pinnedOptionIds = DashboardPreference.DEFAULT.pinnedOptionIds,
                        summary = DashboardSummary.NoExpenseHistory,
                    ),
                    welcomeName = "Mira",
                    onOpenDestination = { destination = it },
                    onCustomize = {},
                    onToggle = {},
                    onSave = {},
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.onNodeWithText("Pinned for you").assertIsDisplayed()
        compose.onNodeWithText("More").assertIsDisplayed()
        compose.onNodeWithText("Good to see you, Mira.").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-icon-expense_capture").assertIsDisplayed()
        val customizeTop = compose.onNodeWithText("Customize").fetchSemanticsNode().boundsInRoot.top
        val firstTileTop = compose.onNodeWithText("Add expense").fetchSemanticsNode().boundsInRoot.top
        assertTrue("Customize should appear before the first pinned tile", customizeTop < firstTileTop)
        compose.onNodeWithText("Add expense").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_CAPTURE, destination) }
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, destination) }
        compose.onNodeWithText("See all (1)").performClick()
        compose.onNodeWithText("Everything stays within reach, even when it is not pinned.").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-icon-reminders").assertIsDisplayed()
        compose.onNodeWithText("Reminders").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.REMINDERS, destination) }
    }

    @Test fun brandActionReturnsFromMoreScreenToDashboard() {
        var returnedHome = false
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = DashboardUiState(showingMore = true),
                    welcomeName = "Mira",
                    onOpenDestination = {},
                    onCustomize = {},
                    onHome = { returnedHome = true },
                    onToggle = {},
                    onSave = {},
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.onNodeWithText("More").assertIsDisplayed()
        compose.onNodeWithContentDescription("NestVest, return to dashboard").performClick()
        compose.runOnIdle { assertEquals(true, returnedHome) }
    }

    @Test fun hidesMoreSectionWhenEveryOptionIsPinned() {
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = DashboardUiState(pinnedOptionIds = DashboardOptionCatalog.ids),
                    onOpenDestination = {},
                    onCustomize = {},
                    onToggle = {},
                    onSave = {},
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.onNodeWithText("More").assertDoesNotExist()
        compose.onNodeWithText("See all").assertDoesNotExist()
    }
}
