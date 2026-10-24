package com.nestvest.profile.data

import android.content.Context
import androidx.room.Database
import androidx.room.AutoMigration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nestvest.dashboard.data.DashboardPreferenceDao
import com.nestvest.dashboard.data.DashboardPreferenceEntity
import com.nestvest.expense.data.*
import com.nestvest.kakeibo.data.KakeiboCategoryPlanEntity
import com.nestvest.kakeibo.data.KakeiboDao
import com.nestvest.kakeibo.data.KakeiboLearningPreferenceEntity
import com.nestvest.kakeibo.data.KakeiboMonthlyPlanEntity
import com.nestvest.kakeibo.data.KakeiboReflectionEntity
import com.nestvest.kakeibo.data.KakeiboWeeklyAllocationEntity

@Database(
    entities = [
        ProfileEntity::class,
        ExpenseEntity::class,
        ReminderPreferenceEntity::class,
        DashboardPreferenceEntity::class,
        KakeiboMonthlyPlanEntity::class,
        KakeiboCategoryPlanEntity::class,
        KakeiboWeeklyAllocationEntity::class,
        KakeiboReflectionEntity::class,
        KakeiboLearningPreferenceEntity::class,
    ],
    version = 4, exportSchema = true, autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class ProfileDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderPreferenceDao(): ReminderPreferenceDao
    abstract fun dashboardPreferenceDao(): DashboardPreferenceDao
    abstract fun kakeiboDao(): KakeiboDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kakeibo_monthly_plan` (" +
                        "`planId` TEXT NOT NULL, `yearMonth` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, " +
                        "`availableMinorUnits` INTEGER, `savingsIntentionMinorUnits` INTEGER NOT NULL, " +
                        "`weeklyPacingEnabled` INTEGER NOT NULL, `createdAtEpochMillis` INTEGER NOT NULL, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL, PRIMARY KEY(`planId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_kakeibo_monthly_plan_yearMonth` " +
                        "ON `kakeibo_monthly_plan` (`yearMonth`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kakeibo_category_plan` (" +
                        "`planId` TEXT NOT NULL, `categoryCode` TEXT NOT NULL, " +
                        "`plannedMinorUnits` INTEGER NOT NULL, PRIMARY KEY(`planId`, `categoryCode`), " +
                        "FOREIGN KEY(`planId`) REFERENCES `kakeibo_monthly_plan`(`planId`) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_kakeibo_category_plan_planId` " +
                        "ON `kakeibo_category_plan` (`planId`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kakeibo_weekly_allocation` (" +
                        "`planId` TEXT NOT NULL, `categoryCode` TEXT NOT NULL, `weekStartDate` TEXT NOT NULL, " +
                        "`weekEndDateInclusive` TEXT NOT NULL, `plannedMinorUnits` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`planId`, `categoryCode`, `weekStartDate`), " +
                        "FOREIGN KEY(`planId`, `categoryCode`) REFERENCES `kakeibo_category_plan`(`planId`, `categoryCode`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_kakeibo_weekly_allocation_planId_categoryCode` " +
                        "ON `kakeibo_weekly_allocation` (`planId`, `categoryCode`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kakeibo_period_reflection` (" +
                        "`reflectionId` TEXT NOT NULL, `yearMonth` TEXT NOT NULL, `whatWentWell` TEXT NOT NULL, " +
                        "`whatToChange` TEXT NOT NULL, `nextMonthIntention` TEXT NOT NULL, " +
                        "`createdAtEpochMillis` INTEGER NOT NULL, `updatedAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`reflectionId`))",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_kakeibo_period_reflection_yearMonth` " +
                        "ON `kakeibo_period_reflection` (`yearMonth`)",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `kakeibo_learning_preference` (" +
                        "`preferenceId` INTEGER NOT NULL, `learningPromptState` TEXT NOT NULL, " +
                        "PRIMARY KEY(`preferenceId`))",
                )
            }
        }

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

        fun open(context: Context, name: String = "nestvest.db"): ProfileDatabase {
            val appContext = context.applicationContext
            System.loadLibrary("sqlcipher")
            val passphrase = DatabaseKeyStore(appContext).getOrCreatePassphrase()
            EncryptedDatabaseMigration(appContext.getDatabasePath(name), passphrase).migrateIfNeeded()
            return Room.databaseBuilder(appContext, ProfileDatabase::class.java, name)
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
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
}
