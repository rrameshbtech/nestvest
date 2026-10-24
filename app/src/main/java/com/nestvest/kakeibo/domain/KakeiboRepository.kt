package com.nestvest.kakeibo.domain

import com.nestvest.expense.domain.Expense
import com.nestvest.profile.domain.StorageError
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.flow.Flow

data class PeriodReflection(
    val reflectionId: UUID,
    val month: YearMonth,
    val whatWentWell: String,
    val whatToChange: String,
    val nextMonthIntention: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    init {
        require(month.toString().matches(Regex("\\d{4}-\\d{2}")))
    }
}

enum class LearningPromptState { UNSEEN, VIEWED, DISMISSED }

sealed interface KakeiboRead<out T> {
    data object Absent : KakeiboRead<Nothing>
    data class Found<T>(val value: T) : KakeiboRead<T>
    data class Failed(val error: StorageError) : KakeiboRead<Nothing>
}

sealed interface KakeiboWrite {
    data object Saved : KakeiboWrite
    data object NotEligible : KakeiboWrite
    data object InvalidDraft : KakeiboWrite
    data class Failed(val error: StorageError) : KakeiboWrite
}

interface KakeiboRepository {
    suspend fun readPlan(month: YearMonth): KakeiboRead<MonthlyPlan>
    suspend fun savePlan(plan: MonthlyPlan): KakeiboWrite
    fun observeExpenses(period: EpochRange): Flow<KakeiboRead<List<Expense>>>
    suspend fun unassignedExpenses(period: EpochRange, currencyCode: String): KakeiboRead<List<Expense>>
    suspend fun assignExpense(
        expenseId: UUID,
        category: KakeiboCategory,
        month: YearMonth,
        currencyCode: String,
    ): KakeiboWrite = assignExpenses(setOf(expenseId), category, month, currencyCode)
    suspend fun assignExpenses(
        expenseIds: Set<UUID>,
        category: KakeiboCategory,
        month: YearMonth,
        currencyCode: String,
    ): KakeiboWrite
    suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection>
    suspend fun saveReflection(reflection: PeriodReflection): KakeiboWrite
    suspend fun readLearningState(): KakeiboRead<LearningPromptState>
    suspend fun saveLearningState(state: LearningPromptState): KakeiboWrite
}
