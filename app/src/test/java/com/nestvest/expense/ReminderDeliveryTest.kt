package com.nestvest.expense

import com.nestvest.expense.domain.*
import com.nestvest.expense.reminder.ReminderDelivery
import com.nestvest.profile.domain.StorageError
import java.time.LocalDate
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ReminderDeliveryTest {
    private class Fake : ReminderRepository {
        var preference = ReminderPreference(enabled = true)
        var claimResult: ReminderClaim = ReminderClaim.Claimed
        val events = mutableListOf<String>()
        override suspend fun read() = ReminderRead.Found(preference)
        override fun observe() = flowOf(ReminderRead.Found(preference))
        override suspend fun update(enabled: Boolean, minutes: Int) = ReminderWrite.Saved
        override suspend fun markPermissionPrompted() = ReminderWrite.Saved
        override suspend fun claim(date: LocalDate): ReminderClaim {
            events += "claim"
            return claimResult
        }
    }

    @Test fun successorCommitsBeforeClaimAndPost() = runTest {
        val repo = Fake()
        ReminderDelivery(repo, { true }, { repo.events += "next" }, { repo.events += "post" })
            .deliver(1230)
        assertEquals(listOf("next", "claim", "post"), repo.events)
    }

    @Test fun duplicateDayAndClaimFailureStillScheduleTomorrow() = runTest {
        for (claim in listOf(ReminderClaim.AlreadyClaimed, ReminderClaim.Failed(StorageError.FULL))) {
            val repo = Fake().apply { claimResult = claim }
            val result = ReminderDelivery(repo, { true }, { repo.events += "next" },
                { fail("Must not post") }).deliver(1230)
            assertEquals(listOf("next", "claim"), repo.events)
            assertEquals(claim == ReminderClaim.AlreadyClaimed, result)
        }
    }

    @Test fun failedSuccessorNeverClaimsAndFailedPostLeavesSuccessor() = runTest {
        val repo = Fake()
        try {
            ReminderDelivery(repo, { true }, { error("Scheduling failed") }, {}).deliver(1230)
            fail("Expected scheduling failure")
        } catch (_: IllegalStateException) { assertTrue(repo.events.isEmpty()) }
        try {
            ReminderDelivery(repo, { true }, { repo.events += "next" },
                { error("Posting failed") }).deliver(1230)
            fail("Expected notification failure")
        } catch (_: IllegalStateException) {
            assertEquals(listOf("next", "claim"), repo.events)
        }
    }

    @Test fun disableRevokeAndStaleTimeNeverPost() = runTest {
        val repo = Fake()
        ReminderDelivery(repo, { false }, { fail("No work on revoke") }, {}).deliver(1230)
        repo.preference = repo.preference.copy(enabled = false)
        ReminderDelivery(repo, { true }, { fail("No work when disabled") }, {}).deliver(1230)
        assertTrue(repo.events.isEmpty())
        repo.preference = repo.preference.copy(enabled = true)
        ReminderDelivery(repo, { true }, { repo.events += "next" }, { fail("Stale time") })
            .deliver(1200)
        assertEquals(listOf("next"), repo.events)
    }

    @Test fun changedZoneReschedulesWithoutPostingAtStaleLocalTime() = runTest {
        val repo = Fake()
        ReminderDelivery(repo, { true }, { repo.events += "next" }, { fail("Stale zone") })
            .deliver(1230, "different-zone")
        assertEquals(listOf("next"), repo.events)
    }
}
