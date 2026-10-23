package com.nestvest.profile

import androidx.lifecycle.SavedStateHandle
import com.nestvest.profile.domain.*
import com.nestvest.profile.ui.ProfileViewModel
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    val dispatcher = StandardTestDispatcher()
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

val savedProfile = PrimaryProfile(UUID.fromString("cb2fc29b-1154-42e2-8e50-60227249a7c7"), "Mira", "INR")

class FakeProfileRepository : ProfileRepository {
    var readResult: ProfileRead = ProfileRead.Absent
    var writeResult: ProfileWrite = ProfileWrite.Created(savedProfile)
    var readGate: CompletableDeferred<Unit>? = null
    var writeGate: CompletableDeferred<Unit>? = null
    var writes = 0
    var lastDraft: ProfileDraft? = null
    override suspend fun read(): ProfileRead {
        readGate?.await()
        return readResult
    }
    override suspend fun create(draft: ProfileDraft): ProfileWrite {
        writes++
        lastDraft = draft
        writeGate?.await()
        return writeResult
    }
}

fun draftHandle() = SavedStateHandle(mapOf("name" to "Mira", "currency" to "INR"))
fun copyHandle(handle: SavedStateHandle) =
    SavedStateHandle(handle.keys().associateWith { handle.get<String>(it) })

fun ProfileViewModel.enterValidDraft() {
    accept(com.nestvest.profile.ui.ProfileIntent.NameChanged(" Mira "))
    accept(com.nestvest.profile.ui.ProfileIntent.CurrencyChanged("INR"))
}
