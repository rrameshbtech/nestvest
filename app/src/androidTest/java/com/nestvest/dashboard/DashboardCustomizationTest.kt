package com.nestvest.dashboard

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.nestvest.dashboard.data.RoomDashboardPreferenceRepository
import com.nestvest.dashboard.domain.DashboardPreference
import com.nestvest.dashboard.domain.DashboardPreferenceRead
import com.nestvest.dashboard.domain.DashboardPreferenceWrite
import com.nestvest.dashboard.ui.DashboardScreen
import com.nestvest.dashboard.ui.DashboardUiState
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardCustomizationTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun savedChoicesShowOnlyATransientSuccessMessage() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            NestVestTheme {
                DashboardScreen(
                    state = DashboardUiState(saveSucceeded = true),
                    onOpenDestination = {},
                    onCustomize = {},
                    onToggle = {},
                    onSave = {},
                    onCancel = {},
                    onRetryPreferences = {},
                )
            }
        }

        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("Dashboard saved.").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(5_000)
        compose.waitForIdle()
        compose.onNodeWithText("Dashboard saved.").assertDoesNotExist()
    }

    @Test fun pinChoicesPersistAcrossDatabaseReopen() = runBlocking {
        val name = "dashboard-preference-${UUID.randomUUID()}.db"
        val expected = DashboardPreference(setOf("reminders", "expense_capture"))
        val first = ProfileDatabase.open(context, name)
        try {
            val result = RoomDashboardPreferenceRepository(first.dashboardPreferenceDao()).save(expected)
            assertEquals(DashboardPreferenceWrite.Saved, result)
        } finally {
            first.close()
        }

        val reopened = ProfileDatabase.open(context, name)
        try {
            assertEquals(
                DashboardPreferenceRead.Found(expected),
                RoomDashboardPreferenceRepository(reopened.dashboardPreferenceDao()).observe().first(),
            )
        } finally {
            reopened.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun closedDatabaseReportsWriteFailureRatherThanSuccess() = runBlocking {
        val name = "dashboard-preference-failure-${UUID.randomUUID()}.db"
        val database = ProfileDatabase.open(context, name)
        val repository = RoomDashboardPreferenceRepository(database.dashboardPreferenceDao())
        database.close()

        try {
            val result = repository.save(DashboardPreference(setOf("reminders")))
            assertTrue(result is DashboardPreferenceWrite.Failed)
        } finally {
            context.deleteDatabase(name)
        }
    }
}
