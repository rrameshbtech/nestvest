package com.nestvest.kakeibo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import androidx.test.core.app.ApplicationProvider
import com.nestvest.dashboard.data.DashboardPreferenceEntity
import com.nestvest.expense.data.ReminderPreferenceEntity
import com.nestvest.kakeibo.data.RoomKakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.profile.data.EncryptedDatabaseMigration
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.ProfileEntity
import java.io.File
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class KakeiboMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun plaintextSchemaThreeDataSurvivesEncryptionAndKakeiboPersistsAfterReopen() = runBlocking {
        val databaseName = "kakeibo-migration-${UUID.randomUUID()}.db"
        val profileId = UUID.randomUUID().toString()
        val expenseId = UUID.randomUUID().toString()
        val profile = ProfileEntity(profileId, "Mira", "USD", "leaf", "sage")
        createVersionThreeFixture(databaseName, profile, expenseId)

        val firstOpen = ProfileDatabase.open(context, databaseName)
        try {
            assertEquals(profile, firstOpen.profileDao().read())
            assertEquals(expenseId, firstOpen.expenseDao().observeAll().first().single().expenseId)
            assertEquals(
                ReminderPreferenceEntity(
                    slot = 1,
                    enabled = true,
                    localTimeMinutes = 480,
                    permissionPrompted = true,
                    lastDeliveredLocalDate = "2026-06-12",
                ),
                firstOpen.reminderPreferenceDao().read(),
            )
            assertEquals(
                DashboardPreferenceEntity(pinnedOptionIds = "expense_history,expense_capture"),
                firstOpen.dashboardPreferenceDao().observe().first(),
            )

            val month = YearMonth.of(2026, 6)
            val plan = MonthlyPlan(
                planId = UUID.randomUUID(),
                month = month,
                currencyCode = "USD",
                availableMinorUnits = null,
                savingsIntentionMinorUnits = 0,
                weeklyPacingEnabled = false,
                categoryAmounts = KakeiboCategory.entries.associateWith { 10_000L },
                weeklyAllocations = emptyList(),
                createdAtEpochMillis = 1,
                updatedAtEpochMillis = 1,
            )
            assertEquals(KakeiboWrite.Saved, RoomKakeiboRepository(firstOpen).savePlan(plan))
        } finally {
            firstOpen.close()
        }

        val encryptedFile = context.getDatabasePath(databaseName)
        assertFalse(hasPlaintextHeader(encryptedFile))
        assertFalse(File("${encryptedFile.absolutePath}.encrypted.tmp").exists())
        assertFalse(File("${encryptedFile.absolutePath}.plaintext-backup").exists())
        try {
            SQLiteDatabase.openDatabase(
                encryptedFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY,
            ).use { database -> database.rawQuery("SELECT * FROM primary_profile", null).use { it.count } }
            throw AssertionError("The encrypted database must not open as plaintext SQLite")
        } catch (_: SQLiteException) {
            // Expected: SQLCipher has replaced the plaintext SQLite file.
        }

        val reopened = ProfileDatabase.open(context, databaseName)
        try {
            val saved = RoomKakeiboRepository(reopened).readPlan(YearMonth.of(2026, 6))
            assertTrue(saved is KakeiboRead.Found)
            assertEquals(4, (saved as KakeiboRead.Found).value.categoryAmounts.size)
            assertEquals(profile, reopened.profileDao().read())
        } finally {
            reopened.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun failedPlaintextConversionKeepsTheOriginalFileAndDoesNotOpenItInRoom() {
        val databaseName = "kakeibo-failed-conversion-${UUID.randomUUID()}.db"
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        val malformedPlaintext = SQLITE_HEADER + "unreadable".toByteArray()
        file.writeBytes(malformedPlaintext)

        try {
            ProfileDatabase.open(context, databaseName)
            throw AssertionError("A failed conversion must not fall back to opening the plaintext database")
        } catch (_: Exception) {
            assertTrue(file.exists())
            assertTrue(hasPlaintextHeader(file))
            assertTrue(file.readBytes().contentEquals(malformedPlaintext))
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun verificationWithWrongKeyPreservesEncryptedDatabaseAndAllowsReopen() = runBlocking {
        val databaseName = "kakeibo-wrong-key-${UUID.randomUUID()}.db"
        val profile = ProfileEntity(UUID.randomUUID().toString(), "Mira", "USD", "leaf", "sage")
        createVersionThreeFixture(databaseName, profile, UUID.randomUUID().toString())
        val database = ProfileDatabase.open(context, databaseName)
        try {
            assertEquals(profile, database.profileDao().read())
        } finally {
            database.close()
        }
        val file = context.getDatabasePath(databaseName)
        val encryptedBytes = file.readBytes()

        try {
            assertThrows(IllegalStateException::class.java) {
                EncryptedDatabaseMigration(file, "incorrect-passphrase".toByteArray()).migrateIfNeeded()
            }
            assertTrue(file.exists())
            assertTrue(file.readBytes().contentEquals(encryptedBytes))
            val reopened = ProfileDatabase.open(context, databaseName)
            try {
                assertEquals(profile, reopened.profileDao().read())
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }

    private fun createVersionThreeFixture(
        databaseName: String,
        profile: ProfileEntity,
        expenseId: String,
    ) {
        context.deleteDatabase(databaseName)
        context.getDatabasePath(databaseName).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(databaseName), null).use { database ->
            database.execSQL(
                "CREATE TABLE `primary_profile` (`profileId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`preferredCurrency` TEXT NOT NULL, `avatarId` TEXT, `colorId` TEXT, " +
                    "`primarySlot` INTEGER NOT NULL, PRIMARY KEY(`profileId`))",
            )
            database.execSQL(
                "CREATE UNIQUE INDEX `index_primary_profile_primarySlot` ON `primary_profile` (`primarySlot`)",
            )
            database.execSQL(
                "CREATE TABLE `expenses` (`expenseId` TEXT NOT NULL, `amountMinorUnits` INTEGER NOT NULL, " +
                    "`currencyCode` TEXT NOT NULL, `capturedAtEpochMillis` INTEGER NOT NULL, " +
                    "`createdByProfileId` TEXT NOT NULL, `vendor` TEXT, `category` TEXT, " +
                    "`receiptFileName` TEXT, PRIMARY KEY(`expenseId`))",
            )
            database.execSQL(
                "CREATE INDEX `index_expenses_capturedAtEpochMillis` ON `expenses` (`capturedAtEpochMillis`)",
            )
            database.execSQL(
                "CREATE TABLE `reminder_preference` (`slot` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, " +
                    "`localTimeMinutes` INTEGER NOT NULL, `permissionPrompted` INTEGER NOT NULL, " +
                    "`lastDeliveredLocalDate` TEXT, PRIMARY KEY(`slot`))",
            )
            database.execSQL(
                "CREATE TABLE `dashboard_preference` (`preferenceId` INTEGER NOT NULL, " +
                    "`pinnedOptionIds` TEXT NOT NULL, PRIMARY KEY(`preferenceId`))",
            )
            database.execSQL(
                "INSERT INTO primary_profile VALUES (?, ?, ?, ?, ?, 1)",
                arrayOf(profile.profileId, profile.name, profile.preferredCurrency, profile.avatarId, profile.colorId),
            )
            database.execSQL(
                "INSERT INTO expenses VALUES (?, 1250, 'USD', 1781222400000, ?, 'Market', NULL, NULL)",
                arrayOf(expenseId, profile.profileId),
            )
            database.execSQL("INSERT INTO reminder_preference VALUES (1, 1, 480, 1, '2026-06-12')")
            database.execSQL(
                "INSERT INTO dashboard_preference VALUES (1, 'expense_history,expense_capture')",
            )
            database.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            database.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) " +
                    "VALUES (42, '787b840b48c53471c8675f2ea2474841')",
            )
            database.version = 3
        }
    }

    private fun hasPlaintextHeader(file: File): Boolean =
        file.inputStream().use {
            val header = ByteArray(SQLITE_HEADER.size)
            it.read(header) == header.size && header.contentEquals(SQLITE_HEADER)
        }

    private companion object {
        val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    }
}
