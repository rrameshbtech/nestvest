package com.nestvest.profile

import androidx.lifecycle.SavedStateHandle
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun invalidFieldsNeverWrite() = runTest {
        val repo = FakeProfileRepository()
        val vm = ProfileViewModel(repo, SavedStateHandle())
        runCurrent()
        for (currency in listOf("", "INVALID")) {
            vm.accept(ProfileIntent.NameChanged("  "))
            vm.accept(ProfileIntent.CurrencyChanged(currency))
            vm.accept(ProfileIntent.Submit)
            runCurrent()
            assertTrue(vm.state.value.nameError)
            assertTrue(vm.state.value.currencyError)
            assertEquals(0, repo.writes)
        }
    }

    @Test fun minimalSaveWaitsForCommitAndBlocksDuplicates() = runTest {
        val repo = FakeProfileRepository().apply { writeGate = CompletableDeferred() }
        val handle = SavedStateHandle()
        val vm = ProfileViewModel(repo, handle)
        runCurrent()
        vm.enterValidDraft()
        vm.accept(ProfileIntent.Submit)
        vm.accept(ProfileIntent.Submit)
        runCurrent()
        assertEquals(ProfileStage.SAVING, vm.state.value.stage)
        assertNull(vm.state.value.profile)
        assertEquals(1, repo.writes)
        assertEquals("Mira", repo.lastDraft?.name)
        repo.writeGate!!.complete(Unit)
        runCurrent()
        assertEquals(savedProfile, vm.state.value.profile)
        assertEquals(ProfileStage.SAVED, vm.state.value.stage)
        assertTrue(handle.keys().isEmpty())
    }

    @Test fun everySaveFailureRetainsValuesAndAllowsExplicitRetry() = runTest {
        for (error in StorageError.entries) {
            val repo = FakeProfileRepository().apply { writeResult = ProfileWrite.Failed(error) }
            val vm = ProfileViewModel(repo, SavedStateHandle())
            runCurrent()
            vm.enterValidDraft()
            vm.accept(ProfileIntent.Submit)
            runCurrent()
            assertEquals(ProfileStage.SETUP, vm.state.value.stage)
            assertEquals(error, vm.state.value.saveError)
            assertEquals(" Mira ", vm.state.value.draft.name)
            assertEquals("INR", vm.state.value.draft.currencyCode)
            assertNull(vm.state.value.profile)
            repo.writeResult = ProfileWrite.Created(savedProfile)
            vm.accept(ProfileIntent.Submit)
            runCurrent()
            assertEquals(ProfileStage.SAVED, vm.state.value.stage)
        }
    }

    @Test fun readErrorDoesNotAllowCreatingASecondProfile() = runTest {
        val repo = FakeProfileRepository().apply { readResult = ProfileRead.Failed(StorageError.UNAVAILABLE) }
        val vm = ProfileViewModel(repo, SavedStateHandle())
        runCurrent()
        assertEquals(ProfileStage.LOAD_ERROR, vm.state.value.stage)
        vm.enterValidDraft()
        vm.accept(ProfileIntent.Submit)
        runCurrent()
        assertEquals(0, repo.writes)
        repo.readResult = ProfileRead.Found(savedProfile)
        vm.accept(ProfileIntent.RetryLoad)
        runCurrent()
        assertEquals(ProfileStage.SAVED, vm.state.value.stage)
    }

    @Test fun existingRecordWinsAnAtomicCreationRace() = runTest {
        val repo = FakeProfileRepository().apply { writeResult = ProfileWrite.Existing(savedProfile) }
        val vm = ProfileViewModel(repo, draftHandle())
        runCurrent()
        vm.accept(ProfileIntent.Submit)
        runCurrent()
        assertEquals(savedProfile, vm.state.value.profile)
    }
}
