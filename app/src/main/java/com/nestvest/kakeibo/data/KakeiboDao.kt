package com.nestvest.kakeibo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nestvest.expense.data.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KakeiboDao {
    @Query("SELECT * FROM kakeibo_monthly_plan WHERE yearMonth = :yearMonth")
    suspend fun plan(yearMonth: String): KakeiboMonthlyPlanEntity?

    @Query("SELECT * FROM kakeibo_monthly_plan WHERE yearMonth = :yearMonth")
    fun observePlan(yearMonth: String): Flow<KakeiboMonthlyPlanEntity?>

    @Query("SELECT * FROM kakeibo_category_plan WHERE planId = :planId ORDER BY categoryCode")
    suspend fun categories(planId: String): List<KakeiboCategoryPlanEntity>

    @Query("SELECT * FROM kakeibo_category_plan WHERE planId = :planId ORDER BY categoryCode")
    fun observeCategories(planId: String): Flow<List<KakeiboCategoryPlanEntity>>

    @Query("SELECT * FROM kakeibo_weekly_allocation WHERE planId = :planId ORDER BY weekStartDate")
    suspend fun weeklyAllocations(planId: String): List<KakeiboWeeklyAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putPlan(plan: KakeiboMonthlyPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putCategories(categories: List<KakeiboCategoryPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putWeeklyAllocations(allocations: List<KakeiboWeeklyAllocationEntity>)

    @Query("DELETE FROM kakeibo_weekly_allocation WHERE planId = :planId")
    suspend fun deleteWeeklyAllocations(planId: String)

    @Query("DELETE FROM kakeibo_category_plan WHERE planId = :planId")
    suspend fun deleteCategories(planId: String)

    @Transaction
    suspend fun replacePlan(
        plan: KakeiboMonthlyPlanEntity,
        categories: List<KakeiboCategoryPlanEntity>,
        allocations: List<KakeiboWeeklyAllocationEntity>,
    ) {
        putPlan(plan)
        deleteWeeklyAllocations(plan.planId)
        deleteCategories(plan.planId)
        putCategories(categories)
        putWeeklyAllocations(allocations)
    }

    @Query("SELECT * FROM kakeibo_period_reflection WHERE yearMonth = :yearMonth")
    suspend fun reflection(yearMonth: String): KakeiboReflectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putReflection(reflection: KakeiboReflectionEntity)

    @Query("SELECT * FROM kakeibo_learning_preference WHERE preferenceId = 1")
    fun observeLearningPreference(): Flow<KakeiboLearningPreferenceEntity?>

    @Query("SELECT * FROM kakeibo_learning_preference WHERE preferenceId = 1")
    suspend fun learningPreference(): KakeiboLearningPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initializeLearningPreference(preference: KakeiboLearningPreferenceEntity)

    @Query("UPDATE kakeibo_learning_preference SET learningPromptState = :state WHERE preferenceId = 1")
    suspend fun setLearningPromptState(state: String): Int

    @Query(
        "SELECT * FROM expenses WHERE capturedAtEpochMillis >= :startInclusive " +
            "AND capturedAtEpochMillis < :endExclusive " +
            "ORDER BY capturedAtEpochMillis ASC, expenseId ASC",
    )
    fun observeExpenses(startInclusive: Long, endExclusive: Long): Flow<List<ExpenseEntity>>

    @Query(
        "SELECT * FROM expenses WHERE capturedAtEpochMillis >= :startInclusive " +
            "AND capturedAtEpochMillis < :endExclusive AND category IS NULL " +
            "AND currencyCode = :currencyCode " +
            "ORDER BY capturedAtEpochMillis ASC, expenseId ASC",
    )
    suspend fun unassignedExpenses(
        startInclusive: Long,
        endExclusive: Long,
        currencyCode: String,
    ): List<ExpenseEntity>

    @Query(
        "UPDATE expenses SET category = :categoryCode WHERE expenseId = :expenseId " +
            "AND category IS NULL AND currencyCode = :currencyCode " +
            "AND capturedAtEpochMillis >= :startInclusive " +
            "AND capturedAtEpochMillis < :endExclusive",
    )
    suspend fun assignUncategorizedExpense(
        expenseId: String,
        categoryCode: String,
        currencyCode: String,
        startInclusive: Long,
        endExclusive: Long,
    ): Int
}
