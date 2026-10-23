package com.nestvest.expense.domain

import com.nestvest.profile.domain.PrimaryProfile
import com.nestvest.profile.domain.StorageError
import java.util.UUID
import kotlinx.coroutines.flow.Flow

sealed interface ExpenseRead {
    data class Found(val expenses: List<Expense>) : ExpenseRead
    data class Failed(val error: StorageError) : ExpenseRead
}

sealed interface ExpenseWrite {
    data class Saved(val expense: Expense, val cleanupPending: Boolean = false) : ExpenseWrite
    data class Deleted(val cleanupPending: Boolean = false) : ExpenseWrite
    data class Failed(val error: StorageError) : ExpenseWrite
    data object Invalid : ExpenseWrite
    data object Missing : ExpenseWrite
}

interface ExpenseRepository {
    fun observe(startInclusive: Long, endExclusive: Long): Flow<ExpenseRead>
    fun observeAll(): Flow<ExpenseRead>
    suspend fun create(amount: Long, profile: PrimaryProfile): ExpenseWrite
}
