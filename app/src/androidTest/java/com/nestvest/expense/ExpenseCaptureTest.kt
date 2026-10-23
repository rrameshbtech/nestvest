package com.nestvest.expense

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import com.nestvest.expense.domain.*
import com.nestvest.expense.ui.ExpenseHistoryScreen
import com.nestvest.expense.ui.ExpenseCaptureScreen
import com.nestvest.expense.ui.ExpenseUiState
import com.nestvest.profile.ui.theme.NestVestTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ExpenseCaptureTest {
    @get:Rule val compose = createComposeRule()

    @Test fun amountOnlyActionsAndHistoryRemainReachable() {
        var amount = ""
        var saved = false
        compose.setContent {
            NestVestTheme {
                ExpenseCaptureScreen(ExpenseUiState(), "USD", { amount = it }, { saved = true }, {})
            }
        }
        compose.onNodeWithContentDescription("1").performClick()
        assertEquals("1", amount)
        compose.onNodeWithText("Save expense").performClick()
        assertTrue(saved)
        compose.onNodeWithText("Today").assertDoesNotExist()
        compose.onNodeWithText("Add details now").assertExists()
    }

    @Test fun historyErrorsOfferRetryAndCaptureAndSameAmountRowsSelectCorrectIdentity() {
        val first = Expense(UUID.randomUUID(), 100, "USD",
            Instant.parse("2026-10-23T09:00:00Z").toEpochMilli(), UUID.randomUUID(), vendor = "Morning Store")
        val second = first.copy(expenseId = UUID.randomUUID(), vendor = "Evening Store",
            capturedAtEpochMillis = Instant.parse("2026-10-23T18:00:00Z").toEpochMilli())
        val state = mutableStateOf(ExpenseUiState(loading = false, readError = true))
        var retried = false
        var captured = false
        var selected: UUID? = null
        compose.setContent {
            NestVestTheme {
                ExpenseHistoryScreen(state.value, { state.value = state.value.copy(period = it) },
                    { selected = it.expenseId }, { retried = true }, { captured = true }, "USD")
            }
        }
        compose.onNodeWithText("Retry").performClick()
        assertTrue(retried)
        compose.onNodeWithText("Add an expense").performClick()
        assertTrue(captured)
        compose.runOnUiThread {
            state.value = state.value.copy(readError = false, expenses = listOf(second, first))
        }
        compose.onNodeWithText("This week").performClick()
        compose.onNode(hasText("This week") and isSelectable()).assertIsSelected()
        compose.onNodeWithText("Morning Store").performScrollTo().performClick()
        assertEquals(first.expenseId, selected)
        compose.runOnUiThread { state.value = state.value.copy(expenses = emptyList()) }
        compose.onNodeWithText("Add an expense").assertExists()
    }
}
