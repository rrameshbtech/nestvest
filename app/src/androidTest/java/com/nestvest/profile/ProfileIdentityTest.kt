package com.nestvest.profile

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestvest.profile.data.*
import com.nestvest.profile.domain.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ProfileIdentityTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun independentInstallationsWithEqualNamesHaveDifferentIds() = runBlocking {
        val ids = (1..3).map {
            val name = "identity-${UUID.randomUUID()}.db"
            val db = ProfileDatabase.open(context, name)
            try {
                val result = RoomProfileRepository(db).create(ProfileDraft("Mira", "INR")) as ProfileWrite.Created
                result.profile.profileId
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
        assertEquals(3, ids.toSet().size)
    }

    @Test fun restoredFixtureRetainsUuidAcrossReadsAndReopens() = runBlocking {
        val name = "restore-${UUID.randomUUID()}.db"
        val id = "78a18d43-a375-48e3-8cf2-e0e4d2aa4689"
        var db = ProfileDatabase.open(context, name)
        try {
            db.profileDao().insert(ProfileEntity(id, "Restored", "USD", "sun", "sky"))
            repeat(3) {
                val repo = RoomProfileRepository(db)
                assertEquals(UUID.fromString(id), (repo.read() as ProfileRead.Found).profile.profileId)
                val result = repo.create(ProfileDraft("Changed", "INR")) as ProfileWrite.Existing
                assertEquals(UUID.fromString(id), result.profile.profileId)
                assertEquals("Restored", result.profile.name)
                db.close()
                db = ProfileDatabase.open(context, name)
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
