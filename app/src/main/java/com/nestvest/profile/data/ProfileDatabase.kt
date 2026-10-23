package com.nestvest.profile.data

import android.content.Context
import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nestvest.expense.data.*

@Database(
    entities = [ProfileEntity::class, ExpenseEntity::class, ReminderPreferenceEntity::class],
    version = 2, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderPreferenceDao(): ReminderPreferenceDao

    companion object {
        fun open(context: Context, name: String = "nestvest.db"): ProfileDatabase =
            Room.databaseBuilder(context.applicationContext, ProfileDatabase::class.java, name)
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
