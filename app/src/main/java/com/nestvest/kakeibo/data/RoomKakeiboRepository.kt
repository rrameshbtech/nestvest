package com.nestvest.kakeibo.data

import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.util.Log
import androidx.room.withTransaction
import com.nestvest.expense.data.ExpenseEntity
import com.nestvest.expense.domain.Expense
import com.nestvest.kakeibo.domain.EpochRange
import com.nestvest.kakeibo.domain.KakeiboCategory
import com.nestvest.kakeibo.domain.KakeiboRead
import com.nestvest.kakeibo.domain.KakeiboRepository
import com.nestvest.kakeibo.domain.KakeiboWrite
import com.nestvest.kakeibo.domain.KakeiboPeriod
import com.nestvest.kakeibo.domain.LearningPromptState
import com.nestvest.kakeibo.domain.MonthlyPlan
import com.nestvest.kakeibo.domain.PeriodReflection
import com.nestvest.kakeibo.domain.WeeklyAllocation
import com.nestvest.profile.data.ProfileDatabase
import com.nestvest.profile.domain.StorageError
import java.io.IOException
import java.time.YearMonth
import java.time.ZoneId
import java.util.Currency
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class RoomKakeiboRepository(
    private val database: ProfileDatabase,
) : KakeiboRepository {
    private val dao = database.kakeiboDao()

    override suspend fun readPlan(month: YearMonth): KakeiboRead<MonthlyPlan> = try {
        val plan = dao.plan(month.toString()) ?: return KakeiboRead.Absent
        val categories = dao.categories(plan.planId).associate {
            KakeiboCategory.valueOf(it.categoryCode) to it.plannedMinorUnits
        }
        val allocations = dao.weeklyAllocations(plan.planId).map {
            WeeklyAllocation(
                category = KakeiboCategory.valueOf(it.categoryCode),
                weekStartDate = java.time.LocalDate.parse(it.weekStartDate),
                weekEndDateInclusive = java.time.LocalDate.parse(it.weekEndDateInclusive),
                plannedMinorUnits = it.plannedMinorUnits,
            )
        }
        KakeiboRead.Found(
            MonthlyPlan(
                planId = UUID.fromString(plan.planId),
                month = YearMonth.parse(plan.yearMonth),
                currencyCode = plan.currencyCode,
                availableMinorUnits = plan.availableMinorUnits,
                savingsIntentionMinorUnits = plan.savingsIntentionMinorUnits,
                weeklyPacingEnabled = plan.weeklyPacingEnabled,
                categoryAmounts = categories,
                weeklyAllocations = allocations,
                createdAtEpochMillis = plan.createdAtEpochMillis,
                updatedAtEpochMillis = plan.updatedAtEpochMillis,
            ),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("read plan", error)
        KakeiboRead.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("read plan", error)
        KakeiboRead.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("read plan", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: IllegalStateException) {
        diagnose("read plan", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: ArithmeticException) {
        diagnose("read plan", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun savePlan(plan: MonthlyPlan): KakeiboWrite = try {
        Currency.getInstance(plan.currencyCode)
        database.withTransaction {
            val existing = dao.plan(plan.month.toString())
            val planId = existing?.planId ?: plan.planId.toString()
            val now = System.currentTimeMillis()
            dao.replacePlan(
                plan = KakeiboMonthlyPlanEntity(
                    planId = planId,
                    yearMonth = plan.month.toString(),
                    currencyCode = plan.currencyCode,
                    availableMinorUnits = plan.availableMinorUnits,
                    savingsIntentionMinorUnits = plan.savingsIntentionMinorUnits,
                    weeklyPacingEnabled = plan.weeklyPacingEnabled,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: plan.createdAtEpochMillis.takeIf { it > 0 } ?: now,
                    updatedAtEpochMillis = now,
                ),
                categories = plan.categoryAmounts.map { (category, amount) ->
                    KakeiboCategoryPlanEntity(planId, category.name, amount)
                },
                allocations = plan.weeklyAllocations.map { allocation ->
                    KakeiboWeeklyAllocationEntity(
                        planId = planId,
                        categoryCode = allocation.category.name,
                        weekStartDate = allocation.weekStartDate.toString(),
                        weekEndDateInclusive = allocation.weekEndDateInclusive.toString(),
                        plannedMinorUnits = allocation.plannedMinorUnits,
                    )
                },
            )
        }
        KakeiboWrite.Saved
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("save plan", error)
        KakeiboWrite.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("save plan", error)
        KakeiboWrite.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("save plan", error)
        KakeiboWrite.InvalidDraft
    } catch (error: IllegalStateException) {
        diagnose("save plan", error)
        KakeiboWrite.Failed(StorageError.INVALID_RECORD)
    } catch (error: ArithmeticException) {
        diagnose("save plan", error)
        KakeiboWrite.InvalidDraft
    }

    override fun observeExpenses(period: EpochRange): Flow<KakeiboRead<List<Expense>>> {
        val reads: Flow<KakeiboRead<List<Expense>>> =
            dao.observeExpenses(period.startInclusive, period.endExclusive)
                .map { entities -> KakeiboRead.Found(entities.map(ExpenseEntity::toExpense)) }
        return reads.catch { error ->
                when (error) {
                    is CancellationException -> throw error
                    is SQLiteException -> {
                        diagnose("observe expenses", error)
                        emit(KakeiboRead.Failed(storageError(error)))
                    }
                    is IOException -> {
                        diagnose("observe expenses", error)
                        emit(KakeiboRead.Failed(StorageError.UNAVAILABLE))
                    }
                    is IllegalArgumentException, is IllegalStateException -> {
                        diagnose("observe expenses", error)
                        emit(KakeiboRead.Failed(StorageError.INVALID_RECORD))
                    }
                    else -> throw error
                }
            }
    }

    override suspend fun unassignedExpenses(
        period: EpochRange,
        currencyCode: String,
    ): KakeiboRead<List<Expense>> = try {
        Currency.getInstance(currencyCode)
        KakeiboRead.Found(
            dao.unassignedExpenses(period.startInclusive, period.endExclusive, currencyCode)
                .map(ExpenseEntity::toExpense),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("read unassigned expenses", error)
        KakeiboRead.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("read unassigned expenses", error)
        KakeiboRead.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("read unassigned expenses", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: IllegalStateException) {
        diagnose("read unassigned expenses", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun assignExpenses(
        expenseIds: Set<UUID>,
        category: KakeiboCategory,
        month: YearMonth,
        currencyCode: String,
    ): KakeiboWrite = try {
        Currency.getInstance(currencyCode)
        require(expenseIds.isNotEmpty()) { "Select at least one expense" }
        val period = KakeiboPeriod(month).epochRange(ZoneId.systemDefault())
        val assigned = database.withTransaction {
            val plan = dao.plan(month.toString()) ?: return@withTransaction false
            if (plan.currencyCode != currencyCode) return@withTransaction false
            expenseIds.forEach { expenseId ->
                val updated = dao.assignUncategorizedExpense(
                    expenseId = expenseId.toString(),
                    categoryCode = category.name,
                    currencyCode = currencyCode,
                    startInclusive = period.startInclusive,
                    endExclusive = period.endExclusive,
                )
                if (updated != 1) throw AssignmentNoLongerEligible()
            }
            true
        }
        if (assigned) KakeiboWrite.Saved else KakeiboWrite.NotEligible
    } catch (_: AssignmentNoLongerEligible) {
        KakeiboWrite.NotEligible
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("assign expenses", error)
        KakeiboWrite.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("assign expenses", error)
        KakeiboWrite.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("assign expenses", error)
        KakeiboWrite.InvalidDraft
    } catch (error: IllegalStateException) {
        diagnose("assign expenses", error)
        KakeiboWrite.Failed(StorageError.INVALID_RECORD)
    }

    private class AssignmentNoLongerEligible : RuntimeException()

    override suspend fun readReflection(month: YearMonth): KakeiboRead<PeriodReflection> = try {
        val entity = dao.reflection(month.toString()) ?: return KakeiboRead.Absent
        KakeiboRead.Found(
            PeriodReflection(
                reflectionId = UUID.fromString(entity.reflectionId),
                month = YearMonth.parse(entity.yearMonth),
                whatWentWell = entity.whatWentWell,
                whatToChange = entity.whatToChange,
                nextMonthIntention = entity.nextMonthIntention,
                createdAtEpochMillis = entity.createdAtEpochMillis,
                updatedAtEpochMillis = entity.updatedAtEpochMillis,
            ),
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("read reflection", error)
        KakeiboRead.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("read reflection", error)
        KakeiboRead.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("read reflection", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: IllegalStateException) {
        diagnose("read reflection", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun saveReflection(reflection: PeriodReflection): KakeiboWrite = try {
        database.withTransaction {
            val existing = dao.reflection(reflection.month.toString())
            val now = System.currentTimeMillis()
            dao.putReflection(
                KakeiboReflectionEntity(
                    reflectionId = existing?.reflectionId ?: reflection.reflectionId.toString(),
                    yearMonth = reflection.month.toString(),
                    whatWentWell = reflection.whatWentWell,
                    whatToChange = reflection.whatToChange,
                    nextMonthIntention = reflection.nextMonthIntention,
                    createdAtEpochMillis = existing?.createdAtEpochMillis
                        ?: reflection.createdAtEpochMillis.takeIf { it > 0 } ?: now,
                    updatedAtEpochMillis = now,
                ),
            )
        }
        KakeiboWrite.Saved
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("save reflection", error)
        KakeiboWrite.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("save reflection", error)
        KakeiboWrite.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("save reflection", error)
        KakeiboWrite.InvalidDraft
    } catch (error: IllegalStateException) {
        diagnose("save reflection", error)
        KakeiboWrite.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun readLearningState(): KakeiboRead<LearningPromptState> = try {
        dao.initializeLearningPreference(KakeiboLearningPreferenceEntity())
        val state = dao.learningPreference()?.learningPromptState
            ?: return KakeiboRead.Failed(StorageError.INVALID_RECORD)
        KakeiboRead.Found(LearningPromptState.valueOf(state))
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("read learning state", error)
        KakeiboRead.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("read learning state", error)
        KakeiboRead.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("read learning state", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    } catch (error: IllegalStateException) {
        diagnose("read learning state", error)
        KakeiboRead.Failed(StorageError.INVALID_RECORD)
    }

    override suspend fun saveLearningState(state: LearningPromptState): KakeiboWrite = try {
        database.withTransaction {
            dao.initializeLearningPreference(KakeiboLearningPreferenceEntity())
            val current = dao.learningPreference()?.learningPromptState
                ?.let(LearningPromptState::valueOf)
                ?: error("Learning preference is unavailable")
            when {
                current == state -> KakeiboWrite.Saved
                current != LearningPromptState.UNSEEN || state == LearningPromptState.UNSEEN ->
                    KakeiboWrite.InvalidDraft
                dao.setLearningPromptState(state.name) == 1 -> KakeiboWrite.Saved
                else -> KakeiboWrite.Failed(StorageError.INVALID_RECORD)
            }
        }
    } catch (error: CancellationException) {
        throw error
    } catch (error: SQLiteException) {
        diagnose("save learning state", error)
        KakeiboWrite.Failed(storageError(error))
    } catch (error: IOException) {
        diagnose("save learning state", error)
        KakeiboWrite.Failed(StorageError.UNAVAILABLE)
    } catch (error: IllegalArgumentException) {
        diagnose("save learning state", error)
        KakeiboWrite.InvalidDraft
    } catch (error: IllegalStateException) {
        diagnose("save learning state", error)
        KakeiboWrite.Failed(StorageError.INVALID_RECORD)
    }

    private fun storageError(error: Throwable): StorageError =
        if (error is SQLiteFullException) StorageError.FULL else StorageError.UNAVAILABLE

    private fun diagnose(operation: String, error: Exception) {
        Log.w(TAG, "Kakeibo $operation failed (${error.javaClass.simpleName})")
    }

    private companion object {
        const val TAG = "NestVestStorage"
    }
}
