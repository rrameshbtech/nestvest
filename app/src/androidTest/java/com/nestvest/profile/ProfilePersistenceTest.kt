package com.nestvest.profile

import androidx.test.core.app.ApplicationProvider
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.DatabaseKeyStore
import com.nestvest.profile.data.ProfileEntity
import com.nestvest.profile.data.RoomProfileRepository
import com.nestvest.profile.domain.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import android.content.Context

class ProfilePersistenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun unreadableSchemaReturnsFailureWithoutDestructiveRecreation() = runBlocking {
        val name = "unreadable-${UUID.randomUUID()}.db"
        var db = ProfileDatabase.open(context, name)
        try {
            RoomProfileRepository(db).create(ProfileDraft("Mira", "INR"))
            db.openHelper.writableDatabase.let { it.version = it.version + 1 }
            db.close()
            db = ProfileDatabase.open(context, name)
            val repo = RoomProfileRepository(db)
            assertEquals(ProfileRead.Failed(StorageError.INVALID_RECORD), repo.read())
            assertEquals(ProfileWrite.Failed(StorageError.INVALID_RECORD), repo.create(ProfileDraft("Other", "USD")))
            net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                context.getDatabasePath(name).path,
                DatabaseKeyStore(context).getOrCreatePassphrase(),
                null,
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                null,
            ).use { raw ->
                raw.rawQuery("SELECT name FROM primary_profile", null).use {
                    assertTrue(it.moveToFirst())
                    assertEquals("Mira", it.getString(0))
                }
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun invalidStoredValuesAreReadErrorsNotAnAbsentProfile() = runBlocking {
        val name = "invalid-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            db.profileDao().insert(ProfileEntity("not-a-uuid", "Mira", "INR", null, null))
            assertEquals(ProfileRead.Failed(StorageError.INVALID_RECORD), RoomProfileRepository(db).read())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun selectedAndDefaultAppearanceSurviveReopen() = runBlocking {
        for ((avatar, color) in listOf("leaf" to "sky", null to null, "sun" to null, null to "sage")) {
            val name = "appearance-${UUID.randomUUID()}.db"
            var db = ProfileDatabase.open(context, name)
            try {
                RoomProfileRepository(db).create(ProfileDraft("Mira", "INR", avatar, color))
                db.close()
                db = ProfileDatabase.open(context, name)
                val profile = (RoomProfileRepository(db).read() as ProfileRead.Found).profile
                assertEquals(avatar, profile.avatarId)
                assertEquals(color, profile.colorId)
            } finally {
                db.close()
                context.deleteDatabase(name)
            }
        }
    }

    @Test fun fileBackedProfileSurvivesCloseReopen() = runBlocking {
        val name = "persistence-${UUID.randomUUID()}.db"
        var db = ProfileDatabase.open(context, name)
        try {
            val created = RoomProfileRepository(db).create(ProfileDraft("Mira", "INR")) as ProfileWrite.Created
            db.close()
            db = ProfileDatabase.open(context, name)
            assertEquals(ProfileRead.Found(created.profile), RoomProfileRepository(db).read())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun concurrentAndRepeatedCreationKeepsExactlyOneIdentity() = runBlocking {
        val name = "concurrent-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val repo = RoomProfileRepository(db)
            val results = (1..12).map { async { repo.create(ProfileDraft("Mira", "INR")) } }.awaitAll()
            assertEquals(1, results.count { it is ProfileWrite.Created })
            assertEquals(11, results.count { it is ProfileWrite.Existing })
            assertEquals(1, db.profileDao().count())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    @Test fun rejectedWriteDoesNotExposeACompletedRecord() = runBlocking {
        val name = "failure-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER reject_test BEFORE INSERT ON primary_profile BEGIN SELECT RAISE(ABORT, 'test'); END",
            )
            val repo = RoomProfileRepository(db)
            assertTrue(repo.create(ProfileDraft("Mira", "INR")) is ProfileWrite.Failed)
            assertEquals(ProfileRead.Absent, repo.read())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
