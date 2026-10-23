package com.nestvest.profile

import androidx.compose.ui.test.*
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.SavedStateHandle
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.*
import com.nestvest.profile.ui.theme.NestVestTheme
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class UiRepository : ProfileRepository {
    var readResult: ProfileRead = ProfileRead.Absent
    var writeResult: ProfileWrite = ProfileWrite.Failed(StorageError.UNAVAILABLE)
    var readGate: CompletableDeferred<Unit>? = null
    var writeGate: CompletableDeferred<Unit>? = null
    var writes = 0
    override suspend fun read(): ProfileRead { readGate?.await(); return readResult }
    override suspend fun create(draft: ProfileDraft): ProfileWrite {
        writes++
        writeGate?.await()
        return writeResult
    }
}

val uiProfile = PrimaryProfile(UUID.fromString("cb2fc29b-1154-42e2-8e50-60227249a7c7"), "Mira", "INR")

class ProfileSetupTest {
    @get:Rule val compose = createComposeRule()
    private fun show(repo: UiRepository, handle: SavedStateHandle = SavedStateHandle()) {
        compose.setContent {
            val vm = remember { ProfileViewModel(repo, handle) }
            NestVestTheme { ProfileRoute(vm) }
        }
    }

    @Test fun invalidFieldsShowGuidanceWithoutWriting() {
        val repo = UiRepository()
        show(repo)
        compose.onNodeWithText("Create my profile").performScrollTo().performClick()
        compose.onNodeWithText("Enter a name to continue.").assertExists()
        compose.onNodeWithText("Choose an available currency.").assertExists()
        compose.runOnIdle { assertEquals(0, repo.writes) }
    }

    @Test fun failedSaveRetainsRestoredValuesAndRetryOnlyShowsCommittedProfile() {
        val repo = UiRepository()
        show(repo, SavedStateHandle(mapOf("name" to "Mira", "currency" to "INR")))
        compose.onNodeWithText("Create my profile").performScrollTo().performClick()
        compose.onNodeWithText("Your profile was not created. Your details are still here. Please retry.").assertExists()
        compose.onNodeWithTag("name").assertTextContains("Mira")
        compose.runOnIdle { repo.writeResult = ProfileWrite.Created(uiProfile) }
        compose.onNodeWithText("Retry").performScrollTo().performClick()
        compose.onNodeWithText("Saved on this device").assertExists()
    }

    @Test fun loadingAndReadErrorNeverExposeNewSetup() {
        val repo = UiRepository().apply {
            readGate = CompletableDeferred()
            readResult = ProfileRead.Failed(StorageError.UNAVAILABLE)
        }
        show(repo, SavedStateHandle(mapOf("name" to "Mira", "currency" to "INR")))
        compose.onNodeWithText("Loading your local profile…").assertExists()
        compose.onNodeWithTag("name").assertDoesNotExist()
        compose.runOnIdle { repo.readGate!!.complete(Unit) }
        compose.onNodeWithText("Your profile could not be read. Retry before continuing.").assertExists()
        compose.onNodeWithTag("name").assertDoesNotExist()
        compose.runOnIdle { repo.readResult = ProfileRead.Found(uiProfile) }
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Saved on this device").assertExists()
    }

    @Test fun savingDisablesRepeatedSubmissionAndWaitsForConfirmation() {
        val repo = UiRepository().apply {
            writeResult = ProfileWrite.Created(uiProfile)
            writeGate = CompletableDeferred()
        }
        show(repo, SavedStateHandle(mapOf("name" to "Mira", "currency" to "INR")))
        compose.onNodeWithText("Create my profile").performScrollTo().performClick().assertIsNotEnabled()
        compose.onNodeWithText("Saved on this device").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, repo.writes)
            repo.writeGate!!.complete(Unit)
        }
        compose.onNodeWithText("Saved on this device").assertExists()
    }
}
