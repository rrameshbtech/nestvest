package com.nestvest.profile.data

import android.content.Context
import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nestvest.dashboard.data.DashboardPreferenceDao
import com.nestvest.dashboard.data.DashboardPreferenceEntity
import com.nestvest.expense.data.*

@Database(
    entities = [
        ProfileEntity::class,
        ExpenseEntity::class,
        ReminderPreferenceEntity::class,
        DashboardPreferenceEntity::class,
    ],
    version = 3, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderPreferenceDao(): ReminderPreferenceDao
    abstract fun dashboardPreferenceDao(): DashboardPreferenceDao

    companion object {
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dashboard_preference` " +
                        "(`preferenceId` INTEGER NOT NULL, `pinnedOptionIds` TEXT NOT NULL, " +
                        "PRIMARY KEY(`preferenceId`))",
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO `dashboard_preference` " +
                        "(`preferenceId`, `pinnedOptionIds`) VALUES (1, 'expense_capture,expense_history')",
                )
            }
        }

        fun open(context: Context, name: String = "nestvest.db"): ProfileDatabase =
            Room.databaseBuilder(context.applicationContext, ProfileDatabase::class.java, name)
                .addMigrations(MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // The UUID stays the primary key; a separate invariant limits the installation to one profile.
                        for (operation in listOf("INSERT", "UPDATE")) {
                            db.execSQL(
                                "CREATE TRIGGER singleton_${operation.lowercase()} BEFORE $operation ON primary_profile " +
                                    "WHEN NEW.primarySlot != 1 BEGIN SELECT RAISE(ABORT, 'Invalid primary slot'); END",
                            )
                        }
                    }
                })
                .build()
    }
}
