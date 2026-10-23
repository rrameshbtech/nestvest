package com.nestvest.profile

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
class ProfileRecoveryTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun absentRecordRestoresDraftButNeverReplaysSave() = runTest {
        val originalHandle = draftHandle()
        val interrupted = FakeProfileRepository().apply { writeGate = CompletableDeferred() }
        val original = ProfileViewModel(interrupted, originalHandle)
        runCurrent()
        original.accept(ProfileIntent.Submit)
        runCurrent()
        val repo = FakeProfileRepository()
        val restored = ProfileViewModel(repo, copyHandle(originalHandle))
        runCurrent()
        assertEquals(ProfileStage.SETUP, restored.state.value.stage)
        assertEquals("Mira", restored.state.value.draft.name)
        assertEquals("INR", restored.state.value.draft.currencyCode)
        assertEquals(0, repo.writes)
        interrupted.writeGate!!.complete(Unit)
        runCurrent()
    }

    @Test fun committedProfileWinsRestoredDraftAndClearsIt() = runTest {
        val handle = draftHandle()
        val repo = FakeProfileRepository().apply { readResult = ProfileRead.Found(savedProfile) }
        val vm = ProfileViewModel(repo, handle)
        runCurrent()
        assertEquals(savedProfile.profileId, vm.state.value.profile?.profileId)
        assertEquals(ProfileStage.SAVED, vm.state.value.stage)
        assertTrue(handle.keys().isEmpty())
        assertEquals(0, repo.writes)
    }

    @Test fun reconciliationKeepsSetupHiddenAndReadRetryPreservesDraft() = runTest {
        val handle = draftHandle()
        val repo = FakeProfileRepository().apply {
            readGate = CompletableDeferred()
            readResult = ProfileRead.Failed(StorageError.UNAVAILABLE)
        }
        val vm = ProfileViewModel(repo, handle)
        runCurrent()
        assertEquals(ProfileStage.LOADING, vm.state.value.stage)
        repo.readGate!!.complete(Unit)
        runCurrent()
        assertEquals(ProfileStage.LOAD_ERROR, vm.state.value.stage)
        assertEquals("Mira", handle.get<String>("name"))
        repo.readResult = ProfileRead.Absent
        vm.accept(ProfileIntent.RetryLoad)
        runCurrent()
        assertEquals(ProfileStage.SETUP, vm.state.value.stage)
        assertEquals("Mira", vm.state.value.draft.name)
    }
}
