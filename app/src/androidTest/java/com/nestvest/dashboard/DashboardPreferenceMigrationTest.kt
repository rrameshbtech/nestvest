package com.nestvest.dashboard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.nestvest.dashboard.data.DashboardPreferenceEntity
import com.nestvest.expense.data.ExpenseEntity
import com.nestvest.expense.data.ReminderPreferenceEntity
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.data.ProfileEntity
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardPreferenceMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun migrationFromVersionTwoPreservesExistingRowsAndAddsDefaultPreference() = runBlocking {
        val name = "dashboard-migration-${UUID.randomUUID()}.db"
        val profile = ProfileEntity(
            profileId = "8d712d98-6119-40bd-ae18-91d599dc673e",
            name = "Mira",
            preferredCurrency = "INR",
            avatarId = "leaf",
            colorId = "sage",
        )
        val expense = ExpenseEntity(
            expenseId = "95c9442d-a152-42f8-ab2b-9bc132341438",
            amountMinorUnits = 1234,
            currencyCode = "INR",
            capturedAtEpochMillis = 1_729_747_200_000,
            createdByProfileId = profile.profileId,
            vendor = "Tea shop",
            category = "Culture",
        )
        val reminder = ReminderPreferenceEntity(
            enabled = true,
            localTimeMinutes = 1200,
            permissionPrompted = true,
            lastDeliveredLocalDate = "2026-10-23",
        )
        createVersionTwoFixture(name, profile, expense, reminder)

        val database = ProfileDatabase.open(context, name)
        try {
            assertEquals(profile, database.profileDao().read())
            assertEquals(listOf(expense), database.expenseDao().observeAll().first())
            assertEquals(reminder, database.reminderPreferenceDao().read())
            assertEquals(
                DashboardPreferenceEntity(),
                database.dashboardPreferenceDao().observe().first(),
            )
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }

    private fun createVersionTwoFixture(
        name: String,
        profile: ProfileEntity,
        expense: ExpenseEntity,
        reminder: ReminderPreferenceEntity,
    ) {
        context.deleteDatabase(name)
        context.getDatabasePath(name).parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL(
                "CREATE TABLE `primary_profile` (`profileId` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                    "`preferredCurrency` TEXT NOT NULL, `avatarId` TEXT, `colorId` TEXT, " +
                    "`primarySlot` INTEGER NOT NULL, PRIMARY KEY(`profileId`))",
            )
            db.execSQL("CREATE UNIQUE INDEX `index_primary_profile_primarySlot` ON `primary_profile` (`primarySlot`)")
            db.execSQL(
                "INSERT INTO `primary_profile` VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(profile.profileId, profile.name, profile.preferredCurrency,
                    profile.avatarId, profile.colorId, profile.primarySlot),
            )
            db.execSQL(
                "CREATE TABLE `expenses` (`expenseId` TEXT NOT NULL, `amountMinorUnits` INTEGER NOT NULL, " +
                    "`currencyCode` TEXT NOT NULL, `capturedAtEpochMillis` INTEGER NOT NULL, " +
                    "`createdByProfileId` TEXT NOT NULL, `vendor` TEXT, `category` TEXT, " +
                    "`receiptFileName` TEXT, PRIMARY KEY(`expenseId`))",
            )
            db.execSQL("CREATE INDEX `index_expenses_capturedAtEpochMillis` ON `expenses` (`capturedAtEpochMillis`)")
            db.execSQL(
                "INSERT INTO `expenses` VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(expense.expenseId, expense.amountMinorUnits, expense.currencyCode,
                    expense.capturedAtEpochMillis, expense.createdByProfileId, expense.vendor,
                    expense.category, expense.receiptFileName),
            )
            db.execSQL(
                "CREATE TABLE `reminder_preference` (`slot` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, " +
                    "`localTimeMinutes` INTEGER NOT NULL, `permissionPrompted` INTEGER NOT NULL, " +
                    "`lastDeliveredLocalDate` TEXT, PRIMARY KEY(`slot`))",
            )
            db.execSQL(
                "INSERT INTO `reminder_preference` VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(reminder.slot, reminder.enabled, reminder.localTimeMinutes,
                    reminder.permissionPrompted, reminder.lastDeliveredLocalDate),
            )
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            db.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
                arrayOf("9996bf95cde162a0ffa31be258edc64e"),
            )
            db.version = 2
        }
    }
}
