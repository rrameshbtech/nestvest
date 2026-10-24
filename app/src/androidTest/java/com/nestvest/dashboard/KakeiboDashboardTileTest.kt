package com.nestvest.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nestvest.dashboard.domain.DashboardDestination
import com.nestvest.dashboard.ui.DashboardScreen
import com.nestvest.dashboard.ui.DashboardUiState
import com.nestvest.kakeibo.domain.DashboardKakeiboSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class KakeiboDashboardTileTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun kakeiboTileRemainsAvailableWithoutPinnedShortcutsAndNavigates() {
        var destination: DashboardDestination? = null
        compose.setContent {
            DashboardScreen(
                state = DashboardUiState(
                    pinnedOptionIds = emptySet(),
                    kakeiboSummary = DashboardKakeiboSummary.NoPlan,
                ),
                onOpenDestination = { destination = it },
                onCustomize = {},
                onToggle = {},
                onSave = {},
                onCancel = {},
                onRetryPreferences = {},
            )
        }

        compose.onNodeWithTag("dashboard-kakeibo-tile").assertIsDisplayed().performClick()
        compose.onNodeWithTag("dashboard-icon-kakeibo", useUnmergedTree = true).assertExists()
        val heading = compose.onNodeWithText("Pinned for you").fetchSemanticsNode().boundsInRoot
        val tile = compose.onNodeWithTag("dashboard-kakeibo-tile").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue("Kakeibo belongs in the shortcut section", tile.top >= heading.bottom)

        assertEquals(DashboardDestination.KAKEIBO, destination)
    }
}
