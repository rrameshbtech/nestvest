package com.nestvest.expense

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import com.nestvest.expense.data.*
import com.nestvest.expense.domain.*
import com.nestvest.expense.reminder.ReminderScheduling
import com.nestvest.expense.ui.*
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.UUID
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ExpenseNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun historySettingsReturnsToOfflineCaptureAndSavesAttributedExpense() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "navigation-${UUID.randomUUID()}.db"
        val database = ProfileDatabase.open(context, name)
        val expenses = RoomExpenseRepository(database)
        val profile = PrimaryProfile(UUID.randomUUID(), "Mira", "USD")
        val scheduler = object : ReminderScheduling {
            override suspend fun reconcile(preference: ReminderPreference, force: Boolean) = Unit
        }
        lateinit var capture: ExpenseViewModel
        lateinit var reminders: ReminderViewModel
        var returnedHome = false
        compose.runOnUiThread {
            capture = ExpenseViewModel(expenses, profile, SavedStateHandle())
            reminders = ReminderViewModel(RoomReminderRepository(database), scheduler, { false }, { true })
        }
        try {
            compose.setContent {
                NestVestTheme {
                    ExpenseRoute(capture, expenses, reminders, onDashboardBack = { returnedHome = true })
                }
            }
            compose.onNodeWithContentDescription("NestVest, return to dashboard").performClick()
            compose.runOnIdle { assertTrue(returnedHome) }
            compose.onNodeWithText("Expenses").performClick()
            compose.onNodeWithContentDescription("Daily reminder").performClick()
            compose.onNodeWithText("Daily expense reminders").assertExists()
            compose.onNodeWithText("Back to expenses").performClick()
            compose.onNodeWithText("Add an expense").assertIsDisplayed().performClick()
            compose.onNodeWithText("1", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Save expense").performClick()
            compose.waitUntil(5000) { capture.state.value.saved && capture.state.value.expenses.size == 1 }
            val saved = database.expenseDao().receiptFiles()
            assertTrue(saved.isEmpty())
            assertEquals(1, capture.state.value.expenses.size)
            assertEquals(profile.profileId, capture.state.value.expenses.single().createdByProfileId)
            assertEquals(100L, capture.state.value.expenses.single().amountMinorUnits)
            assertFalse(reminders.state.value.preference.enabled)
        } finally {
            compose.runOnUiThread {
                capture.viewModelScope.cancel()
                reminders.viewModelScope.cancel()
            }
            database.close()
            context.deleteDatabase(name)
        }
    }
}
