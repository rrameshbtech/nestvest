package com.nestvest.dashboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardPreferenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun initialize(preference: DashboardPreferenceEntity = DashboardPreferenceEntity())

    @Query("SELECT * FROM dashboard_preference WHERE preferenceId = 1")
    fun observe(): Flow<DashboardPreferenceEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(preference: DashboardPreferenceEntity)
}
