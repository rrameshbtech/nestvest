package com.nestvest.profile

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.nestvest.profile.data.*
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProfileSchemaTest {
    @Test fun exportedSchemaSeparatesUuidIdentityFromSingletonConstraint() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val schema = assets.open("com.nestvest.profile.data.ProfileDatabase/1.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val entity = schema.getJSONObject("database").getJSONArray("entities").getJSONObject(0)
        val primaryKey = entity.getJSONObject("primaryKey")
        assertFalse(primaryKey.getBoolean("autoGenerate"))
        assertEquals("profileId", primaryKey.getJSONArray("columnNames").getString(0))
        val index = entity.getJSONArray("indices").getJSONObject(0)
        assertTrue(index.getBoolean("unique"))
        assertEquals("primarySlot", index.getJSONArray("columnNames").getString(0))
    }

    @Test fun databaseRejectsExtraProfilesAndInvalidSingletonSlots() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "schema-${UUID.randomUUID()}.db"
        val db = ProfileDatabase.open(context, name)
        try {
            val first = ProfileEntity(UUID.randomUUID().toString(), "Mira", "INR", null, null)
            db.profileDao().insert(first)
            try {
                db.profileDao().insert(first.copy(profileId = UUID.randomUUID().toString()))
                fail("A second primary profile must be rejected")
            } catch (_: SQLiteConstraintException) {
                assertEquals(1, db.profileDao().count())
            }
            try {
                db.profileDao().insert(first.copy(profileId = UUID.randomUUID().toString(), primarySlot = 2))
                fail("An invalid singleton slot must be rejected")
            } catch (_: SQLiteConstraintException) {
                assertEquals(1, db.profileDao().count())
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
