package com.nestvest.expense

import com.nestvest.expense.domain.*
import com.nestvest.expense.reminder.ReminderScheduling
import com.nestvest.expense.ui.ReminderViewModel
import com.nestvest.profile.MainDispatcherRule
import com.nestvest.profile.domain.StorageError
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Fake : ReminderRepository {
        var value = ReminderPreference()
        val flow = MutableStateFlow<ReminderRead>(ReminderRead.Found(value))
        var fail = false
        override fun observe() = flow
        override suspend fun read() = ReminderRead.Found(value)
        override suspend fun update(enabled: Boolean, minutes: Int): ReminderWrite {
            if (fail) return ReminderWrite.Failed(StorageError.FULL)
            value = value.copy(enabled = enabled, localTimeMinutes = minutes)
            flow.value = ReminderRead.Found(value)
            return ReminderWrite.Saved
        }
        override suspend fun markPermissionPrompted(): ReminderWrite {
            if (fail) return ReminderWrite.Failed(StorageError.FULL)
            value = value.copy(permissionPrompted = true)
            flow.value = ReminderRead.Found(value)
            return ReminderWrite.Saved
        }
        override suspend fun claim(date: LocalDate) = ReminderClaim.Claimed
    }
    private val scheduler = object : ReminderScheduling {
        override suspend fun reconcile(preference: ReminderPreference, force: Boolean) = Unit
    }

    @Test fun explicitEnablePersistsPromptBeforeEventAndNeverReprompts() = runTest {
        val repo = Fake()
        val vm = ReminderViewModel(repo, scheduler, { false }, { true })
        runCurrent()
        assertFalse(vm.state.value.preference.enabled)
        vm.setEnabled(true)
        runCurrent()
        assertTrue(repo.value.permissionPrompted)
        assertTrue(vm.state.value.requestPermission)
        vm.permissionResult()
        runCurrent()
        assertFalse(vm.state.value.requestPermission)
        assertTrue(vm.state.value.unavailable)
        vm.setEnabled(false)
        runCurrent()
        assertFalse(repo.value.enabled)
        vm.setEnabled(true)
        runCurrent()
        assertFalse(vm.state.value.requestPermission)
    }

    @Test fun failureDoesNotLaunchPermissionOrPretendWriteSucceeded() = runTest {
        val repo = Fake().apply { fail = true }
        val vm = ReminderViewModel(repo, scheduler, { false }, { true })
        runCurrent()
        vm.setEnabled(true)
        runCurrent()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.requestPermission)
        assertFalse(repo.value.enabled)
    }

    @Test fun grantThenRevokeReconcilesWithoutPromptingAndTimeFailureRetainsPreference() = runTest {
        val repo = Fake()
        var available = false
        val vm = ReminderViewModel(repo, scheduler, { available }, { true })
        runCurrent()
        vm.setEnabled(true)
        runCurrent()
        available = true
        vm.permissionResult()
        runCurrent()
        assertFalse(vm.state.value.unavailable)
        vm.setTime(600)
        runCurrent()
        assertEquals(600, repo.value.localTimeMinutes)
        repo.fail = true
        vm.setTime(700)
        runCurrent()
        assertEquals(600, vm.state.value.preference.localTimeMinutes)
        assertNotNull(vm.state.value.error)
        available = false
        vm.refreshAvailability()
        runCurrent()
        assertTrue(vm.state.value.unavailable)
        assertFalse(vm.state.value.requestPermission)
        val recreated = ReminderViewModel(repo, scheduler, { false }, { true })
        runCurrent()
        assertFalse(recreated.state.value.requestPermission)
    }
}
