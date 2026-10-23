package com.nestvest.expense.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initialize(preference: ReminderPreferenceEntity = ReminderPreferenceEntity())
    @Query("SELECT * FROM reminder_preference WHERE slot = 1")
    suspend fun read(): ReminderPreferenceEntity?
    @Query("SELECT * FROM reminder_preference WHERE slot = 1")
    fun observe(): Flow<ReminderPreferenceEntity?>
    @Query("UPDATE reminder_preference SET enabled = :enabled, localTimeMinutes = :minutes WHERE slot = 1")
    suspend fun update(enabled: Boolean, minutes: Int)
    @Query("UPDATE reminder_preference SET permissionPrompted = 1 WHERE slot = 1")
    suspend fun markPrompted()
    @Query("UPDATE reminder_preference SET lastDeliveredLocalDate = :date WHERE slot = 1 AND enabled = 1 AND (lastDeliveredLocalDate IS NULL OR lastDeliveredLocalDate < :date)")
    suspend fun claim(date: String): Int
}
