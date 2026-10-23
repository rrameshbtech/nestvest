package com.nestvest.expense

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.nestvest.expense.data.ReminderPreferenceEntity
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.ProfileEntity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun exportedSchemaKeepsExpenseUuidAsExplicitPrimaryKey() {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val schema = assets.open("com.nestvest.profile.data.ProfileDatabase/2.json")
            .bufferedReader().use { JSONObject(it.readText()) }
        val entities = schema.getJSONObject("database").getJSONArray("entities")
        val expense = (0 until entities.length())
            .map(entities::getJSONObject)
            .single { it.getString("tableName") == "expenses" }

        val primaryKey = expense.getJSONObject("primaryKey")
        assertFalse(primaryKey.getBoolean("autoGenerate"))
        assertEquals("expenseId", primaryKey.getJSONArray("columnNames").getString(0))
        assertTrue(
            expense.getJSONArray("indices").let { indices ->
                (0 until indices.length()).map(indices::getJSONObject).any {
                    it.getJSONArray("columnNames").getString(0) == "capturedAtEpochMillis"
                }
            },
        )
    }

    @Test fun versionOneProfileSurvivesMigrationAndSingletonGuardsRemainActive() = runBlocking {
        val name = "expense-migration-${UUID.randomUUID()}.db"
        val profile = ProfileEntity(
            profileId = "8d712d98-6119-40bd-ae18-91d599dc673e",
            name = "Mira",
            preferredCurrency = "INR",
            avatarId = "leaf",
            colorId = "sage",
        )
        createVersionOneFixture(name, profile)

        val db = ProfileDatabase.open(context, name)
        try {
            assertEquals(profile, db.profileDao().read())
            assertTrue(db.expenseDao().observeAll().first().isEmpty())

            db.reminderPreferenceDao().initialize()
            assertEquals(ReminderPreferenceEntity(), db.reminderPreferenceDao().read())

            try {
                db.profileDao().insert(profile.copy(profileId = UUID.randomUUID().toString(), primarySlot = 2))
                throw AssertionError("The migrated database must reject an invalid singleton slot")
            } catch (_: SQLiteConstraintException) {
                assertEquals(profile, db.profileDao().read())
            }

            try {
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE primary_profile SET primarySlot = 2 WHERE profileId = ?",
                    arrayOf(profile.profileId),
                )
                throw AssertionError("The migrated database must reject an invalid singleton update")
            } catch (_: SQLiteConstraintException) {
                assertEquals(profile, db.profileDao().read())
            }

            try {
                db.profileDao().insert(profile.copy(profileId = UUID.randomUUID().toString()))
                throw AssertionError("The migrated database must reject a second primary profile")
            } catch (_: SQLiteConstraintException) {
                assertEquals(profile, db.profileDao().read())
            }
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }

    private fun createVersionOneFixture(name: String, profile: ProfileEntity) {
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL(
                "CREATE TABLE `primary_profile` (`profileId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`preferredCurrency` TEXT NOT NULL, `avatarId` TEXT, `colorId` TEXT, " +
                    "`primarySlot` INTEGER NOT NULL, PRIMARY KEY(`profileId`))",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX `index_primary_profile_primarySlot` " +
                    "ON `primary_profile` (`primarySlot`)",
            )
            db.execSQL(
                "CREATE TRIGGER singleton_insert BEFORE INSERT ON primary_profile " +
                    "WHEN NEW.primarySlot != 1 BEGIN SELECT RAISE(ABORT, 'Invalid primary slot'); END",
            )
            db.execSQL(
                "CREATE TRIGGER singleton_update BEFORE UPDATE ON primary_profile " +
                    "WHEN NEW.primarySlot != 1 BEGIN SELECT RAISE(ABORT, 'Invalid primary slot'); END",
            )
            db.execSQL(
                "INSERT INTO primary_profile " +
                    "(profileId, name, preferredCurrency, avatarId, colorId, primarySlot) " +
                    "VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(
                    profile.profileId,
                    profile.name,
                    profile.preferredCurrency,
                    profile.avatarId,
                    profile.colorId,
                    profile.primarySlot,
                ),
            )
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf("06708a791512e096e119792148e95d0f"),
            )
            db.version = 1
        }
    }
}
