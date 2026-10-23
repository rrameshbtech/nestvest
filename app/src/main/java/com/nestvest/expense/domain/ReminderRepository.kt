package com.nestvest.expense.domain

import com.nestvest.profile.domain.StorageError
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

sealed interface ReminderRead {
    data class Found(val preference: ReminderPreference) : ReminderRead
    data class Failed(val error: StorageError) : ReminderRead
}

sealed interface ReminderWrite {
    data object Saved : ReminderWrite
    data class Failed(val error: StorageError) : ReminderWrite
}

sealed interface ReminderClaim {
    data object Claimed : ReminderClaim
    data object AlreadyClaimed : ReminderClaim
    data class Failed(val error: StorageError) : ReminderClaim
}

interface ReminderRepository {
    fun observe(): Flow<ReminderRead>
    suspend fun read(): ReminderRead
    suspend fun update(enabled: Boolean, minutes: Int): ReminderWrite
    suspend fun markPermissionPrompted(): ReminderWrite
    suspend fun claim(date: LocalDate): ReminderClaim
}
