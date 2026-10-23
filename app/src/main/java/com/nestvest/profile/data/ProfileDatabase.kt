package com.nestvest.profile.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ProfileEntity::class], version = 1, exportSchema = true)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

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
