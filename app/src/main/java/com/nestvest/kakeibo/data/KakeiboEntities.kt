package com.nestvest.kakeibo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kakeibo_monthly_plan",
    indices = [Index(value = ["yearMonth"], unique = true)],
)
data class KakeiboMonthlyPlanEntity(
    @PrimaryKey val planId: String,
    val yearMonth: String,
    val currencyCode: String,
    val availableMinorUnits: Long?,
    val savingsIntentionMinorUnits: Long,
    val weeklyPacingEnabled: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "kakeibo_category_plan",
    primaryKeys = ["planId", "categoryCode"],
    foreignKeys = [
        ForeignKey(
            entity = KakeiboMonthlyPlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("planId")],
)
data class KakeiboCategoryPlanEntity(
    val planId: String,
    val categoryCode: String,
    val plannedMinorUnits: Long,
)

@Entity(
    tableName = "kakeibo_weekly_allocation",
    primaryKeys = ["planId", "categoryCode", "weekStartDate"],
    foreignKeys = [
        ForeignKey(
            entity = KakeiboCategoryPlanEntity::class,
            parentColumns = ["planId", "categoryCode"],
            childColumns = ["planId", "categoryCode"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["planId", "categoryCode"])],
)
data class KakeiboWeeklyAllocationEntity(
    val planId: String,
    val categoryCode: String,
    val weekStartDate: String,
    val weekEndDateInclusive: String,
    val plannedMinorUnits: Long,
)

@Entity(
    tableName = "kakeibo_period_reflection",
    indices = [Index(value = ["yearMonth"], unique = true)],
)
data class KakeiboReflectionEntity(
    @PrimaryKey val reflectionId: String,
    val yearMonth: String,
    val whatWentWell: String,
    val whatToChange: String,
    val nextMonthIntention: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "kakeibo_learning_preference")
data class KakeiboLearningPreferenceEntity(
    @PrimaryKey val preferenceId: Int = SINGLETON_ID,
    val learningPromptState: String = UNSEEN,
) {
    companion object {
        const val SINGLETON_ID = 1
        const val UNSEEN = "UNSEEN"
        const val VIEWED = "VIEWED"
        const val DISMISSED = "DISMISSED"
    }
}
