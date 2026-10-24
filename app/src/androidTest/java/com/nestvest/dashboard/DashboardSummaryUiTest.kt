package com.nestvest.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithTag
import com.nestvest.dashboard.domain.DashboardDestination
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardSummary
import com.nestvest.dashboard.ui.DashboardScreen
import com.nestvest.dashboard.ui.DashboardUiState
import com.nestvest.profile.ui.theme.NestVestTheme
import java.math.BigInteger
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardSummaryUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun summaryStatesKeepExpensesDestinationActionable() {
        var state by mutableStateOf(
            DashboardUiState(
                pinnedOptionIds = DashboardPreference.DEFAULT.pinnedOptionIds,
                summary = DashboardSummary.Loading,
            ),
        )
        var opened: DashboardDestination? = null
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = state,
                    onOpenDestination = { opened = it },
                    onCustomize = {},
                    onToggle = {},
                    onSave = {},
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.onNodeWithText("Loading today’s summary…").assertIsDisplayed()
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, opened) }

        compose.runOnIdle { state = state.copy(summary = DashboardSummary.NoExpenseHistory) }
        compose.onNodeWithText("No expenses recorded yet.").assertIsDisplayed()
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, opened) }

        compose.runOnIdle {
            state = state.copy(
                summary = DashboardSummary.NoExpensesToday(LocalDate.of(2026, 10, 24), "USD"),
            )
        }
        compose.onNodeWithText("0 today · $0.00").assertIsDisplayed()
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, opened) }

        compose.runOnIdle {
            state = state.copy(
                summary = DashboardSummary.Available(
                    LocalDate.of(2026, 10, 24), 1, BigInteger.valueOf(1234), "USD",
                ),
            )
        }
        compose.onNodeWithText("1 today · $12.34").assertIsDisplayed()
        compose.onNodeWithTag("dashboard-icon-expense_history").assertIsDisplayed()
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, opened) }

        compose.runOnIdle { state = state.copy(summary = DashboardSummary.Unavailable) }
        compose.onNodeWithText("Summary unavailable").assertIsDisplayed()
        compose.onNodeWithText("Expenses").performClick()
        compose.runOnIdle { assertEquals(DashboardDestination.EXPENSE_HISTORY, opened) }
    }
}
