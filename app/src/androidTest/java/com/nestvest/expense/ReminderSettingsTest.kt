package com.nestvest.expense

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nestvest.expense.domain.*
import com.nestvest.expense.reminder.ReminderScheduling
import com.nestvest.expense.ui.ReminderSettingsScreen
import com.nestvest.expense.ui.ReminderViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ReminderSettingsTest {
    @get:Rule val compose = createComposeRule()
    private class Fake : ReminderRepository {
        val preference = MutableStateFlow<ReminderRead>(ReminderRead.Found(ReminderPreference()))
        override fun observe() = preference
        override suspend fun read() = preference.value
        override suspend fun update(enabled: Boolean, minutes: Int): ReminderWrite {
            val current = (preference.value as ReminderRead.Found).preference
            preference.value = ReminderRead.Found(current.copy(enabled = enabled, localTimeMinutes = minutes))
            return ReminderWrite.Saved
        }
        override suspend fun markPermissionPrompted() = ReminderWrite.Saved
        override suspend fun claim(date: LocalDate) = ReminderClaim.AlreadyClaimed
    }

    @Test fun accessibleOffSwitchEnablesAndDisablesWithoutBlockingBack() {
        val repository = Fake()
        val scheduler = object : ReminderScheduling {
            override suspend fun reconcile(preference: ReminderPreference, force: Boolean) = Unit
        }
        var back = false
        lateinit var vm: ReminderViewModel
        compose.runOnUiThread { vm = ReminderViewModel(repository, scheduler, { true }, { false }) }
        compose.setContent {
            MaterialTheme {
                ReminderSettingsScreen(vm, { fail("No permission needed") }, {}, { back = true })
            }
        }
        compose.onNode(isToggleable()).assertIsOff().performClick()
        compose.waitForIdle()
        compose.onNode(isToggleable()).assertIsOn().performClick()
        compose.waitForIdle()
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText("Remembering\nis easier gently.").assertIsDisplayed()
        compose.onNodeWithText("On this device").assertExists()
        val paper = compose.onNodeWithTag("expense-screen").captureToImage().toPixelMap()
        assertEquals(Color(0xFFFAFBF7), paper[1, paper.height / 2])
        compose.onNodeWithText("Back to expenses").performClick()
        assertTrue(back)
    }

    @Test fun mockupCardAndFixedFooterRemainUsableWithLargeTextAndTimeDialog() {
        val repository = Fake()
        val scheduler = object : ReminderScheduling {
            override suspend fun reconcile(preference: ReminderPreference, force: Boolean) = Unit
        }
        lateinit var vm: ReminderViewModel
        compose.runOnUiThread { vm = ReminderViewModel(repository, scheduler, { true }, { false }) }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { ReminderSettingsScreen(vm, {}, {}, {}) }
            }
        }
        compose.onNodeWithTag("reminder-settings-card").assertExists()
        compose.onNodeWithText("Back to expenses").assertIsDisplayed()
        val footer = compose.onNodeWithTag("expense-footer").fetchSemanticsNode().boundsInRoot
        val screen = compose.onNodeWithTag("expense-screen").fetchSemanticsNode().boundsInRoot
        assertEquals(screen.bottom, footer.bottom, 1f)
        compose.onNodeWithTag("reminder-time").performScrollTo().assertIsNotEnabled()
        compose.onNode(isToggleable()).performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("reminder-time").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Choose a local time").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Back to expenses").assertIsDisplayed()
        compose.onNodeWithTag("reminder-time").performClick()
        compose.onNodeWithText("Confirm").performClick()
        assertTrue((repository.preference.value as ReminderRead.Found).preference.enabled)
    }
}
