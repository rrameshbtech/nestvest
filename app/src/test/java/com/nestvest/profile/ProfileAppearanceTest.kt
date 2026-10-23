package com.nestvest.profile

import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileAppearanceTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun applyAndSkipPreserveEssentialsAndSurviveFreshViewModel() = runTest {
        val handle = draftHandle()
        val vm = ProfileViewModel(FakeProfileRepository(), handle)
        runCurrent()
        vm.accept(ProfileIntent.OpenAppearance)
        vm.accept(ProfileIntent.AvatarChanged("leaf"))
        vm.accept(ProfileIntent.ColorChanged("sky"))
        vm.accept(ProfileIntent.ApplyAppearance)
        assertEquals("leaf", vm.state.value.draft.avatarId)
        assertEquals("sky", vm.state.value.draft.colorId)
        assertEquals("Mira", vm.state.value.draft.name)
        val restored = ProfileViewModel(FakeProfileRepository(), copyHandle(handle))
        runCurrent()
        assertEquals(vm.state.value.draft, restored.state.value.draft)
        vm.accept(ProfileIntent.OpenAppearance)
        vm.accept(ProfileIntent.SkipAppearance)
        val skipped = ProfileViewModel(FakeProfileRepository(), copyHandle(handle))
        runCurrent()
        assertNull(skipped.state.value.draft.avatarId)
        assertNull(skipped.state.value.draft.colorId)
        assertEquals("INR", skipped.state.value.draft.currencyCode)
    }

    @Test fun dismissAndTerminationDiscardUnappliedEdits() = runTest {
        val handle = draftHandle()
        val vm = ProfileViewModel(FakeProfileRepository(), handle)
        runCurrent()
        vm.accept(ProfileIntent.OpenAppearance)
        vm.accept(ProfileIntent.AvatarChanged("sun"))
        val restored = ProfileViewModel(FakeProfileRepository(), copyHandle(handle))
        runCurrent()
        assertNull(restored.state.value.draft.avatarId)
        assertNull(restored.state.value.sheetDraft)
        vm.accept(ProfileIntent.DismissAppearance)
        assertNull(vm.state.value.draft.avatarId)
    }

    @Test fun unavailableChoicesUseDefaultsWithFeedbackAndNeverBlockSave() = runTest {
        val repo = FakeProfileRepository()
        val handle = draftHandle().apply { set("avatar", "removed"); set("color", "missing") }
        val vm = ProfileViewModel(repo, handle)
        runCurrent()
        assertTrue(vm.state.value.appearanceWarning)
        assertNull(vm.state.value.draft.avatarId)
        assertNull(vm.state.value.draft.colorId)
        vm.accept(ProfileIntent.Submit)
        runCurrent()
        assertEquals(1, repo.writes)
    }

    @Test fun appearanceFieldsAreIndependentlyNullableAndRetainedOnFailure() = runTest {
        for ((avatar, color) in listOf("leaf" to null, null to "sage")) {
            val repo = FakeProfileRepository().apply { writeResult = ProfileWrite.Failed(StorageError.UNAVAILABLE) }
            val vm = ProfileViewModel(repo, draftHandle())
            runCurrent()
            vm.accept(ProfileIntent.OpenAppearance)
            vm.accept(ProfileIntent.AvatarChanged(avatar))
            vm.accept(ProfileIntent.ColorChanged(color))
            vm.accept(ProfileIntent.ApplyAppearance)
            vm.accept(ProfileIntent.Submit)
            runCurrent()
            assertEquals(avatar, vm.state.value.draft.avatarId)
            assertEquals(color, vm.state.value.draft.colorId)
        }
    }
}
